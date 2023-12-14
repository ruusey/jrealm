package com.jrealm.game.realm;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.model.ProjectilePositionMode;
import com.jrealm.game.states.PlayState;
import com.jrealm.game.tiles.TileMap;
import com.jrealm.game.tiles.blocks.Tile;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.TimedWorkerThread;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.ProcessingThread;
import com.jrealm.net.server.ServerGameLogic;
import com.jrealm.net.server.SocketServer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("unused")
public class RealmManagerServer implements Runnable {
	private SocketServer server;
	private Realm realm;
	private boolean shutdown = false;

	private final Map<Byte, BiConsumer<RealmManagerServer, Packet>> packetCallbacksServer = new HashMap<>();
	private List<Vector2f> shotDestQueue;
	private long lastUpdateTime;
	private long now;
	private long lastRenderTime;
	private long lastSecondTime;

	private int oldFrameCount;
	private int oldTickCount;
	private int tickCount;

	private List<Long> expiredEnemies;
	private List<Long> expiredBullets;
	private List<Long> expiredPlayers;

	private Map<String, Long> remoteAddresses = new HashMap<>();
	private Map<Long, LoadPacket> playerLoadState = new HashMap<>();
	private Map<Long, UpdatePacket> playerUpdateState = new HashMap<>();
	private UnloadPacket lastUnload;
	private volatile Queue<Packet> outboundPacketQueue = new ConcurrentLinkedQueue<>();

