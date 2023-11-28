package com.jrealm.game.realm;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.enemy.Monster;
import com.jrealm.game.entity.item.Chest;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.material.Material;
import com.jrealm.game.entity.material.MaterialManager;
import com.jrealm.game.graphics.Animation;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.tiles.TileManager;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.GameObjectKey;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@Slf4j
public class Realm {
	public static final transient SecureRandom RANDOM = new SecureRandom();

	private Map<Long, Player> players;
	private Map<Long, Bullet> bullets;
	private Map<Long, List<Long>> bulletHits;
	private Map<Long, Enemy> enemies;
	private Map<Long, LootContainer> loot;
	private Map<Long, Material> materials;
	private Map<Integer, MaterialManager> materialManagers;

	private TileManager tileManager;
	private Camera realmCamera = null;
	private Semaphore playerLock = new Semaphore(1);

	private boolean isServer;
	public Realm(final Camera cam, final boolean isServer) {
		this.players = new ConcurrentHashMap<>();
		this.isServer = isServer;
		this.realmCamera = cam;

		this.loadMap("tile/vault.xml", null);
		if(this.isServer) {
			this.setupChests();
		}
		WorkerThread.submit(this.getStatsThread());
	}

	private void setupChests() {
		final Vector2f chestLoc = new Vector2f((0 + (GamePanel.width / 2)) - 450, (0 + (GamePanel.height / 2)) - 200);
		if (this.getChests().size() == 0) {
			this.await(100);
			this.addLootContainer(new Chest(chestLoc));
			this.await(100);
			this.addLootContainer(new Chest(chestLoc.clone(-128, 0)));
			this.await(100);
			this.addLootContainer(new Chest(chestLoc.clone(-256, 0)));
		}
	}

	public void loadRandomTerrain() {
		final List<Chest> curr = this.getChests();

		this.bullets = new ConcurrentHashMap<>();
		this.enemies = new ConcurrentHashMap<>();
		this.loot = new ConcurrentHashMap<>();
		if (curr.size() > 0) {
			curr.forEach(chest -> {
				this.addLootContainer(chest);
			});
		}
		this.bulletHits = new ConcurrentHashMap<>();
		this.materials = new ConcurrentHashMap<>();
		this.materialManagers = new ConcurrentHashMap<>();

		final SpriteSheet tileset = GameDataManager.SPRITE_SHEETS.get("tile/overworldOP.png");
		final SpriteSheet treeset = GameDataManager.SPRITE_SHEETS.get("material/trees.png");
		final SpriteSheet rockset = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-items-1.png");

		final MaterialManager treeMgr = new MaterialManager(64, 150);
		treeMgr.setMaterial(MaterialManager.TYPE.TREE, treeset.getSprite(1, 0), 64);
		// treeMgr.setMaterial(MaterialManager.TYPE.TREE, treeset.getSprite(3, 0), 64);

		final MaterialManager rockMgr = new MaterialManager(32, 150);
		rockMgr.setMaterial(MaterialManager.TYPE.TREE, rockset.getSprite(10, 5), 32);

		this.tileManager = new TileManager(tileset, 150, this.realmCamera, treeMgr, rockMgr);

		for (final MaterialManager mm : this.tileManager.getMaterialManagers()) {
			for (final GameObjectKey go : mm.list) {
				if (go.go instanceof Material) {
					this.addMaterial((Material) go.go);
				}
			}
		}
		this.spawnRandomEnemies();
	}

	public void loadMap(final String path, final Player player) {
		final List<Chest> curr = this.getChests();

		this.bullets = new ConcurrentHashMap<>();
		this.enemies = new ConcurrentHashMap<>();
		this.loot = new ConcurrentHashMap<>();
		if (curr.size() > 0) {
			curr.forEach(chest -> {
				this.addLootContainer(chest);
			});
		}
		this.bulletHits = new ConcurrentHashMap<>();
		this.materials = new ConcurrentHashMap<>();
		this.materialManagers = new ConcurrentHashMap<>();
		this.tileManager = new TileManager(path, this.realmCamera);
		if (!path.toLowerCase().contains("vault") && this.isServer) {
			this.spawnRandomEnemies();
		}

		if (player != null) {
			player.resetPosition();
		}
	}

