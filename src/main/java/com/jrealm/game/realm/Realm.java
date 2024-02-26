package com.jrealm.game.realm;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import com.jrealm.account.dto.ChestDto;
import com.jrealm.account.dto.GameItemRefDto;
import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.LootTier;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Monster;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.Chest;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.graphics.Animation;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.model.TerrainGenerationParameters;
import com.jrealm.game.tile.TileManager;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.ServerGameLogic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@Slf4j
public class Realm {
	// Shared Secure Random instance for generating Ids and other random data
	public static final transient SecureRandom RANDOM = new SecureRandom();
	private long realmId;
	private int mapId;
	private int depth;
	private Map<Long, Player> players;
	private Map<Long, Bullet> bullets;
	private Map<Long, List<Long>> bulletHits;
	private Map<Long, Enemy> enemies;
	private Map<Long, LootContainer> loot;
	private Map<Long, Portal> portals;

	private List<Long> expiredEnemies;
	private List<Long> expiredBullets;
	private List<Long> expiredPlayers;

	private TileManager tileManager;
	private Semaphore playerLock = new Semaphore(1);

	private boolean isServer;

	public Realm(boolean isServer, int mapId) {
		this.depth = 0;
		this.realmId = Realm.RANDOM.nextLong();
		this.players = new ConcurrentHashMap<>();
		this.isServer = isServer;
		this.expiredEnemies = new ArrayList<>();
		this.expiredPlayers = new ArrayList<>();
		this.expiredBullets = new ArrayList<>();
		this.loadMap(mapId);

		if(this.isServer) {
			WorkerThread.submit(this.getStatsThread());
		}
	}

	public int getDepth() {
		return this.depth;
	}

	public List<Long> getExpiredPlayers(){
		return this.expiredPlayers;
	}

	public void setupChests(final Player player) {
		Vector2f chestLoc = new Vector2f((0 + (GamePanel.width / 2)) - 450, (0 + (GamePanel.height / 2)) - 200);
		try {
			final PlayerAccountDto account = ServerGameLogic.DATA_SERVICE
					.executeGet("/data/account/" + player.getAccountUuid(), null, PlayerAccountDto.class);
			for(ChestDto chest : account.getPlayerVault()) {
				List<GameItem> itemsInChest = chest.getItems().stream().map(GameItem::fromGameItemRef).collect(Collectors.toList());
				Chest toSpawn = new Chest(chestLoc.clone(-64 * chest.getOrdinal(), 0),
						itemsInChest.toArray(new GameItem[8]));
				this.addLootContainer(toSpawn);
			}
		}catch(Exception e) {
			Realm.log.error("Failed to get player account for chests. Reason: {}", e);
		}
	}

	public List<ChestDto> serializeChests() {
		List<ChestDto> result = new ArrayList<ChestDto>();
		int ordinal = 0;
		for (LootContainer container : this.loot.values()) {
			if (container instanceof Chest) {
				ChestDto chest = ChestDto.builder().chestId(container.getUid()).chestUuid(container.getUid())
						.ordinal(ordinal++).build();
				List<GameItemRefDto> itemRefs = new ArrayList<>();
				for (int i = 0; i < container.getItems().length; i++) {
					GameItem toCopy = container.getItems()[i];
					if (toCopy != null) {
						itemRefs.add(GameItemRefDto.builder().itemId(toCopy.getItemId()).itemUuid(toCopy.getUid())
								.slotIdx(i).build());

					}
				}
				chest.setItems(itemRefs);
				result.add(chest);
			}
		}
		return result;
	}

	public void loadMap(int mapId) {
		this.mapId = mapId;
		this.bullets = new ConcurrentHashMap<>();
		this.enemies = new ConcurrentHashMap<>();
		this.loot = new ConcurrentHashMap<>();
		this.portals = new ConcurrentHashMap<>();

		this.bulletHits = new ConcurrentHashMap<>();
		if (this.isServer) {
			this.tileManager = new TileManager(mapId);
		} else {
			this.tileManager = new TileManager(GameDataManager.MAPS.get(mapId));
		}
	}

	public long addPlayer(Player player) {
		this.acquirePlayerLock();
		this.players.put(player.getId(), player);
		this.releasePlayerLock();
		return player.getId();
	}

