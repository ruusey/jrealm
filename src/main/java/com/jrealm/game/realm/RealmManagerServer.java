package com.jrealm.game.realm;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

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
import com.jrealm.game.messaging.CommandType;
import com.jrealm.game.messaging.LoginRequestMessage;
import com.jrealm.game.messaging.LoginResponseMessage;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.model.ProjectilePositionMode;
import com.jrealm.game.states.PlayState;
import com.jrealm.game.tiles.TileMap;
import com.jrealm.game.tiles.blocks.Tile;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.Cardinality;
import com.jrealm.game.util.TimedWorkerThread;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.ProcessingThread;
import com.jrealm.net.server.SocketServer;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.net.server.packet.UseAbilityPacket;

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
	private Map<String, Long> remoteAddresses = new HashMap<>();
	public RealmManagerServer(Realm realm) {
		this.registerPacketCallbacks();
		this.realm = realm;
		this.server = new SocketServer(2222);
		this.shotDestQueue = new ArrayList<>();
		this.expiredEnemies = new ArrayList<>();
		this.expiredBullets = new ArrayList<>();
		WorkerThread.submitAndForkRun(this.server);
		this.getRealm().loadMap("tile/vault.xml", null);

	}

	private void addTestPlayer() {
		Camera c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
		CharacterClass cls = CharacterClass.ARCHER;
		Player player = new Player(Realm.RANDOM.nextLong(), c, GameDataManager.loadClassSprites(cls),
				new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
						(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
				GlobalConstants.PLAYER_SIZE, cls);
		player.setName("Dingus");
		player.equipSlots(PlayState.getStartingEquipment(cls));
		player.setMaxSpeed(0.6f);
		player.setDown(true);
		player.setRight(true);
		player.setHeadless(true);
		long newId = this.getRealm().addPlayer(player);

		//		c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
		//		cls = CharacterClass.PALLADIN;
		//		player = new Player(Realm.RANDOM.nextLong(), c, GameDataManager.loadClassSprites(cls),
		//				new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
		//						(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
		//				GlobalConstants.PLAYER_SIZE, cls);
		//		player.equipSlots(PlayState.getStartingEquipment(cls));
		//		player.setMaxSpeed(0.6f);
		//		player.setLeft(true);
		//		player.setDown(true);
		//		newId = this.getRealm().addPlayer(player);

	}

	@Override
	public void run() {
		RealmManagerServer.log.info("Starting JRealm Server");
		Runnable tick = () -> {
			this.tick();
			this.update(0);
		};


		TimedWorkerThread workerThread = new TimedWorkerThread(tick, 32);
		WorkerThread.submitAndForkRun(workerThread);

		RealmManagerServer.log.info("RealmManager exiting run().");
	}

	private void tick() {
		try {
			Runnable broadcastGameData = () -> {
				this.broadcastGameData();
			};

			Runnable processServerPackets = () -> {
				this.processServerPackets();
			};

			WorkerThread.submitAndRun(broadcastGameData, processServerPackets);
		} catch (Exception e) {
			RealmManagerServer.log.error("Failed to sleep");
		}
	}

	public void broadcastGameData() {
		UnloadPacket unload = null;
		try {
			unload = this.getUnloadPacket();
		} catch (Exception e) {
			RealmManagerServer.log.error("Failed to create unload packet. Reason: {}", e);
		}
		final UnloadPacket unloadToBroadcast = unload;
		final List<String> disconnectedClients = new ArrayList<>();
		for (Map.Entry<String, ProcessingThread> client : this.server.getClients().entrySet()) {
			if (!client.getValue().isHandshakeComplete()) {
				continue;
			}
			List<Runnable> playerWork = new ArrayList<>();
			for (Map.Entry<Long, Player> player : this.realm.getPlayers().entrySet()) {
				try {
					List<UpdatePacket> uPackets = this.realm
							.getPlayersAsPackets(player.getValue().getCam().getBounds());
					ObjectMovePacket mPacket = this.realm
							.getGameObjectsAsPackets(player.getValue().getCam().getBounds());
					OutputStream toClientStream = client.getValue().getClientSocket().getOutputStream();
					DataOutputStream dosToClient = new DataOutputStream(toClientStream);

					for (UpdatePacket packet : uPackets) {
						packet.serializeWrite(dosToClient);
					}

					LoadPacket load = this.realm
							.getLoadPacket(this.realm.getTileManager().getRenderViewPort(player.getValue()));
					load.serializeWrite(dosToClient);
					if (unloadToBroadcast != null) {
						unloadToBroadcast.serializeWrite(dosToClient);
					}
					if (mPacket != null) {
						mPacket.serializeWrite(dosToClient);
					}

				} catch (Exception e) {
					disconnectedClients.add(client.getKey());
					RealmManagerServer.log.error("Failed to get OutputStream to Client");
				}

				// playerWork.add(perPlayerWork);
			}
			// WorkerThread.submitAndRun(playerWork.toArray(new Runnable[0]));
		}
		for(String disconnectedClient : disconnectedClients) {
			this.realm.getPlayers().remove(this.getRemoteAddresses().get(disconnectedClient));
			this.server.getClients().remove(disconnectedClient);
		}
	}

	public void processServerPackets() {
		for(ProcessingThread thread: this.getServer().getClients().values()) {
			while(!thread.getPacketQueue().isEmpty()) {
				Packet packet = thread.getPacketQueue().remove();
				try {
					Packet created = Packet.newInstance(packet.getId(), packet.getData());
					created.setSrcIp(packet.getSrcIp());
					this.packetCallbacksServer.get(created.getId()).accept(this, created);
				} catch (Exception e) {
					RealmManagerServer.log.error("Failed to process server packets {}", e);
				}
			}
		}
	}

	public Player getClosestPlayer(Vector2f pos, float limit) {
		float best = Float.MAX_VALUE;
		Player bestPlayer = null;
		for (Player player : this.realm.getPlayers().values()) {
			float dist = player.getPos().distanceTo(pos);
			if ((dist < best) && (dist <= limit)) {
				best = dist;
				bestPlayer = player;
			}
		}
		return bestPlayer;
	}

	private UnloadPacket getUnloadPacket() throws Exception {
		Long[] expiredBullets = this.expiredBullets.toArray(new Long[0]);
		Long[] expiredEnemies = this.expiredEnemies.toArray(new Long[0]);
		this.expiredBullets.clear();
		this.expiredEnemies.clear();
		return UnloadPacket.from(new Long[0], new Long[0], expiredBullets, expiredEnemies);
	}

	private void registerPacketCallbacks() {
		this.registerPacketCallback(PacketType.PLAYER_MOVE.getPacketId(), RealmManagerServer::handlePlayerMoveServer);
		this.registerPacketCallback(PacketType.PLAYER_SHOOT.getPacketId(), RealmManagerServer::handlePlayerShootServer);
		this.registerPacketCallback(PacketType.HEARTBEAT.getPacketId(), RealmManagerServer::handleHeartbeatServer);
		this.registerPacketCallback(PacketType.TEXT.getPacketId(), RealmManagerServer::handleTextServer);
		this.registerPacketCallback(PacketType.COMMAND.getPacketId(), RealmManagerServer::handleCommandServer);
		this.registerPacketCallback(PacketType.LOAD_MAP.getPacketId(), RealmManagerServer::handleLoadMapServer);
		this.registerPacketCallback(PacketType.USE_ABILITY.getPacketId(), RealmManagerServer::handleUseAbilityServer);
	}

	private void registerPacketCallback(byte packetId, BiConsumer<RealmManagerServer, Packet> callback) {
		this.packetCallbacksServer.put(packetId, callback);
	}

	public void update(double time) {
		for (Map.Entry<Long, Player> player : this.realm.getPlayers().entrySet()) {
			Player p = this.realm.getPlayer(player.getValue().getId());
			if (p == null) {
				continue;
			}

			Runnable playerShootDequeue = () -> {
				for (int i = 0; i < this.shotDestQueue.size(); i++) {
					Vector2f dest = this.shotDestQueue.remove(i);

					dest.addX(p.getCam().getPos().x);
					dest.addY(p.getCam().getPos().y);

					Vector2f source = p.getPos().clone(p.getSize() / 2, p.getSize() / 2);
					ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(p.getWeaponId());
					float angle = Bullet.getAngle(source, dest);
					for (Projectile proj : group.getProjectiles()) {
						short offset = (short) (p.getSize() / (short) 2);
						short rolledDamage = p.getInventory()[0].getDamage().getInRange();
						rolledDamage += p.getComputedStats().getAtt();
						this.addProjectile(0l, p.getId(), proj.getProjectileId(), p.getWeaponId(),
								source.clone(-offset, -offset), angle + Float.parseFloat(proj.getAngle()),
								proj.getSize(), proj.getMagnitude(), proj.getRange(), rolledDamage, false,
								proj.getFlags(), proj.getAmplitude(), proj.getFrequency());
					}
				}

			};
			Runnable processGameObjects = () -> {

				this.processBulletHit(p);
			};
			// Rewrite this asap
			Runnable checkAbilityUsage = () -> {

				for (GameObject e : this.realm
						.getGameObjectsInBounds(this.getRealm().getTileManager().getRenderViewPort(p))) {
					if ((e instanceof Entity) || (e instanceof Enemy)) {
						Entity entCast = (Entity) e;
						entCast.removeExpiredEffects();
					}
				}
			};
			Runnable updatePlayerAndUi = () -> {
				p.update(time);
				this.movePlayer(p);
			};
			WorkerThread.submitAndRun(playerShootDequeue, processGameObjects, updatePlayerAndUi, checkAbilityUsage);
		}

		Runnable processGameObjects = () -> {
			GameObject[] gameObject = this.realm.getAllGameObjects();
			for (int i = 0; i < gameObject.length; i++) {
				if (gameObject[i] instanceof Enemy) {
					Enemy enemy = ((Enemy) gameObject[i]);
					enemy.update(this, time);
				}

				if (gameObject[i] instanceof Bullet) {
					Bullet bullet = ((Bullet) gameObject[i]);
					if (bullet != null) {
						bullet.update();
					}
				}
			}
		};

		WorkerThread.submitAndRun(processGameObjects);
	}

	private void movePlayer(Player p) {
		if (!p.isFallen()) {
			p.move();
			if (!p.getTc().collisionTile(this.realm.getTileManager().getTm().get(1).getBlocks(), p.getDx(), 0)) {
				p.getPos().x += p.getDx();
				p.xCol = false;
			} else {
				p.xCol = true;
			}
			if (!p.getTc().collisionTile(this.realm.getTileManager().getTm().get(1).getBlocks(), 0, p.getDy())) {
				p.getPos().y += p.getDy();
				p.yCol = false;
			} else {
				p.yCol = true;
			}

			p.getTc().normalTile(p.getDx(), 0);
			p.getTc().normalTile(0, p.getDy());

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

	private void useAbility(long playerId, Vector2f pos) {
		Player player = this.realm.getPlayer(playerId);
		GameItem abilityItem = player.getAbility();
		if ((abilityItem == null) || (abilityItem.getEffect() == null))
			return;
		Effect effect = abilityItem.getEffect();
		if (player.getMana() < effect.getMpCost())
			return;
		player.setMana(player.getMana() - effect.getMpCost());

		if (((abilityItem.getDamage() != null) && (abilityItem.getEffect() != null))) {
			ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS
					.get(abilityItem.getDamage().getProjectileGroupId());

			Vector2f dest = new Vector2f(pos.x, pos.y);
			dest.addX(player.getCam().getPos().x);
			dest.addY(player.getCam().getPos().y);
			Vector2f source = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
			float angle = Bullet.getAngle(source, dest);

			for (Projectile p : group.getProjectiles()) {
				short offset = (short) (p.getSize() / (short) 2);
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
			ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS
					.get(abilityItem.getDamage().getProjectileGroupId());
			Vector2f dest = new Vector2f(pos.x, pos.y);
			dest.addX(player.getCam().getPos().x);
			dest.addY(player.getCam().getPos().y);
			for (Projectile p : group.getProjectiles()) {

				short offset = (short) (p.getSize() / (short) 2);
				short rolledDamage = player.getInventory()[1].getDamage().getInRange();
				rolledDamage += player.getComputedStats().getAtt();
				this.addProjectile(0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
						p.getProjectileId(),
						dest.clone(-offset, -offset), Float.parseFloat(p.getAngle()), p.getSize(), p.getMagnitude(),
						p.getRange(), rolledDamage, false, p.getFlags(), p.getAmplitude(), p.getFrequency());
			}

		} else if (abilityItem.getEffect() != null) {
			player.addEffect(effect.getEffectId(), effect.getDuration());
			if (abilityItem.getEffect().getEffectId().equals(EffectType.HEAL)) {
				player.addHealth(50);
			}
		}
	}

	private List<Bullet> getBullets() {
		GameObject[] gameObject = this.getRealm()
				.getGameObjectsInBounds(this.getRealm().getTileManager().getRenderViewPort());

		List<Bullet> results = new ArrayList<>();
		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Bullet) {
				results.add((Bullet) gameObject[i]);
			}
		}
		return results;
	}

	public synchronized void processBulletHit(Player p) {
		List<Bullet> results = this.getBullets(p);
		GameObject[] gameObject = this.realm.getGameObjectsInBounds(p.getCam().getBounds());
		Player player = this.realm.getPlayer(p.getId());

		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Enemy) {
				Enemy enemy = ((Enemy) gameObject[i]);
				for (Bullet b : results) {
					this.processPlayerHit(b, player);
					this.proccessEnemyHit(b, enemy);
				}
			}
		}
		this.proccessTerrainHit(p);
	}

	private void proccessTerrainHit(Player p) {
		List<Bullet> toRemove = new ArrayList<>();
		TileMap currentMap = this.realm.getTileManager().getTm().get(1);
		Tile[] viewportTiles = null;
		if (currentMap == null)
			return;
		viewportTiles = currentMap.getBlocksInBounds(p.getCam().getBounds());
		for (Bullet b : this.getBullets(p)) {
			if (b.remove()) {
				toRemove.add(b);
				continue;
			}
			for (Tile tile : viewportTiles) {
				if (tile == null) {
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

	private synchronized void processPlayerHit(Bullet b, Player p) {
		Player player = this.realm.getPlayer(p.getId());
		if (player == null)
			return;
		if (b.getBounds().collides(0, 0, player.getBounds()) && b.isEnemy() && !b.isPlayerHit()) {
			Stats stats = player.getComputedStats();
			b.setPlayerHit(true);
			short minDmg = (short) (b.getDamage() * 0.15);
			short dmgToInflict = (short) (b.getDamage() - stats.getDef());
			if (dmgToInflict < minDmg) {
				dmgToInflict = minDmg;
			}
			player.setHealth(player.getHealth() - dmgToInflict, 0, false);
			this.expiredBullets.add(b.getId());
			this.realm.removeBullet(b);
		}
	}

	private synchronized void proccessEnemyHit(Bullet b, Enemy e) {
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

	public synchronized void addProjectile(long id, long targetPlayerId, int projectileId, int projectileGroupId,
			Vector2f src, Vector2f dest, short size, float magnitude, float range, short damage, boolean isEnemy,
			List<Short> flags) {
		Player player = this.realm.getPlayer(targetPlayerId);
		if (player == null)
			return;
		ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		if (pg.getAngleOffset() != null) {
			bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
		}
		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}
		long idToUse = id == 0l ? Realm.RANDOM.nextLong() : id;

		Bullet b = new Bullet(id, projectileId, bulletImage, src, dest, size, magnitude, range, damage, isEnemy);
		b.setFlags(flags);

		this.realm.addBullet(b);
	}

	public synchronized void addProjectile(long id, long targetPlayerId, int projectileId, int projectileGroupId,
			Vector2f src, float angle, short size, float magnitude, float range, short damage, boolean isEnemy,
			List<Short> flags, short amplitude, short frequency) {
		Player player = this.realm.getPlayer(targetPlayerId);
		if (player == null)
			return;
		ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		if (pg.getAngleOffset() != null) {
			bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
		}
		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}
		long idToUse = id == 0l ? Realm.RANDOM.nextLong() : id;
		Bullet b = new Bullet(idToUse, projectileId, bulletImage, src, angle, size, magnitude, range, damage, isEnemy);
		b.setAmplitude(amplitude);
		b.setFrequency(frequency);

		b.setFlags(flags);
		this.realm.addBullet(b);
	}

	private List<Bullet> getBullets(Player p) {

		GameObject[] gameObject = this.realm.getGameObjectsInBounds(this.realm.getTileManager().getRenderViewPort(p));

		List<Bullet> results = new ArrayList<>();
		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Bullet) {
				results.add((Bullet) gameObject[i]);
			}
		}
		return results;
	}

	private void broadcastPacket(Packet packet) {
		for (Map.Entry<String, ProcessingThread> client : this.server.getClients().entrySet()) {
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

	public static void handleHeartbeatServer(RealmManagerServer mgr, Packet packet) {
		HeartbeatPacket heartbeatPacket = (HeartbeatPacket) packet;
		RealmManagerServer.log.info("[SERVER] Recieved Heartbeat Packet For Player {}@{}", heartbeatPacket.getPlayerId(),
				heartbeatPacket.getTimestamp());
	}

	public static void handlePlayerMoveServer(RealmManagerServer mgr, Packet packet) {
		PlayerMovePacket heartbeatPacket = (PlayerMovePacket) packet;
		Player toMove = mgr.getRealm().getPlayer(heartbeatPacket.getEntityId());
		boolean doMove = heartbeatPacket.isMove();
		if (heartbeatPacket.getDirection().equals(Cardinality.NORTH)) {
			toMove.setUp(doMove);
			toMove.setDy(doMove ? toMove.getMaxSpeed() : 0.0f);
		}
		if (heartbeatPacket.getDirection().equals(Cardinality.SOUTH)) {
			toMove.setDown(doMove);
			toMove.setDy(doMove ? toMove.getMaxSpeed() : 0.0f);
		}
		if (heartbeatPacket.getDirection().equals(Cardinality.EAST)) {
			toMove.setRight(doMove);
			toMove.setDx(doMove ? toMove.getMaxSpeed() : 0.0f);
		}
		if (heartbeatPacket.getDirection().equals(Cardinality.WEST)) {
			toMove.setLeft(doMove);
			toMove.setDx(doMove ? toMove.getMaxSpeed() : 0.0f);
		}
		if (heartbeatPacket.getDirection().equals(Cardinality.NONE)) {
			toMove.setLeft(false);
			toMove.setRight(false);
			toMove.setDown(false);
			toMove.setUp(false);

			toMove.setDx(0);
			toMove.setDy(0);
		}
		RealmManagerServer.log.info("[SERVER] Recieved PlayerMove Packet For Player {}", heartbeatPacket.getEntityId());
	}

	public static void handleUseAbilityServer(RealmManagerServer mgr, Packet packet) {
		UseAbilityPacket useAbilityPacket = (UseAbilityPacket) packet;
		mgr.useAbility(useAbilityPacket.getPlayerId(), new Vector2f(useAbilityPacket.getPosX(), useAbilityPacket.getPosY()));
		RealmManagerServer.log.info("[SERVER] Recieved UseAbility Packet For Player {}", useAbilityPacket.getPlayerId());
	}

	public static void handlePlayerShootServer(RealmManagerServer mgr, Packet packet) {
		PlayerShootPacket shootPacket = (PlayerShootPacket) packet;
		Vector2f src = new Vector2f(shootPacket.getSrcX(), shootPacket.getSrcY());
		Player player = mgr.getRealm().getPlayer(shootPacket.getEntityId());

		Vector2f dest = new Vector2f(shootPacket.getDestX(), shootPacket.getDestY());
		dest.addX(player.getCam().getPos().x);
		dest.addY(player.getCam().getPos().y);
		Vector2f source = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
		ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(player.getWeaponId());
		float angle = Bullet.getAngle(source, dest);
		for (Projectile proj : group.getProjectiles()) {
			short offset = (short) (player.getSize() / (short) 2);
			short rolledDamage = player.getInventory()[0].getDamage().getInRange();
			rolledDamage += player.getComputedStats().getAtt();
			mgr.addProjectile(shootPacket.getProjectileId(), player.getId(), proj.getProjectileId(),
					player.getWeaponId(), source.clone(-offset, -offset), angle + Float.parseFloat(proj.getAngle()),
					proj.getSize(), proj.getMagnitude(), proj.getRange(), rolledDamage, false, proj.getFlags(),
					proj.getAmplitude(), proj.getFrequency());
		}
		RealmManagerServer.log.info("[SERVER] Recieved PlayerShoot Packet For Player {}, BulletId: {}", shootPacket.getEntityId(),
				shootPacket.getProjectileId());
	}

	public static void handleTextServer(RealmManagerServer mgr, Packet packet) {
		TextPacket textPacket = (TextPacket) packet;
		try {
			RealmManagerServer.log.info("[SERVER] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}\nSrcIp: {}", textPacket.getTo(),
					textPacket.getFrom(), textPacket.getMessage(), textPacket.getSrcIp());
			OutputStream toClientStream = mgr.getServer().getClients().get(textPacket.getSrcIp()).getClientSocket()
					.getOutputStream();
			DataOutputStream dosToClient = new DataOutputStream(toClientStream);

			TextPacket welcomeMessage = TextPacket.create("SYSTEM", textPacket.getFrom(),
					"Welcome to JRealm " + textPacket.getFrom());

			welcomeMessage.serializeWrite(dosToClient);

			RealmManagerServer.log.info("[SERVER] Sent welcome message to {}", textPacket.getSrcIp());
		} catch (Exception e) {
			RealmManagerServer.log.error("Failed to send welcome message. Reason: {}", e);
		}
	}

	public static void handleCommandServer(RealmManagerServer mgr, Packet packet) {
		CommandPacket commandPacket = (CommandPacket) packet;
		RealmManagerServer.log.info("[SERVER] Recieved Command Packet For Player {}. Command={}. SrcIp={}", commandPacket.getPlayerId(),
				commandPacket.getCommand(), commandPacket.getSrcIp());
		try {
			switch (commandPacket.getCommandId()) {
			case 1:
				RealmManagerServer.doLogin(mgr, CommandType.fromPacket(commandPacket), commandPacket);
				break;
			}
		} catch (Exception e) {
			RealmManagerServer.log.error("Failed to perform Server command for Player {}. Reason: {}", commandPacket.getPlayerId(),
					e.getMessage());
		}
	}

	public static void handleLoadMapServer(RealmManagerServer mgr, Packet packet) {
		LoadMapPacket loadMapPacket = (LoadMapPacket) packet;
		try {
			Player player = mgr.getRealm().getPlayer(loadMapPacket.getPlayerId());
			mgr.getRealm().loadMap(loadMapPacket.getMapKey(), player);

		} catch (Exception e) {
			RealmManagerServer.log.error("Failed to  Load Map packet from Player {}. Reason: {}", loadMapPacket.getPlayerId(),
					e.getMessage());
		}
		RealmManagerServer.log.info("[SERVER] Recieved Load Map packet from Player {}. Map={}", loadMapPacket.getPlayerId(),
				loadMapPacket.getMapKey());
	}

	private static void doLogin(RealmManagerServer mgr, LoginRequestMessage request, CommandPacket command) {
		try {
			Camera c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
			CharacterClass cls = CharacterClass.ROGUE;
			Player player = new Player(Realm.RANDOM.nextLong(), c, GameDataManager.loadClassSprites(cls),
					new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
							(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
					GlobalConstants.PLAYER_SIZE, cls);
			player.equipSlots(PlayState.getStartingEquipment(cls));
			player.setName(request.getUsername());
			player.setHeadless(false);
			OutputStream toClientStream = mgr.getServer().getClients().get(command.getSrcIp()).getClientSocket()
					.getOutputStream();
			DataOutputStream dosToClient = new DataOutputStream(toClientStream);
			LoginResponseMessage message = LoginResponseMessage.builder().playerId(player.getId()).success(true)
					.build();
			mgr.getRemoteAddresses().put(command.getSrcIp(), player.getId());

			CommandPacket commandResponse = CommandPacket.create(player, CommandType.LOGIN_RESPONSE, message);
			commandResponse.serializeWrite(dosToClient);
			long newId = mgr.getRealm().addPlayer(player);
			mgr.getServer().getClients().get(command.getSrcIp()).setHandshakeComplete(true);
		} catch (Exception e) {
			RealmManagerServer.log.error("Failed to perform Client Login. Reason: {}", e);
		}
	}
}