	public long addMaterial(final Material m) {
		this.materials.put(m.getId(), m);
		return m.getId();
	}

	public long addPlayer(final Player player) {
		this.acquirePlayerLock();
		this.players.put(player.getId(), player);
		this.releasePlayerLock();
		return player.getId();
	}

	public long addPlayerIfNotExists(final Player player) {
		if(!this.players.containsKey(player.getId())) {
			this.acquirePlayerLock();
			final SpriteSheet sheet = GameDataManager.loadClassSprites(CharacterClass.valueOf(player.getClassId()));
			player.setSprite(sheet);
			player.setAni(new Animation());
			player.getAnimation().setFrames(sheet.getSpriteArray(0));
			player.getAnimation().setNumFrames(2, player.UP);
			player.getAnimation().setNumFrames(2, player.DOWN);
			player.getAnimation().setNumFrames(2, player.RIGHT);
			player.getAnimation().setNumFrames(2, player.LEFT);
			player.getAnimation().setNumFrames(2, player.ATTACK + player.RIGHT);
			player.getAnimation().setNumFrames(2, player.ATTACK + player.LEFT);
			player.getAnimation().setNumFrames(2, player.ATTACK + player.UP);
			player.getAnimation().setNumFrames(2, player.ATTACK + player.DOWN);
			player.setAnimation(player.RIGHT, sheet.getSpriteArray(player.RIGHT), 10);

			this.players.put(player.getId(), player);
			this.releasePlayerLock();
		}
		return player.getId();
	}

	public boolean removePlayer(final Player player) {
		this.acquirePlayerLock();
		final Player p = this.players.remove(player.getId());
		this.releasePlayerLock();
		return p != null;
	}

	public boolean hasHitEnemy(final long bulletId, final long enemyId) {
		return (this.bulletHits.get(bulletId) != null) && this.bulletHits.get(bulletId).contains(enemyId);
	}

	public void clearHitMap() {
		this.bulletHits.clear();
	}

	public void hitEnemy(final long bulletId, final long enemyId) {
		if (this.bulletHits.get(bulletId) == null) {
			final List<Long> hits = new ArrayList<>();
			hits.add(enemyId);
			this.bulletHits.put(bulletId, hits);
		} else {
			final List<Long> curr = this.bulletHits.get(bulletId);
			curr.add(enemyId);
			this.bulletHits.put(bulletId, curr);
		}
	}

	public boolean removePlayer(long playerId) {
		this.acquirePlayerLock();
		final Player p = this.players.remove(playerId);
		this.releasePlayerLock();
		return p != null;
	}

	public Player getPlayer(final long playerId) {
		this.acquirePlayerLock();
		final Player p = this.players.get(playerId);
		this.releasePlayerLock();
		return p;
	}

	public Bullet getBullet(final long bulletId) {
		return this.bullets.get(bulletId);
	}

	public long addBullet(final Bullet b) {
		this.bullets.put(b.getId(), b);
		return b.getId();
	}

	public long addBulletIfNotExists(final Bullet b) {
		final Bullet existing = this.bullets.get(b.getId());
		if(existing==null) {
			final ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(b.getProjectileId());
			final SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
			final Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
			if (pg.getAngleOffset() != null) {
				bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
			}
			b.setImage(bulletImage);
			this.bullets.put(b.getId(), b);
		}
		return b.getId();
	}

	public boolean removeBullet(Bullet b) {
		Bullet bullet = this.bullets.remove(b.getId());
		this.bulletHits.remove(b.getId());
		return bullet != null;
	}

	public boolean removeBullet(Collection<Long> b) {
		for (Long l : b) {
			this.bullets.remove(l);
			this.bulletHits.remove(l);
		}
		return true;
	}