	public RealmManagerServer(Realm realm) {
		this.registerPacketCallbacks();
		this.realm = realm;
		this.server = new SocketServer(2222);
		this.shotDestQueue = new ArrayList<>();
		this.expiredEnemies = new ArrayList<>();
		this.expiredPlayers = new ArrayList<>();
		this.expiredBullets = new ArrayList<>();
		WorkerThread.submitAndForkRun(this.server);
		this.getRealm().loadMap("tile/vault.xml", null);
	}
	// Adds a headless player for each of CharacterClass in available classes
	// Will wait briefly after adding each player to avoid client buffer overflow.
	private void spawnTestPlayers(final int count) {
		final Runnable spawnTestPlayers = ()-> {
			final Random random = new Random(Instant.now().toEpochMilli());
			for(int i = 0 ; i < count; i++) {
				final CharacterClass classToSpawn = CharacterClass.getCharacterClasses().get(random.nextInt(CharacterClass.getCharacterClasses().size()));
				final Camera c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
				try {
					final Player player = new Player(Realm.RANDOM.nextLong(), c, GameDataManager.loadClassSprites(classToSpawn),
							new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
									(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
							GlobalConstants.PLAYER_SIZE, classToSpawn);
					String playerName = UUID.randomUUID().toString().replaceAll("-", "");
					playerName = playerName.substring(playerName.length()/2);
					player.setName(playerName);
					player.equipSlots(PlayState.getStartingEquipment(classToSpawn));
					player.setMaxSpeed(random.nextFloat()/2);
					
					final boolean up = random.nextBoolean();
					final boolean right = random.nextBoolean();

					if(up) {
						player.setUp(true);
					}else {
						player.setDown(true);
					}
					if(right) {
						player.setRight(true);
					}else {
						player.setLeft(true);
					}
					
					player.setHeadless(true);
					final long newId = this.getRealm().addPlayer(player);
					Thread.sleep(500);
				}catch(Exception e) {
					log.error("Failed to spawn test character of class type {}. Reason: {}", classToSpawn, e);
				}
			}		
		};
		// Run this in a completely separate thread
		WorkerThread.submitAndForkRun(spawnTestPlayers);
	}

	@Override
	public void run() {
		RealmManagerServer.log.info("Starting JRealm Server");
		final Runnable tick = () -> {
			this.tick();
			this.update(0);
		};

		final TimedWorkerThread workerThread = new TimedWorkerThread(tick, 32);
		WorkerThread.submitAndForkRun(workerThread);
		RealmManagerServer.log.info("RealmManager exiting run().");
		this.spawnTestPlayers(7);
	}

	private void tick() {
		try {
			final Runnable enqueueGameData = () -> {
				this.enqueueGameData();
			};

			final Runnable processServerPackets = () -> {
				this.processServerPackets();
			};

			final Runnable sendGameData = () -> {
				this.sendGameData();
			};
			// We dont necessarily care what order these three tasks take place in
			// rather that all three are completed at least once per tick, so run them 
			// asynchronously
			WorkerThread.submitAndRun(enqueueGameData, processServerPackets, sendGameData);
		} catch (Exception e) {
			RealmManagerServer.log.error("Failed to sleep");
		}
	}

	private void sendGameData() {
		final List<Packet> packetsToBroadcast = new ArrayList<>();
		
		while(!this.outboundPacketQueue.isEmpty()) {
			packetsToBroadcast.add(this.outboundPacketQueue.remove());
		}

		final List<String> disconnectedClients = new ArrayList<>();
		for (final Map.Entry<String, ProcessingThread> client : this.server.getClients().entrySet()) {
			try {
				final OutputStream toClientStream = client.getValue().getClientSocket().getOutputStream();
				final DataOutputStream dosToClient = new DataOutputStream(toClientStream);

				for (final Packet packet : packetsToBroadcast) {
					packet.serializeWrite(dosToClient);
				}
			}catch(Exception e) {
				disconnectedClients.add(client.getKey());
				RealmManagerServer.log.error("Failed to get OutputStream to Client. Reason: {}", e);
			}
		}

		for(final String disconnectedClient : disconnectedClients) {
			final Long dcPlayerId = this.getRemoteAddresses().get(disconnectedClient);
			this.expiredPlayers.add(dcPlayerId);
			this.realm.getPlayers().remove(dcPlayerId);
			this.server.getClients().remove(disconnectedClient);
		}
	}

	public void enqueueGameData() {
		// Holds 'dead' or expired entities (old bullets, DC'd players, dead enemies)
		UnloadPacket unload = null;
		try {
			unload = this.getUnloadPacket();
		} catch (Exception e) {
			RealmManagerServer.log.error("Failed to create unload packet. Reason: {}", e);
		}
		final UnloadPacket unloadToBroadcast = unload;
		final List<String> disconnectedClients = new ArrayList<>();

		// For each player currently connected
		for (final Map.Entry<Long, Player> player : this.realm.getPlayers().entrySet()) {
			try {
				// Get UpdatePacket for this player and all players in this players viewport
				// Contains, player stat info, inventory, status effects, health and mana data
				final List<UpdatePacket> uPackets = this.realm
						.getPlayersAsPackets(player.getValue().getCam().getBounds());

				// Get LoadPacket for this player
				// Contains newly spawned bullets, entities, players
				final LoadPacket load = this.realm.getLoadPacket(this.realm.getTileManager().getRenderViewPort(player.getValue()));

				// Get the posX, posY, dX, dY of all Entities in this players viewport
				final ObjectMovePacket mPacket = this.realm
						.getGameObjectsAsPackets(player.getValue().getCam().getBounds());

				for (final UpdatePacket packet : uPackets) {
					// Only transmit THIS players UpdatePacket if the state has changed
					if(packet.getPlayerId()!=player.getKey()) {
						continue;
					}
					if(this.playerUpdateState.get(player.getKey())==null) {
						this.playerUpdateState.put(player.getKey(), packet);
						this.enqueueServerPacket(packet);
					}else {
						final UpdatePacket old = this.playerUpdateState.get(player.getKey());
						if(!old.equals(packet)) {
							this.playerUpdateState.put(player.getKey(), packet);
							this.enqueueServerPacket(packet);
						}
					}
				}
				// Only transmit the LoadPacket if its state is changed (it can potentially be
				// large).
				// If the state is changed, only transmit the DELTA data
				if(this.playerLoadState.get(player.getKey())==null) {
					this.playerLoadState.put(player.getKey(), load);
					this.enqueueServerPacket(load);
				}else {
					final LoadPacket old = this.playerLoadState.get(player.getKey());
					if(!old.equals(load)) {
						// Get the LoadPacket delta
						final LoadPacket toSend = old.combine(load);
						this.playerLoadState.put(player.getKey(), load);
						this.enqueueServerPacket(toSend);
					}
				}

				// Only transmit the UnloadPacket if it is non-empty and
				// does not equal the previous Unload state (it can potentially be large)
				if (unloadToBroadcast != null) {
					if((this.lastUnload==null) || !this.lastUnload.equals(unloadToBroadcast)) {
						this.lastUnload = unloadToBroadcast;
						this.enqueueServerPacket(unloadToBroadcast);
					}
				}
				// If the ObjectMove packet isnt empty
				if (mPacket != null) {
					this.enqueueServerPacket(mPacket);
				}

			} catch (Exception e) {
				RealmManagerServer.log.error("Failed to build game data for Player {}. Reason: {}", player.getKey(), e);
			}
		}
		// Used to dynamically re-render changed loot containers (chests) on the client
		// if their
		// contents change in a server tick (receive MoveItem packet from client this
		// tick)
		this.getRealm().getLoot().values().forEach(lootContainer->{
			lootContainer.setContentsChanged(false);
		});
	}

	// For each connected client, dequeue all pending packets 
	// pass the packet and RealmManager context to the handler
	// script
	public void processServerPackets() {
		for(final Map.Entry<String, ProcessingThread> thread : this.getServer().getClients().entrySet()) {
			if(!thread.getValue().isShutdownProcessing()) {
				while(!thread.getValue().getPacketQueue().isEmpty()) {
					final Packet packet = thread.getValue().getPacketQueue().remove();
					try {
						final Packet created = Packet.newInstance(packet.getId(), packet.getData());
						created.setSrcIp(packet.getSrcIp());
						this.packetCallbacksServer.get(created.getId()).accept(this, created);
					} catch (Exception e) {
						RealmManagerServer.log.error("Failed to process server packets {}", e);
					}
				}
			}else {
				final Long dcPlayerId = this.getRemoteAddresses().get(thread.getKey());
				this.expiredPlayers.add(dcPlayerId);
				this.server.getClients().remove(thread.getKey());
				this.realm.removePlayer(dcPlayerId);
			}
		}
	}

	public Player getClosestPlayer(final Vector2f pos, final float limit) {
		float best = Float.MAX_VALUE;
		Player bestPlayer = null;
		for (final Player player : this.realm.getPlayers().values()) {
			final float dist = player.getPos().distanceTo(pos);
			if ((dist < best) && (dist <= limit)) {
				best = dist;
				bestPlayer = player;
			}
		}
		return bestPlayer;
	}

	public LootContainer getClosestLootContainer(final Vector2f pos, final float limit) {
		float best = Float.MAX_VALUE;
		LootContainer bestLoot = null;
		for (final LootContainer lootContainer : this.realm.getLoot().values()) {
			float dist = lootContainer.getPos().distanceTo(pos);
			if ((dist < best) && (dist <= limit)) {
				best = dist;
				bestLoot = lootContainer;
			}
		}
		return bestLoot;
	}

	private UnloadPacket getUnloadPacket() throws Exception {
		final Long[] expiredBullets = this.expiredBullets.toArray(new Long[0]);
		final Long[] expiredEnemies = this.expiredEnemies.toArray(new Long[0]);
		final Long[] expiredPlayers = this.expiredPlayers.toArray(new Long[0]);
		this.expiredPlayers.clear();
		this.expiredBullets.clear();
		this.expiredEnemies.clear();
		final List<Long> lootContainers = this.realm.getLoot().values().stream().filter(lc->lc.isExpired() || lc.isEmpty()).map(LootContainer::getLootContainerId).collect(Collectors.toList());
		for(final Long lcId: lootContainers) {
			this.realm.getLoot().remove(lcId);
		}
		return UnloadPacket.from(expiredPlayers, lootContainers.toArray(new Long[0]), expiredBullets, expiredEnemies);
	}

	private void registerPacketCallbacks() {
		this.registerPacketCallback(PacketType.PLAYER_MOVE.getPacketId(), ServerGameLogic::handlePlayerMoveServer);
		this.registerPacketCallback(PacketType.PLAYER_SHOOT.getPacketId(), ServerGameLogic::handlePlayerShootServer);
		this.registerPacketCallback(PacketType.HEARTBEAT.getPacketId(), ServerGameLogic::handleHeartbeatServer);
		this.registerPacketCallback(PacketType.TEXT.getPacketId(), ServerGameLogic::handleTextServer);
		this.registerPacketCallback(PacketType.COMMAND.getPacketId(), ServerGameLogic::handleCommandServer);
		this.registerPacketCallback(PacketType.LOAD_MAP.getPacketId(), ServerGameLogic::handleLoadMapServer);
		this.registerPacketCallback(PacketType.USE_ABILITY.getPacketId(), ServerGameLogic::handleUseAbilityServer);
		this.registerPacketCallback(PacketType.MOVE_ITEM.getPacketId(), ServerGameLogic::handleMoveItemServer);
	}

	private void registerPacketCallback(final byte packetId, final BiConsumer<RealmManagerServer, Packet> callback) {
		this.packetCallbacksServer.put(packetId, callback);
	}

	// Updates all game objects on the server
	public void update(double time) {
		// Update player specific game objects (bullets, the players themselves)
		for (final Map.Entry<Long, Player> player : this.realm.getPlayers().entrySet()) {
			final Player p = this.realm.getPlayer(player.getValue().getId());
			if (p == null) {
				continue;
			}

			final Runnable processGameObjects = () -> {
				this.processBulletHit(p);
				this.removeExpiredBullets();
			};
			// Rewrite this asap
			final Runnable checkAbilityUsage = () -> {

				for (final GameObject e : this.realm
						.getGameObjectsInBounds(this.getRealm().getTileManager().getRenderViewPort(p))) {
					if ((e instanceof Entity) || (e instanceof Enemy)) {
						Entity entCast = (Entity) e;
						entCast.removeExpiredEffects();
					}
				}
			};
			
			final Runnable updatePlayer = () -> {
				p.update(time);
				this.movePlayer(p);
			};
			// Run the player update tasks Asynchronously
			WorkerThread.submitAndRun(processGameObjects, updatePlayer, checkAbilityUsage);
		}
		// Once per tick update all non player game objects
		// (bullets, enemies)
		final Runnable processGameObjects = () -> {
			final GameObject[] gameObject = this.realm.getAllGameObjects();
			for (int i = 0; i < gameObject.length; i++) {
				if (gameObject[i] instanceof Enemy) {
					final Enemy enemy = ((Enemy) gameObject[i]);
					enemy.update(this, time);
				}

				if (gameObject[i] instanceof Bullet) {
					final Bullet bullet = ((Bullet) gameObject[i]);
					if (bullet != null) {
						bullet.update();
					}
				}
			}
		};
		WorkerThread.submitAndRun(processGameObjects);
	}

	private void movePlayer(final Player p) {
		if (!p.isFallen()) {
			if(!this.getRealm().getTileManager().collisionTile(p, p.getDx(), 0)) {
				p.xCol=false;
				p.getPos().x += p.getDx();
			}else {
				p.xCol=true;
			}
			
			if(!this.getRealm().getTileManager().collisionTile(p, 0, p.getDy())) {
				p.yCol=false;
				p.getPos().y += p.getDy();
			}else {
				p.yCol=true;
			}

			p.move();
		} else {
			p.xCol = true;
			p.yCol = true;
			if (p.getAni().hasPlayedOnce()) {
				p.resetPosition();
				p.setDx(0);
				p.setDy(0);
				p.setFallen(false);
			}
		}
	}

	// Invokes an ability usage server side for the given player at the
	// desired location if aplicable
	public void useAbility(final long playerId, final Vector2f pos) {
		final Player player = this.realm.getPlayer(playerId);
		final GameItem abilityItem = player.getAbility();
		if ((abilityItem == null) || (abilityItem.getEffect() == null))
			return;
		final Effect effect = abilityItem.getEffect();
		if (player.getMana() < effect.getMpCost())
			return;
		player.setMana(player.getMana() - effect.getMpCost());
		// If the ability is damaging (knight stun, archer arrow, wizard spell)
		if (((abilityItem.getDamage() != null) && (abilityItem.getEffect() != null))) {
			final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS
					.get(abilityItem.getDamage().getProjectileGroupId());

			final Vector2f dest = new Vector2f(pos.x, pos.y);
			dest.addX(player.getCam().getPos().x);
			dest.addY(player.getCam().getPos().y);
			Vector2f source = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
			final float angle = Bullet.getAngle(source, dest);

			for (final Projectile p : group.getProjectiles()) {
				final short offset = (short) (p.getSize() / (short) 2);
				short rolledDamage = player.getInventory()[0].getDamage().getInRange();
				rolledDamage += player.getComputedStats().getAtt();
				if (p.getPositionMode() == ProjectilePositionMode.TARGET_PLAYER) {
					this.addProjectile(0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
							p.getProjectileId(),
							source.clone(-offset, -offset), angle + Float.parseFloat(p.getAngle()), p.getSize(),
							p.getMagnitude(), p.getRange(), rolledDamage, false, p.getFlags(), p.getAmplitude(),
							p.getFrequency());
				} else {
					source = dest;
					this.addProjectile(0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
							p.getProjectileId(),
							source.clone(-offset, -offset), Float.parseFloat(p.getAngle()), p.getSize(),
							p.getMagnitude(), p.getRange(), rolledDamage, false, p.getFlags(), p.getAmplitude(),
							p.getFrequency());
				}

			}
		} else if ((abilityItem.getDamage() != null)) {
			final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS
					.get(abilityItem.getDamage().getProjectileGroupId());
			final Vector2f dest = new Vector2f(pos.x, pos.y);
			dest.addX(player.getCam().getPos().x);
			dest.addY(player.getCam().getPos().y);
			for (final Projectile p : group.getProjectiles()) {

				final short offset = (short) (p.getSize() / (short) 2);
				short rolledDamage = player.getInventory()[1].getDamage().getInRange();
				rolledDamage += player.getComputedStats().getAtt();
				this.addProjectile(0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
						p.getProjectileId(),
						dest.clone(-offset, -offset), Float.parseFloat(p.getAngle()), p.getSize(), p.getMagnitude(),
						p.getRange(), rolledDamage, false, p.getFlags(), p.getAmplitude(), p.getFrequency());
			}
		// If the ability is non damaging (rogue cloak, priest tome)
		} else if (abilityItem.getEffect() != null) {
			player.addEffect(effect.getEffectId(), effect.getDuration());
			if (abilityItem.getEffect().getEffectId().equals(EffectType.HEAL)) {
				player.addHealth(50);
			}
		}
	}

//	private List<Bullet> getBullets() {
//		final GameObject[] gameObject = this.getRealm()
//				.getGameObjectsInBounds(this.getRealm().getTileManager().getRenderViewPort());
//
//		final List<Bullet> results = new ArrayList<>();
//		for (int i = 0; i < gameObject.length; i++) {
//			if (gameObject[i] instanceof Bullet) {
//				results.add((Bullet) gameObject[i]);
//			}
//		}
//		return results;
//	}

	public void removeExpiredBullets() {
		final List<Bullet> toRemove = new ArrayList<>();

		for(final Bullet b : this.realm.getBullets().values()) {
			if(b.remove()) {
				toRemove.add(b);
			}
		}

		toRemove.forEach(bullet -> {
			this.expiredBullets.add(bullet.getId());
			this.realm.removeBullet(bullet);
		});
	}

	public void processBulletHit(final Player p) {
		final List<Bullet> results = this.getBullets(p);
		final GameObject[] gameObject = this.realm.getGameObjectsInBounds(this.getRealm().getTileManager().getRenderViewPort(p));
		final Player player = this.realm.getPlayer(p.getId());
		for (final Bullet b : results) {
			this.processPlayerHit(b, player);
		}

		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Enemy) {
				final Enemy enemy = ((Enemy) gameObject[i]);
				for (final Bullet b : results) {
					this.proccessEnemyHit(b, enemy);
				}
			}
		}
		this.proccessTerrainHit(p);
	}
	// This may not need to be synchronized
	public synchronized void enqueueServerPacket(final Packet packet) {
		this.outboundPacketQueue.add(packet);
	}

	private void proccessTerrainHit(final Player p) {
		final List<Bullet> toRemove = new ArrayList<>();
		final TileMap currentMap = this.realm.getTileManager().getCollisionLayer();
		Tile[] viewportTiles = null;
		if (currentMap == null)
			return;
		viewportTiles = this.realm.getTileManager().getCollisionTile(p.getPos());
		for (final Bullet b : this.getBullets(p)) {
			if (b.remove()) {
				toRemove.add(b);
				continue;
			}
			for (final Tile tile : viewportTiles) {
				if (tile == null || tile.isVoid()) {
					continue;
				}
				if (b.getBounds().intersect(new AABB(tile.getPos(), tile.getWidth(), tile.getHeight()))) {
					toRemove.add(b);
				}
			}
		}
		toRemove.forEach(bullet -> {
			this.expiredBullets.add(bullet.getId());
			this.realm.removeBullet(bullet);
		});
	}

	private  void processPlayerHit(final Bullet b, final Player p) {
		final Player player = this.realm.getPlayer(p.getId());
		if (player == null)
			return;
		if (b.getBounds().collides(0, 0, player.getBounds()) && b.isEnemy() && !b.isPlayerHit()) {
			final Stats stats = player.getComputedStats();
			b.setPlayerHit(true);
			final short minDmg = (short) (b.getDamage() * 0.15);
			short dmgToInflict = (short) (b.getDamage() - stats.getDef());
			if (dmgToInflict < minDmg) {
				dmgToInflict = minDmg;
			}
			player.setHealth(player.getHealth() - dmgToInflict, 0, false);
			this.expiredBullets.add(b.getId());
			this.realm.removeBullet(b);
		}
	}

	private void proccessEnemyHit(final Bullet b, final Enemy e) {
		if (this.realm.hasHitEnemy(b.getId(), e.getId()))
			return;
		if (b.getBounds().collides(0, 0, e.getBounds()) && !b.isEnemy()) {
			this.realm.hitEnemy(b.getId(), e.getId());

			e.setHealth(e.getHealth() - b.getDamage(), 0, false);
			if (b.hasFlag((short) 10) && !b.isEnemyHit()) {
				b.setEnemyHit(true);
			} else if (b.remove()) {
				this.expiredBullets.add(b.getId());
				this.realm.removeBullet(b);
			}

			if (b.hasFlag((short) 2)) {
				if (!e.hasEffect(EffectType.PARALYZED)) {
					e.addEffect(EffectType.PARALYZED, 5000);
				}
			}

			if (b.hasFlag((short) 3)) {
				if (!e.hasEffect(EffectType.STUNNED)) {
					e.addEffect(EffectType.STUNNED, 5000);
				}
			}
			
			if (e.getDeath()) {
				e.getSprite().setEffect(Sprite.EffectEnum.NORMAL);
				this.expiredBullets.add(b.getId());
				this.expiredEnemies.add(e.getId());
				this.realm.clearHitMap();
				this.realm.spawnRandomEnemy();
				this.realm.removeEnemy(e);
				this.realm.addLootContainer(new LootContainer(
						GameDataManager.SPRITE_SHEETS.get("entity/rotmg-items-1.png").getSprite(6, 7, 8, 8),
						e.getPos()));
			}
		}
	}

	public void addProjectile(final long id, final long targetPlayerId, final int projectileId, final int projectileGroupId,
			final Vector2f src, final Vector2f dest, final short size, final float magnitude, final float range, short damage, final boolean isEnemy,
			final List<Short> flags) {
		final Player player = this.realm.getPlayer(targetPlayerId);
		if (player == null)
			return;
		final ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		final SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		final Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		
		if (pg.getAngleOffset() != null) {
			bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
		}
		
		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}
		
		final long idToUse = id == 0l ? Realm.RANDOM.nextLong() : id;
		final Bullet b = new Bullet(id, projectileId, bulletImage, src, dest, size, magnitude, range, damage, isEnemy);
		b.setFlags(flags);
		this.realm.addBullet(b);
	}

	public void addProjectile(final long id, final long targetPlayerId, final int projectileId, final int projectileGroupId,
			final Vector2f src, final float angle, final short size, final float magnitude, final float range, short damage, final boolean isEnemy,
			final List<Short> flags, final short amplitude, final short frequency) {
		final Player player = this.realm.getPlayer(targetPlayerId);
		if (player == null)
			return;
		final ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		final SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		final Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		
		if (pg.getAngleOffset() != null) {
			bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
		}
		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}
		
		final long idToUse = id == 0l ? Realm.RANDOM.nextLong() : id;
		final Bullet b = new Bullet(idToUse, projectileId, bulletImage, src, angle, size, magnitude, range, damage, isEnemy);
		
		b.setAmplitude(amplitude);
		b.setFrequency(frequency);
		b.setFlags(flags);
		this.realm.addBullet(b);
	}

	private List<Bullet> getBullets(final Player p) {
		final GameObject[] gameObject = this.realm.getGameObjectsInBounds(this.realm.getTileManager().getRenderViewPort(p));

		final List<Bullet> results = new ArrayList<>();
		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Bullet) {
				results.add((Bullet) gameObject[i]);
			}
		}
		return results;
	}

	private void broadcastPacket(final Packet packet) {
		for (final Map.Entry<String, ProcessingThread> client : this.server.getClients().entrySet()) {
			if (!client.getValue().isHandshakeComplete()) {
				continue;
			}
			try {
				final OutputStream toClientStream = client.getValue().getClientSocket().getOutputStream();
				final DataOutputStream dosToClient = new DataOutputStream(toClientStream);
			} catch (Exception e) {
				RealmManagerServer.log.error("Failed to broadcast Packet to client {}", client.getKey());
			}
		}
	}
}