	public long addPlayerIfNotExists(Player player) {
		if(!this.players.containsKey(player.getId())) {
			this.acquirePlayerLock();
			SpriteSheet sheet = GameDataManager.loadClassSprites(CharacterClass.valueOf(player.getClassId()));
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

	public boolean removePlayer(Player player) {
		this.acquirePlayerLock();
		Player p = this.players.remove(player.getId());
		this.releasePlayerLock();
		return p != null;
	}

	public boolean hasHitEnemy(long bulletId, long enemyId) {
		return (this.bulletHits.get(bulletId) != null) && this.bulletHits.get(bulletId).contains(enemyId);
	}

	public void clearHitMap() {
		this.bulletHits.clear();
	}

	public void hitEnemy(long bulletId, long enemyId) {
		if (this.bulletHits.get(bulletId) == null) {
			List<Long> hits = new ArrayList<>();
			hits.add(enemyId);
			this.bulletHits.put(bulletId, hits);
		} else {
			List<Long> curr = this.bulletHits.get(bulletId);
			curr.add(enemyId);
			this.bulletHits.put(bulletId, curr);
		}
	}

	public void asd() {
		System.out.println(this.mapId + this.realmId);
	}


	public boolean removePlayer(long playerId) {
		this.acquirePlayerLock();
		Player p = this.players.remove(playerId);
		this.releasePlayerLock();
		return p != null;
	}

	public Player getPlayer(long playerId) {
		this.acquirePlayerLock();
		Player p = this.players.get(playerId);
		this.releasePlayerLock();
		return p;
	}

	public Bullet getBullet(long bulletId) {
		return this.bullets.get(bulletId);
	}

	public long addBullet(Bullet b) {
		this.bullets.put(b.getId(), b);
		return b.getId();
	}

	public long addBulletIfNotExists(Bullet b) {
		Bullet existing = this.bullets.get(b.getId());
		if(existing==null) {
			ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(b.getProjectileId());
			SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
			Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
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

	public long addPortal(Portal portal) {
		this.portals.put(portal.getId(), portal);
		return portal.getId();
	}

	public boolean removePortal(long portalId) {
		Portal removed = this.portals.remove(portalId);
		return removed != null;
	}

	public boolean removePortal(Portal portal) {
		Portal removed = this.portals.remove(portal.getId());
		return removed != null;
	}

	public long addPortalIfNotExists(Portal portal) {
		Portal existing = this.portals.get(portal.getId());
		if (existing == null) {
			PortalModel portalModel = GameDataManager.PORTALS.get((int) portal.getPortalId());
			Sprite portalSprite = GameDataManager.getSubSprite(portalModel, 8);
			portal.setSprite(portalSprite);
			this.portals.put(portal.getId(), portal);
		}
		return portal.getId();
	}

	public long addEnemy(Enemy enemy) {
		this.enemies.put(enemy.getId(), enemy);
		return enemy.getId();
	}

	public long addEnemyIfNotExists(Enemy enemy) {
		Enemy existing = this.enemies.get(enemy.getId());
		if(existing==null) {
			EnemyModel model = GameDataManager.ENEMIES.get(enemy.getEnemyId());
			SpriteSheet enemySheet = GameDataManager.SPRITE_SHEETS.get(model.getSpriteKey());
			SpriteSheet sheet = new SpriteSheet(
					enemySheet.getSprite(model.getCol(), model.getRow(), model.getSpriteSize(), model.getSpriteSize()),
					enemy.getName(), model.getSpriteSize(), model.getSpriteSize(), 0);

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

	public Enemy getEnemy(long enemyId) {
		return this.enemies.get(enemyId);
	}

	public boolean removeEnemy(Enemy enemy) {
		Enemy e = this.enemies.remove(enemy.getId());
		return e != null;
	}

	public long addLootContainer(LootContainer lc) {
		long randomId = Realm.RANDOM.nextLong();
		lc.setLootContainerId(randomId);
		this.loot.put(randomId, lc);
		return randomId;
	}

	public long addLootContainerIfNotExists(LootContainer lc) {
		if(!this.loot.containsKey(lc.getLootContainerId())) {
			Sprite lootSprite = LootTier.getLootSprite(lc.getTier().tierId);
			lc.setSprite(lootSprite.clone());
			for(GameItem item : lc.getItems()) {
				if(item!=null) {
					GameDataManager.loadSpriteModel(item);
				}
			}
			this.loot.put(lc.getLootContainerId(), lc);
		}
		return lc.getLootContainerId();
	}

	public boolean removeLootContainer(LootContainer lc) {
		LootContainer lootContainer = this.loot.remove(lc.getLootContainerId());
		return lootContainer != null;
	}

	public List<Chest> getChests() {
		List<Chest> objs = new ArrayList<>();
		if (this.loot == null)
			return objs;
		for (LootContainer lc : this.loot.values()) {
			if (lc instanceof Chest) {
				objs.add((Chest) lc);
			}
		}
		return objs;
	}

	public Rectangle[] getCollisionBoxesInBounds(Rectangle cam) {
		List<Rectangle> colBoxes = new ArrayList<>();
		GameObject[] go = this.getGameObjectsInBounds(cam);
		for (GameObject g : go) {
			colBoxes.add(g.getBounds());
		}
		return colBoxes.toArray(new Rectangle[0]);
	}

	public Player[] getPlayersInBounds(Rectangle cam) {
		List<Player> objs = new ArrayList<>();

		for (Player p : this.players.values()) {
			if (p.getBounds().intersect(cam)) {
				objs.add(p);
			}
		}

		return objs.toArray(new Player[0]);
	}

	public GameObject[] getGameObjectsInBounds(Rectangle cam) {

		List<GameObject> objs = new ArrayList<>();
		for (Player p : this.players.values()) {
			if (p.getBounds().intersect(cam)) {
				objs.add(p);
			}
		}

		for (Bullet b : this.bullets.values()) {
			if (b.getBounds().intersect(cam)) {
				objs.add(b);
			}
		}

		for (Enemy e : this.enemies.values()) {
			if (e.getBounds().intersect(cam)) {
				objs.add(e);
			}
		}

		return objs.toArray(new GameObject[0]);
	}

	public GameObject[] getGameObjectss() {

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

		return objs.toArray(new GameObject[0]);
	}

	public UpdatePacket getPlayerAsPacket(long playerId) {
		final Player p = this.players.get(playerId);
		UpdatePacket pack = null;
		try {
			pack = UpdatePacket.from(p);
		} catch (Exception e) {
			Realm.log.error("Failed to create update packet from Player. Reason: {}", e);
		}

		return pack;
	}

	public List<UpdatePacket> getPlayersAsPackets(Rectangle cam) {
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

	public LoadPacket getLoadPacket(Rectangle cam) {
		LoadPacket load = null;
		try {
			//final Player[] playersToLoad = this.getPlayers().values().toArray(new Player[0]);
			final List<Player> playersToLoadList = new ArrayList<>();
			for(Player p : this.players.values()) {
				final boolean inViewport = cam.inside((int)p.getPos().x, (int)p.getPos().y);
				if(inViewport) {
					playersToLoadList.add(p);
				}

			}
			final List<LootContainer> containersToLoad = new ArrayList<>();
			for(LootContainer c : this.loot.values()) {
				final boolean inViewport = cam.inside((int)c.getPos().x, (int)c.getPos().y);
				if(inViewport) {
					containersToLoad.add(c);
				}

			}

			final List<Bullet> bulletsToLoad = new ArrayList<>();
			for(Bullet b : this.bullets.values()) {
				final boolean inViewport = cam.inside((int)b.getPos().x, (int)b.getPos().y);
				if(inViewport) {
					bulletsToLoad.add(b);
				}
			}

			final List<Enemy> enemiesToLoad = new ArrayList<>();
			for(Enemy e : this.enemies.values()) {
				final boolean inViewport = cam.inside((int)e.getPos().x, (int)e.getPos().y);
				if(inViewport) {
					enemiesToLoad.add(e);
				}
			}

			final List<Portal> portalsToLoad = new ArrayList<>();
			for (Portal p : this.portals.values()) {
				final boolean inViewport = cam.inside((int) p.getPos().x, (int) p.getPos().y);
				if (inViewport) {
					portalsToLoad.add(p);
				}
			}

			load = LoadPacket.from(playersToLoadList.toArray(new Player[0]), containersToLoad.toArray(new LootContainer[0]),
					bulletsToLoad.toArray(new Bullet[0]), enemiesToLoad.toArray(new Enemy[0]),
					portalsToLoad.toArray(new Portal[0]));
		} catch (Exception e) {
			Realm.log.error("Failed to get load Packet. Reason: {}");
		}
		return load;
	}

	public ObjectMovePacket getGameObjectsAsPackets(Rectangle cam) throws Exception{
		GameObject[] gameObjects = this.getAllGameObjects();
		List<GameObject> validObjects = new ArrayList<>();
		for (GameObject obj : gameObjects) {
			try {
				if(((obj.getDx()>0) || (obj.getDy()>0)) || ((obj.getDx()<0) || (obj.getDy()<0)) || obj.getTeleported()) {
					final boolean inViewport = cam.inside((int) obj.getPos().x, (int) obj.getPos().y);
					if (inViewport) {
						validObjects.add(obj);
					}
					if(obj.getTeleported()) {
						obj.setTeleported(false);
					}
				}
			} catch (Exception e) {
				Realm.log.error("Failed to create ObjectMove Packet. Reason: {}", e.getMessage());
			}
		}
		if (validObjects.size() > 0)
			return ObjectMovePacket.from(validObjects.toArray(new GameObject[0]));
		return null;
	}

	public LootContainer[] getLootInBounds(Rectangle cam) {
		List<LootContainer> objs = new ArrayList<>();
		for (LootContainer lc : this.loot.values()) {
			if (cam.inside((int) lc.getPos().x, (int) lc.getPos().y)) {
				objs.add(lc);
			}
		}
		return objs.toArray(new LootContainer[0]);
	}

	public void spawnRandomEnemies(int mapId) {
		if(this.enemies==null) {
			this.enemies = new ConcurrentHashMap<>();
		}
		SpriteSheet enemySheet = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-bosses.png");
		List<EnemyModel> enemyToSpawn = new ArrayList<>();

		TerrainGenerationParameters params = GameDataManager.TERRAINS.get(GameDataManager.MAPS.get(mapId).getTerrainId());
		for(int enemyId: params.getEnemyGroups().get(0).getEnemyIds()) {
			enemyToSpawn.add(GameDataManager.ENEMIES.get(enemyId));
		}

		Random r = new Random(System.nanoTime());
		for (int i = 0; i < this.tileManager.getMapLayers().get(0).getHeight(); i++) {
			for (int j = 0; j < this.tileManager.getMapLayers().get(0).getWidth(); j++) {
				int doSpawn = r.nextInt(200);
				if ((doSpawn > 198) && (i > 0) && (j > 0)) {
					Vector2f spawnPos = new Vector2f(j * this.tileManager.getMapLayers().get(0).getTileSize(),
							i * this.tileManager.getMapLayers().get(0).getTileSize());
					EnemyModel toSpawn = enemyToSpawn.get(r.nextInt(enemyToSpawn.size()));

					SpriteSheet enemySprite = new SpriteSheet(
							enemySheet.getSprite(toSpawn.getCol(), toSpawn.getRow(), toSpawn.getSpriteSize(),
									toSpawn.getSpriteSize()),
							toSpawn.getName(), toSpawn.getSpriteSize(), toSpawn.getSpriteSize(), 0);
					Enemy enemy = new Monster(Realm.RANDOM.nextLong(), toSpawn.getEnemyId(), enemySprite,
							new Vector2f(j * this.tileManager.getMapLayers().get(0).getTileSize(),
									i * this.tileManager.getMapLayers().get(0).getTileSize()),
							toSpawn.getSize(), toSpawn.getAttackId());
					int healthMult = (this.getDepth() == 0 ? 1 : this.getDepth() + 1);
					enemy.setHealth(enemy.getHealth() * healthMult);
					enemy.setPos(spawnPos);
					this.addEnemy(enemy);
				}
			}
		}
	}

	public void spawnRandomEnemy() {
		SpriteSheet enemySheet = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-bosses.png");
		Random r = new Random(System.nanoTime());
		Vector2f spawnPos = new Vector2f(
				this.tileManager.getMapLayers().get(0).getTileSize()
				* r.nextInt(this.tileManager.getMapLayers().get(0).getWidth()),
				this.tileManager.getMapLayers().get(0).getTileSize()
				* r.nextInt(this.tileManager.getMapLayers().get(0).getHeight()));

		List<EnemyModel> enemyToSpawn = new ArrayList<>();
		GameDataManager.ENEMIES.values().forEach(enemy->{
			enemyToSpawn.add(enemy);
		});
		EnemyModel toSpawn = enemyToSpawn.get(r.nextInt(enemyToSpawn.size()));
		SpriteSheet enemySprite = new SpriteSheet(
				enemySheet.getSprite(toSpawn.getCol(), toSpawn.getRow(), toSpawn.getSpriteSize(),
						toSpawn.getSpriteSize()),
				toSpawn.getName(), toSpawn.getSpriteSize(), toSpawn.getSpriteSize(), 0);
		Enemy enemy = new Monster(Realm.RANDOM.nextLong(), toSpawn.getEnemyId(), enemySprite,
				spawnPos, toSpawn.getSize(), toSpawn.getAttackId());
		int healthMult = (this.getDepth() == 0 ? 1 : this.getDepth() + 1);
		enemy.setHealth(enemy.getHealth() * healthMult);
		enemy.setPos(spawnPos);
		this.addEnemy(enemy);
	}

	private Thread getStatsThread() {
		Runnable r = () -> {
			while (true) {
				double heapSize = Runtime.getRuntime().totalMemory() / 1024.0 / 1024.0;
				Realm.log.info("--- Realm: {} | MapId: {} ---", this.getRealmId(), this.getMapId());
				Realm.log.info("Enemies: {}", this.enemies.size());
				Realm.log.info("Players: {}", this.players.size());
				Realm.log.info("Loot: {}", this.loot.size());
				Realm.log.info("Bullets: {}", this.bullets.size());
				Realm.log.info("BulletHits: {}", this.bulletHits.size());
				Realm.log.info("Portals: {}", this.portals.size());
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
}