	public long addEnemy(Enemy enemy) {
		this.enemies.put(enemy.getId(), enemy);
		return enemy.getId();
	}

	public long addEnemyIfNotExists(Enemy enemy) {
		final Enemy existing = this.enemies.get(enemy.getId());
		if(existing==null) {
			final EnemyModel model = GameDataManager.ENEMIES.get(enemy.getEnemyId());
			final SpriteSheet enemySheet = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-bosses.png");
			final SpriteSheet sheet = new SpriteSheet(enemySheet.getSprite(model.getCol(), model.getRow(), 16, 16),
					enemy.getName(), 16, 16, 0);

			enemy.setSprite(sheet);
			enemy.setAni(new Animation());
			enemy.getAnimation().setFrames(sheet.getSpriteArray(0));
			enemy.getAni().setNumFrames(3, 0);
			enemy.getAni().setNumFrames(5, 1);

			enemy.setCurrentAnimation(enemy.RIGHT);
			this.enemies.put(enemy.getId(), enemy);

		}
		return enemy.getId();
	}

	public Enemy getEnemy(final long enemyId) {
		return this.enemies.get(enemyId);
	}

	public boolean removeEnemy(final Enemy enemy) {
		Enemy e = this.enemies.remove(enemy.getId());
		return e != null;
	}

	public long addLootContainer(final LootContainer lc) {
		final long randomId = Realm.RANDOM.nextLong();
		lc.setLootContainerId(randomId);
		this.loot.put(randomId, lc);
		return randomId;
	}

	public long addLootContainerIfNotExists(final LootContainer lc) {
		if(!this.loot.containsKey(lc.getLootContainerId())) {
			Sprite lootSprite = null;
			if(lc instanceof Chest) {
				lootSprite = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-projectiles.png").getSprite(2, 0, 8, 8);
			}else {
				lootSprite = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-items-1.png").getSprite(6, 7, 8, 8);
			}
			lc.setSprite(lootSprite.clone());
			for(final GameItem item : lc.getItems()) {
				GameDataManager.loadSpriteModel(item);
			}
			this.loot.put(lc.getLootContainerId(), lc);
		}
		return lc.getLootContainerId();
	}

	public boolean removeLootContainer(final LootContainer lc) {
		final LootContainer lootContainer = this.loot.remove(lc.getLootContainerId());
		return lootContainer != null;
	}

	public List<Chest> getChests() {
		final List<Chest> objs = new ArrayList<>();
		if (this.loot == null)
			return objs;
		for (final LootContainer lc : this.loot.values()) {
			if (lc instanceof Chest) {
				objs.add((Chest) lc);
			}
		}
		return objs;
	}

	public AABB[] getCollisionBoxesInBounds(final AABB cam) {
		final List<AABB> colBoxes = new ArrayList<>();
		final GameObject[] go = this.getGameObjectsInBounds(cam);
		for (final GameObject g : go) {
			colBoxes.add(g.getBounds());
		}
		return colBoxes.toArray(new AABB[0]);
	}

	public GameObject[] getGameObjectsInBounds(final AABB cam) {

		final List<GameObject> objs = new ArrayList<>();
		for (final Player p : this.players.values()) {
			if (p.getBounds().intersect(cam)) {
				objs.add(p);
			}
		}

		for (final Bullet b : this.bullets.values()) {
			if (b.getBounds().intersect(cam)) {
				objs.add(b);
			}
		}

		for (final Enemy e : this.enemies.values()) {
			if (e.getBounds().intersect(cam)) {
				objs.add(e);
			}
		}

		for (final Material e : this.materials.values()) {
			if (e.getBounds().intersect(cam)) {
				objs.add(e);
			}
		}

		return objs.toArray(new GameObject[0]);
	}

	public GameObject[] getGameObjectss() {

		final List<GameObject> objs = new ArrayList<>();
		for (final Player p : this.players.values()) {
			objs.add(p);
		}

		for (final Bullet b : this.bullets.values()) {
			objs.add(b);
		}

		for (final Enemy e : this.enemies.values()) {
			objs.add(e);
		}

		for (Material e : this.materials.values()) {
			objs.add(e);
		}

		return objs.toArray(new GameObject[0]);
	}

