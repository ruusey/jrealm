package com.jrealm.game.realm;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.jrealm.game.contants.EffectType;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.states.GameStateManager;
import com.jrealm.game.states.PlayState;
import com.jrealm.game.tiles.TileMap;
import com.jrealm.game.tiles.blocks.Tile;
import com.jrealm.game.ui.DamageText;
import com.jrealm.game.ui.TextEffect;
import com.jrealm.game.util.Cardinality;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.SocketServer;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class RealmManager implements Runnable {
	private SocketServer server;
	private Realm realm;
	private boolean shutdown = false;

	private final Map<Byte, BiConsumer<RealmManager, Packet>> packetCallbacksServer = new HashMap<>();
	private List<Vector2f> shotDestQueue;
	private long lastUpdateTime;
	private long now;
	private long lastRenderTime;
	private long lastSecondTime;
	
	private int oldFrameCount;
	private int oldTickCount;
	private int tickCount;
	
	public RealmManager(Realm realm) {
		this.registerPacketCallbacks();
		this.realm = realm;
		this.server = new SocketServer(2222);
		this.shotDestQueue = new ArrayList<>();
		WorkerThread.submitAndForkRun(this.server);
	}

	@Override
	public void run() {
		//TODO: remove and replace with an actual value: 20

		final double GAME_HERTZ = 2.0;
		final double TBU = 1000000000 / GAME_HERTZ; // Time Before Update

		final int MUBR = 3; // Must Update before render

		this.lastUpdateTime = System.nanoTime();

		//TODO: remove and replace with an actual value: 20
		final double TARGET_FPS = 2;
		final double TTBR = 1000000000 / TARGET_FPS; // Total time before render

		int frameCount = 0;
		this.lastSecondTime = (long) (this.lastUpdateTime / 1000000000);
		this.oldFrameCount = 0;

		this.tickCount = 0;
		this.oldTickCount = 0;

		while (!this.shutdown) {
			this.now = System.nanoTime();
			int updateCount = 0;
			while (((this.now - this.lastUpdateTime) > TBU) && (updateCount < MUBR)) {
				this.tick();
				this.lastUpdateTime += TBU;
				updateCount++;
				this.tickCount++;
			}

			if ((this.now - this.lastUpdateTime) > TBU) {
				this.lastUpdateTime = (long) (this.now - TBU);
			}
			
			int thisSecond = (int) (this.lastUpdateTime / 1000000000);
			if (thisSecond > this.lastSecondTime) {
				if (frameCount != this.oldFrameCount) {
					// System.out.println("NEW SECOND " + thisSecond + " " + frameCount);
					this.oldFrameCount = frameCount;
				}

				if (this.tickCount != this.oldTickCount) {
					this.oldTickCount = this.tickCount;
				}
				this.tickCount = 0;
				frameCount = 0;
				this.lastSecondTime = thisSecond;
			}

			while (((this.now - this.lastRenderTime) < TTBR) && ((this.now - this.lastUpdateTime) < TBU)) {
				try {
				} catch (Exception e) {
					System.out.println("ERROR: yielding thread");
				}
				this.now = System.nanoTime();
			}
			
		}
		log.info("RealmManager exiting run().");
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
			log.error("Failed to sleep");
		}
	}
	
	public void broadcastGameData() {
		for (Map.Entry<Long, Player> player : this.realm.getPlayers().entrySet()) {
			List<UpdatePacket> uPackets = this.realm
					.getPlayersAsPackets(player.getValue().getCam().getBounds());
			List<ObjectMovePacket> mPackets = this.realm
					.getGameObjectsAsPackets(player.getValue().getCam().getBounds());
			try {
				OutputStream toClientStream = server.getClients().get(SocketServer.LOCALHOST).getOutputStream();
				DataOutputStream dosToClient = new DataOutputStream(toClientStream);

				for (UpdatePacket packet : uPackets) {
					packet.serializeWrite(dosToClient);
				}

				for (ObjectMovePacket packet : mPackets) {
					packet.serializeWrite(dosToClient);
				}
			} catch (Exception e) {
				log.error("Failed to get OutputStream to Client");
			}
		}
	}

	public void processServerPackets() {
		while (!this.getServer().getPacketQueue().isEmpty()) {
			Packet toProcess = this.getServer().getPacketQueue().remove();
			try {
				Packet created = Packet.newPacketInstance(toProcess.getId(), toProcess.getData());
				this.packetCallbacksServer.get(created.getId()).accept(this, created);
			} catch (Exception e) {
				log.error("Failed to process server packets {}", e);
			}
		}
	}

	private void registerPacketCallbacks() {
		this.registerPacketCallback(PacketType.PLAYER_MOVE.getPacketId(), RealmManager::handlePlayerMoveServer);
		this.registerPacketCallback(PacketType.HEARTBEAT.getPacketId(), RealmManager::handleHeartbeatServer);
		this.registerPacketCallback(PacketType.TEXT.getPacketId(), RealmManager::handleTextServer);
	}

	private void registerPacketCallback(byte packetId, BiConsumer<RealmManager, Packet> callback) {
		this.packetCallbacksServer.put(packetId, callback);
	}
	
	public void update(double time) {
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);

		for (Map.Entry<Long, Player> player : this.realm.getPlayers().entrySet()) {
			Player p = this.realm.getPlayer(player.getValue().getId());
			if (p == null)
				continue;
			
			Runnable playerShootDequeue = () -> {
				for (int i = 0; i < this.shotDestQueue.size(); i++) {
					Vector2f dest = this.shotDestQueue.remove(i);
					dest.addX(PlayState.map.x);
					dest.addY(PlayState.map.y);
					Vector2f source = p.getPos().clone(p.getSize() / 2, p.getSize() / 2);
					ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(p.getWeaponId());
					float angle = Bullet.getAngle(source, dest);
					for (Projectile proj : group.getProjectiles()) {
						short offset = (short) (p.getSize() / (short) 2);
						short rolledDamage = p.getInventory()[0].getDamage().getInRange();
						rolledDamage += p.getComputedStats().getAtt();
						this.addProjectile(p.getId(), p.getWeaponId(), source.clone(-offset, -offset),
								angle + Float.parseFloat(proj.getAngle()), proj.getSize(), proj.getMagnitude(), proj.getRange(),
								rolledDamage, false, proj.getFlags(), proj.getAmplitude(), proj.getFrequency());
					}
				}

			};
			Runnable processGameObjects = () -> {
				GameObject[] gameObject = this.realm.getGameObjectsInBounds(p.getCam().getBounds());
				for (int i = 0; i < gameObject.length; i++) {
					if (gameObject[i] instanceof Enemy) {
						Enemy enemy = ((Enemy) gameObject[i]);
						//TODO: Fix
						//enemy.update(this, time);
					}

					if (gameObject[i] instanceof Bullet) {
						Bullet bullet = ((Bullet) gameObject[i]);
						if (bullet != null) {
							bullet.update();
						}
					}
				}
				this.processBulletHit(p);
			};
			// Rewrite this asap
			Runnable checkAbilityUsage = () -> {
				
				for (GameObject e : this.realm
						.getGameObjectsInBounds(this.getRealm().getTileManager().getRenderViewPort())) {
					if ((e instanceof Entity) || (e instanceof Enemy)) {
						Entity entCast = (Entity) e;
						entCast.removeExpiredEffects();
					}
				}
			};
			Runnable updatePlayerAndUi = () -> {
				p.update(time);
				this.movePlayer(p);

				//this.pui.update(time);
			};
			WorkerThread.submitAndRun(playerShootDequeue, processGameObjects, updatePlayerAndUi, checkAbilityUsage);

		}	
	}
	
	private void movePlayer(Player p) {
		if (!p.isFallen()) {
			p.move();
			if (!p.getTc().collisionTile(this.realm.getTileManager().getTm().get(1).getBlocks(),
					p.getDx(), 0)) {
				// PlayState.map.x += dx;
				p.getPos().x += p.getDx();
				p.xCol = false;
			} else {
				p.xCol = true;
			}
			if (!p.getTc().collisionTile(this.realm.getTileManager().getTm().get(1).getBlocks(), 0,
					p.getDy())) {
				// PlayState.map.y += dy;
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
	
	public synchronized void processBulletHit(Player p) {
		List<Bullet> results = this.getBullets();
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
		for (Bullet b : this.getBullets()) {
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
			this.realm.removeBullet(bullet);
		});
	}

	private synchronized void processPlayerHit(Bullet b, Player p) {
		Player player = this.realm.getPlayer(p.getId());
		if (player == null)
			return;
		if (b.getBounds().collides(0, 0, player.getBounds()) && b.isEnemy() && !b.isPlayerHit()) {
			Stats stats = player.getComputedStats();
			Vector2f sourcePos = p.getPos();
			int computedDamage = b.getDamage() - p.getComputedStats().getDef();
//			DamageText hitText = DamageText.builder().damage(computedDamage + "").effect(TextEffect.DAMAGE)
//					.sourcePos(sourcePos).build();
//			this.damageText.add(hitText);
			b.setPlayerHit(true);
			// player.getAnimation().getImage().setEffect(Sprite.effect.REDISH);
			short minDmg = (short) (b.getDamage()*0.15);
			short dmgToInflict = (short) (b.getDamage() - stats.getDef());
			if (dmgToInflict < minDmg) {
				dmgToInflict = minDmg;
			}
			player.setHealth(player.getHealth() - dmgToInflict, 0, false);
			this.realm.removeBullet(b);
		}
	}

	private synchronized void proccessEnemyHit(Bullet b, Enemy e) {
		if (this.realm.hasHitEnemy(b.getId(), e.getId()))
			return;
		if (b.getBounds().collides(0, 0, e.getBounds()) && !b.isEnemy()) {
			this.realm.hitEnemy(b.getId(), e.getId());

			e.setHealth(e.getHealth() - b.getDamage(), 0, false);
//			Vector2f sourcePos = e.getPos();
//			DamageText hitText = DamageText.builder().damage("" + b.getDamage()).effect(TextEffect.DAMAGE)
//					.sourcePos(sourcePos).build();
//			this.damageText.add(hitText);
			if (b.hasFlag((short) 10) && !b.isEnemyHit()) {
				b.setEnemyHit(true);
			} else if (b.remove()) {
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

				this.realm.clearHitMap();
				this.realm.spawnRandomEnemy();
				this.realm.removeEnemy(e);
				this.realm.addLootContainer(new LootContainer(
						GameDataManager.SPRITE_SHEETS.get("entity/rotmg-items-1.png").getSprite(6, 7, 8, 8),
						e.getPos()));
			}
		}
	}

	
	public synchronized void addProjectile(long entityId, int projectileGroupId, Vector2f src, Vector2f dest, short size,
			float magnitude, float range, short damage, boolean isEnemy, List<Short> flags) {
		Player player = this.realm.getPlayer(entityId);
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
		Bullet b = new Bullet(Realm.RANDOM.nextLong(), bulletImage, src, dest, size, magnitude, range, damage, isEnemy);
		b.setFlags(flags);

		this.realm.addBullet(b);
	}

	public synchronized void addProjectile(long entityId, int projectileGroupId, Vector2f src, float angle, short size,
			float magnitude, float range, short damage, boolean isEnemy, List<Short> flags, short amplitude,
			short frequency) {
		Player player = this.realm.getPlayer(entityId);
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
		Bullet b = new Bullet(Realm.RANDOM.nextLong(), bulletImage, src, angle, size, magnitude, range, damage,
				isEnemy);
		b.setAmplitude(amplitude);
		b.setFrequency(frequency);

		b.setFlags(flags);
		this.realm.addBullet(b);
	}
	
	private List<Bullet> getBullets() {

		GameObject[] gameObject = this.realm.getGameObjectsInBounds(this.realm.getTileManager().getRenderViewPort());

		List<Bullet> results = new ArrayList<>();
		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Bullet) {
				results.add((Bullet) gameObject[i]);
			}
		}
		return results;
	}
	
	public static void handleHeartbeatServer(RealmManager mgr, Packet packet) {
		HeartbeatPacket heartbeatPacket = (HeartbeatPacket) packet;
		log.info("[SERVER] Recieved Heartbeat Packet For Player {}@{}", heartbeatPacket.getPlayerId(), heartbeatPacket.getTimestamp());
	}
	
	public static void handlePlayerMoveServer(RealmManager mgr, Packet packet) {
		PlayerMovePacket heartbeatPacket = (PlayerMovePacket) packet;
		Player toMove = mgr.getRealm().getPlayer(heartbeatPacket.getEntityId());
		boolean doMove = heartbeatPacket.isMove();
		if (heartbeatPacket.getDirection() == Cardinality.NORTH) {
			toMove.setUp(doMove);
		} else if (heartbeatPacket.getDirection() == Cardinality.SOUTH) {
			toMove.setDown(doMove);
		} else if (heartbeatPacket.getDirection() == Cardinality.EAST) {
			toMove.setRight(doMove);
		} else if (heartbeatPacket.getDirection() == Cardinality.WEST) {
			toMove.setLeft(doMove);
		}
		log.info("[SERVER] Recieved PlayerMove Packet For Player {}", heartbeatPacket.getEntityId());
	}
	
	public static void handleTextServer(RealmManager mgr, Packet packet) {
		TextPacket textPacket = (TextPacket) packet;
		log.info("[SERVER] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
				textPacket.getFrom(), textPacket.getMessage());
		try {
			OutputStream toClientStream = mgr.getServer().getClients().get(SocketServer.LOCALHOST).getOutputStream();
			DataOutputStream dosToClient = new DataOutputStream(toClientStream);
			TextPacket welcomeMessage = TextPacket.create("SYSTEM", textPacket.getFrom(), "Welcome to JRealm "+textPacket.getFrom()+"!");
			welcomeMessage.serializeWrite(dosToClient);
		}catch(Exception e) {
			log.error("Failed to send welcome message. Reason: {}", e);
		}
	
	}
}
