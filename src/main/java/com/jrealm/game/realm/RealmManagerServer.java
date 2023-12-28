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
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.LootTier;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.model.ProjectilePositionMode;
import com.jrealm.game.state.PlayState;
import com.jrealm.game.tile.NetTile;
import com.jrealm.game.tile.Tile;
import com.jrealm.game.tile.TileMap;
import com.jrealm.game.tile.decorators.Beach0Decorator;
import com.jrealm.game.tile.decorators.RealmDecorator;
import com.jrealm.game.ui.TextEffect;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.TimedWorkerThread;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.TextEffectPacket;
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

	private Map<Long, Realm> realms = new HashMap<>();

	// private Realm realm;
	private boolean shutdown = false;

	private final Map<Byte, BiConsumer<RealmManagerServer, Packet>> packetCallbacksServer = new HashMap<>();
	private List<Vector2f> shotDestQueue;

	private Map<String, Long> remoteAddresses = new HashMap<>();
	private Map<Long, LoadPacket> playerLoadState = new HashMap<>();
	private Map<Long, UpdatePacket> playerUpdateState = new HashMap<>();
	private Map<Long, UnloadPacket> playerUnloadState = new HashMap<>();
	private Map<Long, LoadMapPacket> playerLoadMapState = new HashMap<>();

	private UnloadPacket lastUnload;
	private volatile Queue<Packet> outboundPacketQueue = new ConcurrentLinkedQueue<>();
	private volatile Map<Long, ConcurrentLinkedQueue<Packet>> playerOutboundPacketQueue = new HashMap<Long, ConcurrentLinkedQueue<Packet>>();
	private List<RealmDecorator> realmDecorators = new ArrayList<>();
	private Semaphore realmLock = new Semaphore(1);

	public RealmManagerServer() {
		this.registerRealmDecorators();
		this.registerPacketCallbacks();
		this.server = new SocketServer(2222);
		this.shotDestQueue = new ArrayList<>();
		WorkerThread.submitAndForkRun(this.server);
	}

	// Adds a specified amount of random headless players
	public void spawnTestPlayers(final long realmId, final int count) {
		final Realm targetRealm = this.realms.get(realmId);
		final Vector2f spawnPos = targetRealm.getTileManager().getSafePosition();
		final Runnable spawnTestPlayers = ()-> {
			final Random random = new Random(Instant.now().toEpochMilli());
			for(int i = 0 ; i < count; i++) {
				final CharacterClass classToSpawn = CharacterClass.getCharacterClasses().get(random.nextInt(CharacterClass.getCharacterClasses().size()));
				final Camera c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
				try {
					final Player player = new Player(Realm.RANDOM.nextLong(), c, GameDataManager.loadClassSprites(classToSpawn),
							spawnPos, GlobalConstants.PLAYER_SIZE, classToSpawn);
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
					// TODO:
					final long newId = targetRealm.addPlayer(player);
					//Thread.sleep(500);
				}catch(Exception e) {
					RealmManagerServer.log.error("Failed to spawn test character of class type {}. Reason: {}", classToSpawn, e);
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

		final TimedWorkerThread workerThread = new TimedWorkerThread(tick, 64);
		WorkerThread.submitAndForkRun(workerThread);
		RealmManagerServer.log.info("[SERVER] RealmManagerServer exiting run().");
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
		// TODO: Possibly rework this queue as we dont usually send stuff globally
		while(!this.outboundPacketQueue.isEmpty()) {
			packetsToBroadcast.add(this.outboundPacketQueue.remove());
		}

		final List<String> disconnectedClients = new ArrayList<>();
		for (final Map.Entry<String, ProcessingThread> client : this.server.getClients().entrySet()) {
			try {
				// Dequeue and send any player specific packets
				final List<Packet> playerPackets = new ArrayList<>();
				final ConcurrentLinkedQueue<Packet> playerPacketsToSend = this.playerOutboundPacketQueue
						.get(this.remoteAddresses.get(client.getKey()));

				while ((playerPacketsToSend != null) && !playerPacketsToSend.isEmpty()) {
					playerPackets.add(playerPacketsToSend.remove());
				}
				final OutputStream toClientStream = client.getValue().getClientSocket().getOutputStream();
				final DataOutputStream dosToClient = new DataOutputStream(toClientStream);

				for (final Packet packet : packetsToBroadcast) {
					packet.serializeWrite(dosToClient);
				}

				for (final Packet packet : playerPackets) {
					packet.serializeWrite(dosToClient);
				}
			}catch(Exception e) {
				disconnectedClients.add(client.getKey());
				RealmManagerServer.log.error("Failed to get OutputStream to Client. Reason: {}", e);
			}
		}
	}

	public void enqueueGameData() {
		final List<String> disconnectedClients = new ArrayList<>();
		// TODO: Parallelize work for each realm
		// For each realm we have to do work for
		for (final Map.Entry<Long, Realm> realmEntry : this.realms.entrySet()) {
			final Realm realm = realmEntry.getValue();
			for (final Map.Entry<Long, Player> player : realm.getPlayers().entrySet()) {
				try {
					// Get UpdatePacket for this player and all players in this players viewport
					// Contains, player stat info, inventory, status effects, health and mana data
					final UpdatePacket uPacket = realm.getPlayerAsPacket(player.getValue().getId());

					// Get the background + collision tiles in this players viewport
					// condensed into a single array
					final NetTile[] netTilesForPlayer = realm.getTileManager().getLoadMapTiles(player.getValue());
					// Build those tiles into a load map packet (NetTile[] wrapper)
					final LoadMapPacket newLoadMapPacket = LoadMapPacket.from(realm.getRealmId(), netTilesForPlayer);

					// If we dont have load map state for this player, map it and
					// then transmit all the tiles
					if (this.playerLoadMapState.get(player.getKey()) == null) {
						this.playerLoadMapState.put(player.getKey(), newLoadMapPacket);
						this.enqueueServerPacket(player.getValue(), newLoadMapPacket);
					} else {
						// Get the previous loadMap packet and check for Delta,
						// only send the delta to the client
						final LoadMapPacket oldLoadMapPacket = this.playerLoadMapState.get(player.getKey());
						// Custom equals impl
						if (!oldLoadMapPacket.equals(newLoadMapPacket)) {
							final LoadMapPacket loadMapDiff = oldLoadMapPacket.difference(newLoadMapPacket);
							this.playerLoadMapState.put(player.getKey(), newLoadMapPacket);
							if (loadMapDiff != null) {
								this.enqueueServerPacket(player.getValue(), loadMapDiff);
							}
						}
					}
					// Get LoadPacket for this player
					// Contains newly spawned bullets, entities, players
					final LoadPacket load = realm
							.getLoadPacket(realm.getTileManager().getRenderViewPort(player.getValue()));

					// Get the posX, posY, dX, dY of all Entities in this players viewport
					final ObjectMovePacket mPacket = realm
							.getGameObjectsAsPackets(realm.getTileManager().getRenderViewPort(player.getValue()));


					if (this.playerUpdateState.get(player.getKey()) == null) {
						this.playerUpdateState.put(player.getKey(), uPacket);
						this.enqueueServerPacket(player.getValue(), uPacket);
					} else {
						final UpdatePacket old = this.playerUpdateState.get(player.getKey());
						if (!old.equals(uPacket)) {
							this.playerUpdateState.put(player.getKey(), uPacket);
							this.enqueueServerPacket(player.getValue(), uPacket);
						}
					}

					// Only transmit the LoadPacket if its state is changed (it can potentially be
					// large).
					// If the state is changed, only transmit the DELTA data
					if (this.playerLoadState.get(player.getKey()) == null) {
						this.playerLoadState.put(player.getKey(), load);
						this.enqueueServerPacket(player.getValue(), load);
					} else {
						final LoadPacket old = this.playerLoadState.get(player.getKey());
						if (!old.equals(load)) {
							// Get the LoadPacket delta
							final LoadPacket toSend = old.combine(load);
							this.playerLoadState.put(player.getKey(), load);
							this.enqueueServerPacket(player.getValue(), toSend);

							// Unload the delta objects that were in the old LoadPacket
							// but are NOT in the new LoadPacket
							final UnloadPacket unloadDelta = old.difference(load);
							if (unloadDelta.isNotEmpty()) {
								this.enqueueServerPacket(player.getValue(), unloadDelta);
							}
						}
					}

					// If the ObjectMove packet isnt empty
					if (mPacket != null) {
						this.enqueueServerPacket(player.getValue(), mPacket);
					}

				} catch (Exception e) {
					RealmManagerServer.log.error("Failed to build game data for Player {}. Reason: {}", player.getKey(),
							e);
				}
			}
		}
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
				final Realm playerLocation = this.searchRealmsForPlayers(dcPlayerId);
				playerLocation.getExpiredPlayers().add(dcPlayerId);
				this.server.getClients().remove(thread.getKey());
				playerLocation.getPlayers().remove(dcPlayerId);
			}
		}
	}

	public Portal getClosestPortal(final long realmId, final Vector2f pos, final float limit) {
		float best = Float.MAX_VALUE;
		Portal bestPortal = null;
		final Realm targetRealm = this.realms.get(realmId);
		for (final Portal portal : targetRealm.getPortals().values()) {
			float dist = portal.getPos().distanceTo(pos);
			if ((dist < best) && (dist <= limit)) {
				best = dist;
				bestPortal = portal;
			}
		}
		return bestPortal;
	}

	public Player getClosestPlayer(final long realmId, final Vector2f pos, final float limit) {
		float best = Float.MAX_VALUE;
		Player bestPlayer = null;
		final Realm targetRealm = this.realms.get(realmId);
		for (final Player player : targetRealm.getPlayers().values()) {
			final float dist = player.getPos().distanceTo(pos);
			if ((dist < best) && (dist <= limit)) {
				best = dist;
				bestPlayer = player;
			}
		}
		return bestPlayer;
	}

	public LootContainer getClosestLootContainer(final long realmId, final Vector2f pos, final float limit) {
		float best = Float.MAX_VALUE;
		LootContainer bestLoot = null;
		final Realm targetRealm = this.realms.get(realmId);
		for (final LootContainer lootContainer : targetRealm.getLoot().values()) {
			float dist = lootContainer.getPos().distanceTo(pos);
			if ((dist < best) && (dist <= limit)) {
				best = dist;
				bestLoot = lootContainer;
			}
		}
		return bestLoot;
	}

	private UnloadPacket getUnloadPacket(long realmId) throws Exception {
		final Realm targetRealm = this.realms.get(realmId);

		final Long[] expiredBullets = targetRealm.getExpiredBullets().toArray(new Long[0]);
		final Long[] expiredEnemies = targetRealm.getExpiredEnemies().toArray(new Long[0]);
		final Long[] expiredPlayers = targetRealm.getExpiredPlayers().toArray(new Long[0]);
		targetRealm.getExpiredPlayers().clear();
		targetRealm.getExpiredBullets().clear();
		targetRealm.getExpiredEnemies().clear();
		final List<Long> lootContainers = targetRealm.getLoot().values().stream()
				.filter(lc -> lc.isExpired() || lc.isEmpty()).map(LootContainer::getLootContainerId)
				.collect(Collectors.toList());
		for(final Long lcId: lootContainers) {
			targetRealm.getLoot().remove(lcId);
		}
		return UnloadPacket.from(expiredPlayers, lootContainers.toArray(new Long[0]), expiredBullets, expiredEnemies,
				new Long[0]);
	}

	public void tryDecorate(final Realm realm) {
		final RealmDecorator decorator = this.getRealmDecorator(realm.getMapId());
		if (decorator != null) {
			decorator.decorate(realm);
		}
	}

	private RealmDecorator getRealmDecorator(int mapId) {
		RealmDecorator result = null;
		for (final RealmDecorator decorator : this.realmDecorators) {
			if (decorator.getTargetMapId() == mapId) {
				result = decorator;
			}
		}
		return result;
	}

	private void registerRealmDecorators() {
		this.realmDecorators.add(new Beach0Decorator());
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
		this.registerPacketCallback(PacketType.USE_PORTAL.getPacketId(), ServerGameLogic::handleUsePortalServer);
	}

	private void registerPacketCallback(final byte packetId, final BiConsumer<RealmManagerServer, Packet> callback) {
		this.packetCallbacksServer.put(packetId, callback);
	}

	// Updates all game objects on the server
	public void update(double time) {
		// For each world on the server
		for (final Map.Entry<Long, Realm> realmEntry : this.realms.entrySet()) {
			final Realm realm = realmEntry.getValue();
			// Update player specific game objects (bullets, the players themselves)
			for (final Map.Entry<Long, Player> player : realm.getPlayers().entrySet()) {
				final Player p = realm.getPlayer(player.getValue().getId());
				if (p == null) {
					continue;
				}

				final Runnable processGameObjects = () -> {
					this.processBulletHit(realm.getRealmId(), p);
				};
				// Rewrite this asap
				final Runnable checkAbilityUsage = () -> {

					for (final GameObject e : realm
							.getGameObjectsInBounds(realm.getTileManager().getRenderViewPort(p))) {
						if ((e instanceof Entity)) {
							Entity entCast = (Entity) e;
							entCast.removeExpiredEffects();
						}
					}
				};

				final Runnable updatePlayer = () -> {
					p.update(time);
					this.movePlayer(realm.getRealmId(), p);
				};
				// Run the player update tasks Asynchronously
				WorkerThread.submitAndRun(processGameObjects, updatePlayer, checkAbilityUsage);
			}
			// Once per tick update all non player game objects
			// (bullets, enemies)
			final Runnable processGameObjects = () -> {
				final GameObject[] gameObject = realm.getAllGameObjects();
				for (int i = 0; i < gameObject.length; i++) {
					if (gameObject[i] instanceof Enemy) {
						final Enemy enemy = ((Enemy) gameObject[i]);
						enemy.update(realm.getRealmId(), this, time);
					}

					if (gameObject[i] instanceof Bullet) {
						final Bullet bullet = ((Bullet) gameObject[i]);
						if (bullet != null) {
							bullet.update();
						}
					}
				}
			};

			// Used to dynamically re-render changed loot containers (chests) on the client
			// if their
			// contents change in a server tick (receive MoveItem packet from client this
			// tick)
			for (LootContainer lc : realm.getLoot().values()) {
				lc.setContentsChanged(false);
			}

			WorkerThread.submitAndRun(processGameObjects);
		}

		Runnable removeExpiredBullets = () -> {
			this.removeExpiredBullets();
		};

		WorkerThread.submitAndRun(removeExpiredBullets);

	}

	private void movePlayer(final long realmId, final Player p) {
		final Realm targetRealm = this.realms.get(realmId);

		if (!targetRealm.getTileManager().collisionTile(p, p.getDx(), 0)
				&& !targetRealm.getTileManager().collidesXLimit(p, p.getDx())) {
			p.xCol = false;
			if (targetRealm.getTileManager().collidesSlowTile(p)) {
				p.getPos().x += p.getDx() / 3.0f;

			} else {
				p.getPos().x += p.getDx();
			}
		} else {
			p.xCol = true;
		}

		if (!targetRealm.getTileManager().collisionTile(p, 0, p.getDy())
				&& !targetRealm.getTileManager().collidesYLimit(p, p.getDy())) {
			p.yCol = false;
			if (targetRealm.getTileManager().collidesSlowTile(p)) {
				p.getPos().y += p.getDy() / 3.0f;

			} else {
				p.getPos().y += p.getDy();

			}
		} else {
			p.yCol = true;
		}

		p.move();
	}

	// Invokes an ability usage server side for the given player at the
	// desired location if aplicable
	public void useAbility(final long realmId, final long playerId, final Vector2f pos) {
		final Realm targetRealm = this.realms.get(realmId);

		final Player player = targetRealm.getPlayer(playerId);
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
					this.addProjectile(realmId, 0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
							p.getProjectileId(),
							source.clone(-offset, -offset), angle + Float.parseFloat(p.getAngle()), p.getSize(),
							p.getMagnitude(), p.getRange(), rolledDamage, false, p.getFlags(), p.getAmplitude(),
							p.getFrequency());
				} else {
					source = dest;
					this.addProjectile(realmId, 0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
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
				this.addProjectile(realmId, 0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
						p.getProjectileId(),
						dest.clone(-offset, -offset), Float.parseFloat(p.getAngle()), p.getSize(), p.getMagnitude(),
						p.getRange(), rolledDamage, false, p.getFlags(), p.getAmplitude(), p.getFrequency());
			}
			// If the ability is non damaging (rogue cloak, priest tome)
		} else if (abilityItem.getEffect() != null) {
			player.addEffect(effect.getEffectId(), effect.getDuration());
			if (abilityItem.getEffect().getEffectId().equals(EffectType.HEAL)) {
				player.setHealth(player.getHealth() + 50);
			}
		}
	}

	public void removeExpiredBullets() {
		for (final Map.Entry<Long, Realm> realmEntry : this.realms.entrySet()) {
			final Realm realm = realmEntry.getValue();

			final List<Bullet> toRemove = new ArrayList<>();
			for (final Bullet b : realm.getBullets().values()) {
				if (b.remove()) {
					toRemove.add(b);
				}
			}
			toRemove.forEach(bullet -> {
				realm.getExpiredBullets().add(bullet.getId());
				realm.removeBullet(bullet);
			});
		}
	}

	public void processBulletHit(final long realmId, final Player p) {
		final Realm targetRealm = this.realms.get(realmId);
		final List<Bullet> results = this.getBullets(realmId, p);
		final GameObject[] gameObject = targetRealm
				.getGameObjectsInBounds(targetRealm.getTileManager().getRenderViewPort(p));
		final Player player = targetRealm.getPlayer(p.getId());
		for (final Bullet b : results) {
			this.processPlayerHit(realmId, b, player);
		}

		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Enemy) {
				final Enemy enemy = ((Enemy) gameObject[i]);
				for (final Bullet b : results) {
					this.proccessEnemyHit(realmId, b, enemy);
				}
			}
		}
		this.proccessTerrainHit(realmId, p);
	}
	// This may not need to be synchronized
	// Enqueues a server packet to be transmitted to all players
	public synchronized void enqueueServerPacket(final Packet packet) {
		this.outboundPacketQueue.add(packet);
	}

	// This may not need to be synchronized
	// Enqueues a packet to be transmitted to only Player player
	public synchronized void enqueueServerPacket(final Player player, final Packet packet) {
		if (this.playerOutboundPacketQueue.get(player.getId()) == null) {
			final ConcurrentLinkedQueue<Packet> packets = new ConcurrentLinkedQueue<>();
			packets.add(packet);
			this.playerOutboundPacketQueue.put(player.getId(), packets);
		} else {
			this.playerOutboundPacketQueue.get(player.getId()).add(packet);
		}
	}

	private void proccessTerrainHit(final long realmId, final Player p) {
		final Realm targetRealm = this.realms.get(realmId);

		final List<Bullet> toRemove = new ArrayList<>();
		final TileMap currentMap = targetRealm.getTileManager().getCollisionLayer();
		Tile[] viewportTiles = null;
		if (currentMap == null)
			return;
		viewportTiles = targetRealm.getTileManager().getCollisionTiles(p.getPos());
		for (final Bullet b : this.getBullets(realmId, p)) {
			if (b.remove()) {
				toRemove.add(b);
				continue;
			}
			for (final Tile tile : viewportTiles) {
				if ((tile == null) || tile.isVoid()) {
					continue;
				}
				if (b.getBounds().intersect(new AABB(tile.getPos(), tile.getWidth(), tile.getHeight()))) {
					toRemove.add(b);
				}
			}
		}
		toRemove.forEach(bullet -> {
			targetRealm.getExpiredBullets().add(bullet.getId());
			targetRealm.removeBullet(bullet);
		});
	}

	private void processPlayerHit(final long realmId, final Bullet b, final Player p) {
		final Realm targetRealm = this.realms.get(realmId);

		final Player player = targetRealm.getPlayer(p.getId());
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
			try {
				this.enqueueServerPacket(player, TextEffectPacket.from(TextEffect.DAMAGE, "-" + dmgToInflict));
			} catch (Exception e) {
				RealmManagerServer.log.error("Failed to send Damage TextEffect Packet to Player {}. Reason: {}",
						p.getId(), e);
			}
			player.setHealth(player.getHealth() - dmgToInflict);
			targetRealm.getExpiredBullets().add(b.getId());
			targetRealm.removeBullet(b);

			if (p.getDeath()) {
				try {
					final String remoteAddrDeath = this.getRemoteAddressMapRevered().get(player.getId());
					final LootContainer graveLoot = new LootContainer(LootTier.BROWN, p.getPos().clone(),
							p.getSlots(4, 12));
					targetRealm.addLootContainer(graveLoot);
					targetRealm.getExpiredPlayers().add(player.getId());
					this.getServer().getClients().remove(remoteAddrDeath);
					targetRealm.removePlayer(player);
				} catch (Exception e) {
					RealmManagerServer.log.error("Failed to Remove dead Player {}. Reason: {}", e);
				}

			}
		}
	}

	private void proccessEnemyHit(final long realmId, final Bullet b, final Enemy e) {
		final Realm targetRealm = this.realms.get(realmId);

		if (targetRealm.hasHitEnemy(b.getId(), e.getId()))
			return;
		if (b.getBounds().collides(0, 0, e.getBounds()) && !b.isEnemy()) {
			targetRealm.hitEnemy(b.getId(), e.getId());

			e.setHealth(e.getHealth() - b.getDamage());
			if (b.hasFlag((short) 10) && !b.isEnemyHit()) {
				b.setEnemyHit(true);
			} else if (b.remove()) {
				targetRealm.getExpiredBullets().add(b.getId());
				targetRealm.removeBullet(b);
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
				for(Player player : targetRealm.getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(e))){
					EnemyModel model = GameDataManager.ENEMIES.get(e.getEnemyId());
					player.incrementExperience(model.getXp());
				}

				Random random = new Random(Instant.now().toEpochMilli());
				e.getSprite().setEffect(Sprite.EffectEnum.NORMAL);
				targetRealm.getExpiredBullets().add(b.getId());
				targetRealm.getExpiredEnemies().add(e.getId());
				targetRealm.clearHitMap();
				targetRealm.spawnRandomEnemy();
				targetRealm.removeEnemy(e);

				// TODO: Maybe find a better way to introduce randomness to drops.
				if(Realm.RANDOM.nextInt(20)<1) {
					targetRealm.addPortal(new Portal(random.nextLong(), (short) 2, e.getPos().withNoise(128, 128)));
				}
				if ((targetRealm.getMapId() != 3) && (Realm.RANDOM.nextInt(20) < 5)) {
					targetRealm.addPortal(new Portal(random.nextLong(), (short) 0, e.getPos().withNoise(128, 128)));
				}
				if (Realm.RANDOM.nextInt(20) < 10) {
					targetRealm.addLootContainer(new LootContainer(LootTier.BLUE, e.getPos().withNoise(128, 128)));
				}
			}
		}
	}

	public void addProjectile(final long realmId, final long id, final long targetPlayerId, final int projectileId,
			final int projectileGroupId,
			final Vector2f src, final Vector2f dest, final short size, final float magnitude, final float range, short damage, final boolean isEnemy,
			final List<Short> flags) {
		final Realm targetRealm = this.realms.get(realmId);
		final Player player = targetRealm.getPlayer(targetPlayerId);
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
		targetRealm.addBullet(b);
	}

	public void addProjectile(final long realmId, final long id, final long targetPlayerId, final int projectileId,
			final int projectileGroupId,
			final Vector2f src, final float angle, final short size, final float magnitude, final float range, short damage, final boolean isEnemy,
			final List<Short> flags, final short amplitude, final short frequency) {
		final Realm targetRealm = this.realms.get(realmId);
		final Player player = targetRealm.getPlayer(targetPlayerId);
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
		targetRealm.addBullet(b);
	}

	private List<Bullet> getBullets(final long realmId, final Player p) {
		final Realm targetRealm = this.realms.get(realmId);
		final GameObject[] gameObject = targetRealm
				.getGameObjectsInBounds(targetRealm.getTileManager().getRenderViewPort(p));

		final List<Bullet> results = new ArrayList<>();
		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Bullet) {
				results.add((Bullet) gameObject[i]);
			}
		}
		return results;
	}

	public void clearPlayerState(long playerId) {
		this.playerLoadState.remove(playerId);
		this.playerUpdateState.remove(playerId);
		this.playerUnloadState.remove(playerId);
		this.playerLoadMapState.remove(playerId);
	}

	public Map<Long, String> getRemoteAddressMapRevered() {
		final Map<Long, String> result = new HashMap<>();
		for (final Entry<String, Long> entry : this.remoteAddresses.entrySet()) {
			result.put(entry.getValue(), entry.getKey());
		}
		return result;
	}

	// Adds a realm to the map of realms after trying to decorate
	// the realm terrain using any decorators
	public void addRealm(final Realm realm) {
		this.tryDecorate(realm);
		this.realms.put(realm.getRealmId(), realm);
	}

	public Realm searchRealmsForPlayers(long playerId) {
		Realm found = null;
		for (Map.Entry<Long, Realm> realm : this.realms.entrySet()) {
			for (Player player : realm.getValue().getPlayers().values()) {
				if (player.getId() == playerId) {
					found = realm.getValue();
				}
			}
		}
		return found;
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