	public GameObject[] getAllGameObjects() {

		List<GameObject> objs = new ArrayList<>();
		for (Player p : this.players.values()) {
			objs.add(p);
		}

		for (Bullet b : this.bullets.values()) {
			objs.add(b);
		}

		for (Enemy e : this.enemies.values()) {
			objs.add(e);
		}

		for (Material e : this.materials.values()) {
			objs.add(e);
		}
		return objs.toArray(new GameObject[0]);
	}

	public List<UpdatePacket> getPlayersAsPackets(AABB cam) {
		List<UpdatePacket> playerUpdates = new ArrayList<>();
		for (Player p : this.players.values()) {
			// if (p.getBounds().intersect(cam)) {
			try {
				UpdatePacket pack = UpdatePacket.from(p);
				playerUpdates.add(pack);
			} catch (Exception e) {
				Realm.log.error("Failed to create update packet from Player. Reason: {}", e);
			}
			// }
		}
		return playerUpdates;
	}

	public LoadPacket getLoadPacket(AABB cam) {
		LoadPacket load = null;
		try {
			final Player[] playersToLoad = this.getPlayers().values().toArray(new Player[0]);
			//			final List<Player> playersToLoad = new ArrayList<>();
			//			for(Player p : this.players.values()) {
			//				final boolean inViewport = cam.inside((int)p.getPos().x, (int)p.getPos().y);
			//				if(inViewport) {
			//					playersToLoad.add(p);
			//				}
			//
			//			}
			final LootContainer[] containersToLoad = this.getLoot().values().toArray(new LootContainer[0]);
			final List<Bullet> bulletsToLoad = new ArrayList<>();

			for(Bullet b : this.bullets.values()) {
				final boolean inViewport = cam.inside((int)b.getPos().x, (int)b.getPos().y);
				if(inViewport) {
					bulletsToLoad.add(b);
				}
			}
			// Maybe dont send all 500 enemies in the same packet??
			// final Enemy[] enemiesToLoad = this.getEnemies().values().toArray(new Enemy[0]);
			final List<Enemy> enemiesToLoad = new ArrayList<>();
			for(Enemy e : this.getEnemies().values()) {
				final boolean inViewport = cam.inside((int)e.getPos().x, (int)e.getPos().y);
				if(inViewport) {
					enemiesToLoad.add(e);
				}
			}

			load = LoadPacket.from(playersToLoad, containersToLoad,
					bulletsToLoad.toArray(new Bullet[0]), enemiesToLoad.toArray(new Enemy[0]));
		} catch (Exception e) {
			Realm.log.error("Failed to get load Packet. Reason: {}");
		}
		return load;
	}

	public ObjectMovePacket getGameObjectsAsPackets(AABB cam) throws Exception{
		GameObject[] gameObjects = this.getAllGameObjects();
		List<GameObject> validObjects = new ArrayList<>();
		for (GameObject obj : gameObjects) {
			try {
				if(((obj.getDx()>0) || (obj.getDy()>0)) || ((obj.getDx()<0) || (obj.getDy()<0))) {
					validObjects.add(obj);
				}
			} catch (Exception e) {
				Realm.log.error("Failed to create ObjectMove Packet. Reason: {}", e.getMessage());
			}
		}
		if (validObjects.size() > 0)
			return ObjectMovePacket.from(validObjects.toArray(new GameObject[0]));
		return null;
	}

	public LootContainer[] getLootInBounds(AABB cam) {
		List<LootContainer> objs = new ArrayList<>();
		for (LootContainer lc : this.loot.values()) {
			if (cam.inside((int) lc.getPos().x, (int) lc.getPos().y)) {
				objs.add(lc);
			}
		}
		return objs.toArray(new LootContainer[0]);
	}

	public void spawnRandomEnemies() {
		if(this.enemies==null) {
			this.enemies = new ConcurrentHashMap<>();
		}
		Vector2f v = new Vector2f((0 + (GamePanel.width / 2)) - 32, (0 + (GamePanel.height / 2)) - 32);
		SpriteSheet enemySheet = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-bosses.png");

		Random r = new Random(System.nanoTime());
		for (int i = 0; i < this.tileManager.getHeight(); i++) {
			for (int j = 0; j < this.tileManager.getWidth(); j++) {
				int doSpawn = r.nextInt(200);
				if ((doSpawn > 195) && (i > 0) && (j > 0)) {
					Vector2f spawnPos = new Vector2f(j * 64, i * 64);
					AABB bounds = new AABB(spawnPos, 64, 64);
					if (bounds.distance(v) < 500) {
						continue;
					}
					List<EnemyModel> enemyToSpawn = new ArrayList<>();
					GameDataManager.ENEMIES.values().forEach(enemy->{
						enemyToSpawn.add(enemy);
					});
					EnemyModel toSpawn = enemyToSpawn.get(r.nextInt(enemyToSpawn.size()));
					Enemy enemy = new Monster(Realm.RANDOM.nextLong(), toSpawn.getEnemyId(),
							new SpriteSheet(enemySheet.getSprite(toSpawn.getCol(), toSpawn.getRow(), 16, 16),
									toSpawn.getName(), 16, 16, 0),
							new Vector2f(j * 64, i * 64), toSpawn.getSize(), toSpawn.getAttackId());
					enemy.setPos(spawnPos);
					this.addEnemy(enemy);
				}
			}
		}
	}

	public void spawnRandomEnemy() {
		SpriteSheet enemySheet = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-bosses.png");
		Random r = new Random(System.nanoTime());
		Vector2f spawnPos = new Vector2f(GlobalConstants.BASE_SIZE * r.nextInt(this.tileManager.getWidth()),
				GlobalConstants.BASE_SIZE * r.nextInt(this.tileManager.getHeight()));

		List<EnemyModel> enemyToSpawn = new ArrayList<>();
		GameDataManager.ENEMIES.values().forEach(enemy->{
			enemyToSpawn.add(enemy);
		});
		EnemyModel toSpawn = enemyToSpawn.get(r.nextInt(enemyToSpawn.size()));
		Enemy enemy = new Monster(Realm.RANDOM.nextLong(), toSpawn.getEnemyId(),
				new SpriteSheet(enemySheet.getSprite(toSpawn.getCol(), toSpawn.getRow(), 16, 16),
						toSpawn.getName(), 16, 16, 0),
				spawnPos, toSpawn.getSize(), toSpawn.getAttackId());
		enemy.setPos(spawnPos);
		this.addEnemy(enemy);
	}

	private Thread getStatsThread() {
		Runnable r = () -> {
			while (true) {
				double heapSize = Runtime.getRuntime().totalMemory() / 1024.0 / 1024.0;
				String header = this.isServer ? "SERVER STATS" : "CLIENT STATS";
				Realm.log.info(header);
				Realm.log.info("Enemies: {}", this.enemies.size());
				Realm.log.info("Players: {}", this.players.size());
				Realm.log.info("Loot: {}", this.loot.size());
				Realm.log.info("Bullets: {}", this.bullets.size());
				Realm.log.info("BulletHits: {}", this.bulletHits.size());
				Realm.log.info("Heap Mem: {}", heapSize);

				try {
					Thread.sleep(10000);
				} catch (Exception e) {

				}
			}
		};
		return new Thread(r);
	}

	private void acquirePlayerLock() {
		try {
			this.playerLock.acquire();
		} catch (Exception e) {
			Realm.log.error(e.getMessage());
		}
	}

	private void releasePlayerLock() {
		try {
			this.playerLock.release();
		} catch (Exception e) {
			Realm.log.error(e.getMessage());
		}
	}

	private void await(long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {

		}
	}
}
