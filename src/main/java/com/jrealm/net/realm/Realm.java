package com.jrealm.net.realm;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import com.jrealm.account.dto.ChestDto;
import com.jrealm.account.dto.GameItemRefDto;
import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.game.JRealmGame;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.LootTier;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Monster;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.Chest;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.DungeonGraphNode;
import com.jrealm.game.model.EnemyGroup;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.OverworldZone;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.model.TerrainGenerationParameters;
import com.jrealm.game.tile.TileManager;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.ServerGameLogic;
import com.jrealm.util.WorkerThread;

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
    private String nodeId;
    private Map<Long, Player> players;
    private Map<Long, Bullet> bullets;
    private Map<Long, List<Long>> bulletHits;
    private Map<Long, Enemy> enemies;
    private Map<Long, LootContainer> loot;
    private Map<Long, Portal> portals;

    private List<Long> expiredEnemies;
    private List<Long> expiredBullets;
    private List<Long> expiredPlayers;
    private Map<Long, Long> playerLastShotTime;
    private TileManager tileManager;
    private Semaphore playerLock = new Semaphore(1);

    // Spatial hash grid for O(1) neighbor lookups (cell size = viewport radius)
    private transient SpatialHashGrid spatialGrid;

    private boolean isServer;
    private boolean shutdown = false;

    public Realm(boolean isServer, int mapId) {
        this.depth = 0;
        this.realmId = Realm.RANDOM.nextLong();
        this.players = new ConcurrentHashMap<>();
        this.isServer = isServer;
        this.expiredEnemies = new ArrayList<>();
        this.expiredPlayers = new ArrayList<>();
        this.expiredBullets = new ArrayList<>();
        this.playerLastShotTime = new HashMap<>();
        this.spatialGrid = new SpatialHashGrid(10 * GlobalConstants.BASE_TILE_SIZE);
        this.loadMap(mapId);
        if (this.isServer) {
            WorkerThread.submitAndForkRun(this.getStatsThread());
        }
    }

    public Realm(boolean isServer, int mapId, int depth) {
        this(isServer, mapId);
        this.depth = depth;
    }

    public Realm(boolean isServer, int mapId, int depth, String nodeId) {
        this(isServer, mapId);
        this.depth = depth;
        this.nodeId = nodeId;
    }

    public int getDepth() {
        return this.depth;
    }

    public List<Long> getExpiredPlayers() {
        return this.expiredPlayers;
    }
    
    public Set<Player> getPlayersExcept(long playerId){
    	return this.players.values().stream().filter(p->p.getId()!=playerId).collect(Collectors.toSet());
    }

    public void setupChests(final Player player) {
        final Vector2f chestLoc = new Vector2f((0 + (JRealmGame.width / 2)) - 450, (0 + (JRealmGame.height / 2)) - 200);
        try {
            final PlayerAccountDto account = ServerGameLogic.DATA_SERVICE
                    .executeGet("/data/account/" + player.getAccountUuid(), null, PlayerAccountDto.class);
            for (final ChestDto chest : account.getPlayerVault()) {
                final List<GameItem> itemsInChest = chest.getItems().stream().map(GameItem::fromGameItemRef)
                        .collect(Collectors.toList());
                final Chest toSpawn = new Chest(chestLoc.clone((-64 * chest.getOrdinal())+128, 0),
                        itemsInChest.toArray(new GameItem[8]));
                this.addLootContainer(toSpawn);
            }
        } catch (Exception e) {
            Realm.log.error("Failed to get player account for chests. Reason: {}", e);
        }
    }

    public List<ChestDto> serializeChests() {
        final List<ChestDto> result = new ArrayList<ChestDto>();
        int ordinal = 0;
        for (final LootContainer container : this.loot.values()) {
            if (container instanceof Chest) {
                final ChestDto chest = ChestDto.builder().chestId(container.getUid()).chestUuid(container.getUid())
                        .ordinal(ordinal++).build();
                final List<GameItemRefDto> itemRefs = new ArrayList<>();
                for (int i = 0; i < container.getItems().length; i++) {
                    final GameItem toCopy = container.getItems()[i];
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
    
    public void clearData() {
        this.bullets = new ConcurrentHashMap<>();
        this.enemies = new ConcurrentHashMap<>();
        this.loot = new ConcurrentHashMap<>();
        this.portals = new ConcurrentHashMap<>();
        this.players = new ConcurrentHashMap<>();
        this.bulletHits = new ConcurrentHashMap<>();
        this.expiredEnemies = new ArrayList<>();
        this.expiredEnemies = new ArrayList<>();
        this.expiredPlayers = new ArrayList<>();
        this.playerLastShotTime = new ConcurrentHashMap<>();
        if (this.spatialGrid != null) {
            this.spatialGrid.clear();
        }
    }

    public long addPlayer(Player player) {
        this.acquirePlayerLock();
        this.players.put(player.getId(), player);
        if (this.spatialGrid != null) {
            this.spatialGrid.insert(player.getId(), player.getPos().x, player.getPos().y);
        }
        this.releasePlayerLock();
        return player.getId();
    }
    
    public long addPlayerIfNotExists(Player player) {
        if (!this.players.containsKey(player.getId())) {
            this.acquirePlayerLock();
            final SpriteSheet sheet = GameSpriteManager.loadClassSprites(CharacterClass.valueOf(player.getClassId()));
            player.setSpriteSheet(sheet);
            this.players.put(player.getId(), player);
            if (this.spatialGrid != null) {
                this.spatialGrid.insert(player.getId(), player.getPos().x, player.getPos().y);
            }
            this.releasePlayerLock();
        }
        return player.getId();
    }

    public boolean removePlayer(Player player) {
        this.acquirePlayerLock();
        this.playerLastShotTime.remove(player.getId());
        final Player p = this.players.remove(player.getId());
        if (this.spatialGrid != null) {
            this.spatialGrid.remove(player.getId());
        }
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
        if (this.spatialGrid != null) {
            this.spatialGrid.remove(playerId);
        }
        this.releasePlayerLock();
        return p != null;
    }

    public Player getPlayer(long playerId) {
        this.acquirePlayerLock();
        final Player p = this.players.get(playerId);
        this.releasePlayerLock();
        return p;
    }
    
    public Bullet getBullet(long bulletId) {
        return this.bullets.get(bulletId);
    }

    public long addBullet(Bullet b) {
        this.bullets.put(b.getId(), b);
        if (this.spatialGrid != null) {
            this.spatialGrid.insert(b.getId(), b.getPos().x, b.getPos().y);
        }
        return b.getId();
    }

    public long addBulletIfNotExists(Bullet b) {
        final Bullet existing = this.bullets.get(b.getId());
        if (existing == null) {
            final ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(b.getProjectileId());
            final SpriteSheet bulletSprite = GameSpriteManager.getSpriteSheet(pg);
            final Sprite bulletImage = bulletSprite.getSprites().get(0);

            if (pg.getAngleOffset() != null) {
                bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
            }
            b.setSpriteSheet(bulletSprite);
            this.bullets.put(b.getId(), b);
            if (this.spatialGrid != null) {
                this.spatialGrid.insert(b.getId(), b.getPos().x, b.getPos().y);
            }
        }
        return b.getId();
    }

    public boolean removeBullet(Bullet b) {
        final Bullet bullet = this.bullets.remove(b.getId());
        this.bulletHits.remove(b.getId());
        if (this.spatialGrid != null) {
            this.spatialGrid.remove(b.getId());
        }
        return bullet != null;
    }

    public boolean removeBullet(Collection<Long> b) {
        for (Long l : b) {
            this.bullets.remove(l);
            this.bulletHits.remove(l);
            if (this.spatialGrid != null) {
                this.spatialGrid.remove(l);
            }
        }
        return true;
    }

    public long addPortal(Portal portal) {
        this.portals.put(portal.getId(), portal);
        if (this.spatialGrid != null) {
            this.spatialGrid.insert(portal.getId(), portal.getPos().x, portal.getPos().y);
        }
        return portal.getId();
    }

    public boolean removePortal(long portalId) {
        final Portal removed = this.portals.remove(portalId);
        if (this.spatialGrid != null) {
            this.spatialGrid.remove(portalId);
        }
        return removed != null;
    }

    public boolean removePortal(Portal portal) {
        final Portal removed = this.portals.remove(portal.getId());
        if (this.spatialGrid != null) {
            this.spatialGrid.remove(portal.getId());
        }
        return removed != null;
    }

    public long addPortalIfNotExists(Portal portal) {
        final Portal existing = this.portals.get(portal.getId());
        if (existing == null) {
            final PortalModel portalModel = GameDataManager.PORTALS.get((int) portal.getPortalId());
            final Sprite portalSprite = GameSpriteManager.loadSprite(portalModel);
            portal.setSprite(portalSprite);
            this.portals.put(portal.getId(), portal);
            if (this.spatialGrid != null) {
                this.spatialGrid.insert(portal.getId(), portal.getPos().x, portal.getPos().y);
            }
        }
        return portal.getId();
    }

    public long addEnemy(Enemy enemy) {
        this.enemies.put(enemy.getId(), enemy);
        if (this.spatialGrid != null) {
            this.spatialGrid.insert(enemy.getId(), enemy.getPos().x, enemy.getPos().y);
        }
        return enemy.getId();
    }

    public long addEnemyIfNotExists(Enemy enemy) {
        final Enemy existing = this.enemies.get(enemy.getId());
        if (existing == null) {
            final EnemyModel model = GameDataManager.ENEMIES.get(enemy.getEnemyId());
            if (model != null) {
                enemy.setSpriteSheet(SpriteSheet.fromSpriteModel(model));
                enemy.setModel(model);
                if (enemy.getStats() == null) {
                    enemy.setStats(model.getStats().clone());
                }
                enemy.setChaseRange((int) model.getChaseRange());
                enemy.setAttackRange((int) model.getAttackRange());
            }
            this.enemies.put(enemy.getId(), enemy);
            if (this.spatialGrid != null) {
                this.spatialGrid.insert(enemy.getId(), enemy.getPos().x, enemy.getPos().y);
            }
        }
        return enemy.getId();
    }

    public Enemy getEnemy(long enemyId) {
        return this.enemies.get(enemyId);
    }

    public boolean removeEnemy(Enemy enemy) {
        final Enemy e = this.enemies.remove(enemy.getId());
        if (this.spatialGrid != null) {
            this.spatialGrid.remove(enemy.getId());
        }
        return e != null;
    }

    public long addLootContainer(LootContainer lc) {
        long randomId = Realm.RANDOM.nextLong();
        lc.setLootContainerId(randomId);
        this.loot.put(randomId, lc);
        if (this.spatialGrid != null) {
            this.spatialGrid.insert(randomId, lc.getPos().x, lc.getPos().y);
        }
        return randomId;
    }

    public long addLootContainerIfNotExists(LootContainer lc) {
        if (!this.loot.containsKey(lc.getLootContainerId())) {
            Sprite lootSprite = LootTier.getLootSprite(lc.getTier().tierId);
            lc.setSprite(lootSprite.clone());
            for (GameItem item : lc.getItems()) {
                if (item != null) {
                    GameDataManager.loadSpriteModel(item);
                }
            }
            this.loot.put(lc.getLootContainerId(), lc);
            if (this.spatialGrid != null) {
                this.spatialGrid.insert(lc.getLootContainerId(), lc.getPos().x, lc.getPos().y);
            }
        }
        return lc.getLootContainerId();
    }

    public boolean removeLootContainer(LootContainer lc) {
        final LootContainer lootContainer = this.loot.remove(lc.getLootContainerId());
        if (this.spatialGrid != null) {
            this.spatialGrid.remove(lc.getLootContainerId());
        }
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

    /**
     * Updates the spatial grid positions for all moving entities.
     * Call once per tick from the server update loop.
     */
    public void updateSpatialGrid() {
        if (this.spatialGrid == null) return;
        for (final Player p : this.players.values()) {
            this.spatialGrid.update(p.getId(), p.getPos().x, p.getPos().y);
        }
        for (final Enemy e : this.enemies.values()) {
            this.spatialGrid.update(e.getId(), e.getPos().x, e.getPos().y);
        }
        for (final Bullet b : this.bullets.values()) {
            this.spatialGrid.update(b.getId(), b.getPos().x, b.getPos().y);
        }
    }

    /**
     * Returns players near a point using the spatial hash grid.
     * Falls back to brute-force if grid is unavailable.
     */
    public Player[] getPlayersInRadiusFast(Vector2f center, float radius) {
        if (this.spatialGrid == null) {
            return getPlayersInRadius(center, radius);
        }
        final float radiusSq = radius * radius;
        final List<Long> candidates = this.spatialGrid.queryRadius(center.x, center.y, radius);
        final List<Player> objs = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            final Player p = this.players.get(candidates.get(i));
            if (p != null) {
                float dx = p.getPos().x - center.x;
                float dy = p.getPos().y - center.y;
                if (dx * dx + dy * dy <= radiusSq) {
                    objs.add(p);
                }
            }
        }
        return objs.toArray(new Player[0]);
    }

    /**
     * Grid-accelerated circular LoadPacket construction.
     */
    public LoadPacket getLoadPacketCircularFast(Vector2f center, float radius) {
        if (this.spatialGrid == null) {
            return getLoadPacketCircular(center, radius);
        }
        final float radiusSq = radius * radius;
        LoadPacket load = null;
        try {
            final List<Long> candidates = this.spatialGrid.queryRadius(center.x, center.y, radius);
            final List<Player> playersToLoadList = new ArrayList<>();
            final List<LootContainer> containersToLoad = new ArrayList<>();
            final List<Bullet> bulletsToLoad = new ArrayList<>();
            final List<Enemy> enemiesToLoad = new ArrayList<>();
            final List<Portal> portalsToLoad = new ArrayList<>();

            for (int i = 0; i < candidates.size(); i++) {
                final long id = candidates.get(i);
                // Check each entity map for this ID
                Player p = this.players.get(id);
                if (p != null) {
                    float dx = p.getPos().x - center.x;
                    float dy = p.getPos().y - center.y;
                    if (dx * dx + dy * dy <= radiusSq) playersToLoadList.add(p);
                    continue;
                }
                Enemy e = this.enemies.get(id);
                if (e != null) {
                    float dx = e.getPos().x - center.x;
                    float dy = e.getPos().y - center.y;
                    if (dx * dx + dy * dy <= radiusSq) enemiesToLoad.add(e);
                    continue;
                }
                Bullet b = this.bullets.get(id);
                if (b != null) {
                    float dx = b.getPos().x - center.x;
                    float dy = b.getPos().y - center.y;
                    if (dx * dx + dy * dy <= radiusSq) bulletsToLoad.add(b);
                    continue;
                }
                Portal portal = this.portals.get(id);
                if (portal != null) {
                    float dx = portal.getPos().x - center.x;
                    float dy = portal.getPos().y - center.y;
                    if (dx * dx + dy * dy <= radiusSq) portalsToLoad.add(portal);
                    continue;
                }
                LootContainer lc = this.loot.get(id);
                if (lc != null) {
                    float dx = lc.getPos().x - center.x;
                    float dy = lc.getPos().y - center.y;
                    if (dx * dx + dy * dy <= radiusSq) containersToLoad.add(lc);
                }
            }
            load = LoadPacket.from(playersToLoadList.toArray(new Player[0]),
                    containersToLoad.toArray(new LootContainer[0]), bulletsToLoad.toArray(new Bullet[0]),
                    enemiesToLoad.toArray(new Enemy[0]), portalsToLoad.toArray(new Portal[0]));
        } catch (Exception e) {
            Realm.log.error("Failed to get fast circular load Packet. Reason: {}", e.getMessage());
        }
        return load;
    }

    /**
     * Grid-accelerated ObjectMovePacket construction (players + enemies only).
     */
    public ObjectMovePacket getGameObjectsAsPacketsCircularFast(Vector2f center, float radius) throws Exception {
        if (this.spatialGrid == null) {
            return getGameObjectsAsPacketsCircular(center, radius);
        }
        final float radiusSq = radius * radius;
        final List<Long> candidates = this.spatialGrid.queryRadius(center.x, center.y, radius);
        final List<GameObject> validObjects = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            final long id = candidates.get(i);
            Player p = this.players.get(id);
            if (p != null) {
                float dx = p.getPos().x - center.x;
                float dy = p.getPos().y - center.y;
                if (dx * dx + dy * dy <= radiusSq) validObjects.add(p);
                if (p.getTeleported()) p.setTeleported(false);
                continue;
            }
            Enemy e = this.enemies.get(id);
            if (e != null) {
                float dx = e.getPos().x - center.x;
                float dy = e.getPos().y - center.y;
                if (dx * dx + dy * dy <= radiusSq) validObjects.add(e);
                if (e.getTeleported()) e.setTeleported(false);
                continue;
            }
            // Skip bullets in ObjectMovePacket - clients predict bullet positions
            // locally using initial velocity from LoadPacket (deterministic trajectory).
            // This dramatically reduces bandwidth under heavy projectile load.
        }
        if (validObjects.size() > 0)
            return ObjectMovePacket.from(validObjects.toArray(new GameObject[0]));
        return null;
    }

    /**
     * Returns the spatial grid cell key for a world position.
     * Players in the same cell see approximately the same entities.
     */
    public long getSpatialCellKey(float x, float y) {
        if (this.spatialGrid == null) return 0;
        return this.spatialGrid.getCellKey(x, y);
    }

    public Rectangle[] getCollisionBoxesInBounds(Rectangle cam) {
        final List<Rectangle> colBoxes = new ArrayList<>();
        final GameObject[] go = this.getGameObjectsInBounds(cam);
        for (final GameObject g : go) {
            colBoxes.add(g.getBounds());
        }
        return colBoxes.toArray(new Rectangle[0]);
    }

    public Player[] getPlayersInBounds(Rectangle cam) {
        final List<Player> objs = new ArrayList<>();
        for (final Player p : this.players.values()) {
            if (p.getBounds().intersect(cam)) {
                objs.add(p);
            }
        }

        return objs.toArray(new Player[0]);
    }

    public Player[] getPlayersInRadius(Vector2f center, float radius) {
        final float radiusSq = radius * radius;
        final List<Player> objs = new ArrayList<>();
        for (final Player p : this.players.values()) {
            float dx = p.getPos().x - center.x;
            float dy = p.getPos().y - center.y;
            if (dx * dx + dy * dy <= radiusSq) {
                objs.add(p);
            }
        }
        return objs.toArray(new Player[0]);
    }

    public GameObject[] getGameObjectsInBounds(Rectangle cam) {
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

        return objs.toArray(new GameObject[0]);
    }

    public GameObject[] getGameObjectsInRadius(Vector2f center, float radius) {
        final float radiusSq = radius * radius;
        final List<GameObject> objs = new ArrayList<>();
        for (final Player p : this.players.values()) {
            float dx = p.getPos().x - center.x;
            float dy = p.getPos().y - center.y;
            if (dx * dx + dy * dy <= radiusSq) objs.add(p);
        }
        for (final Bullet b : this.bullets.values()) {
            float dx = b.getPos().x - center.x;
            float dy = b.getPos().y - center.y;
            if (dx * dx + dy * dy <= radiusSq) objs.add(b);
        }
        for (final Enemy e : this.enemies.values()) {
            float dx = e.getPos().x - center.x;
            float dy = e.getPos().y - center.y;
            if (dx * dx + dy * dy <= radiusSq) objs.add(e);
        }
        return objs.toArray(new GameObject[0]);
    }

    public ObjectMovePacket getGameObjectsAsPacketsCircular(Vector2f center, float radius) throws Exception {
        final float radiusSq = radius * radius;
        final GameObject[] gameObjects = this.getAllGameObjects();
        final List<GameObject> validObjects = new ArrayList<>();
        for (GameObject obj : gameObjects) {
            try {
                float dx = obj.getPos().x - center.x;
                float dy = obj.getPos().y - center.y;
                if (dx * dx + dy * dy <= radiusSq) {
                    validObjects.add(obj);
                }
                if (obj.getTeleported()) {
                    obj.setTeleported(false);
                }
            } catch (Exception e) {
                Realm.log.error("Failed to create ObjectMove Packet. Reason: {}", e.getMessage());
            }
        }
        if (validObjects.size() > 0)
            return ObjectMovePacket.from(validObjects.toArray(new GameObject[0]));
        return null;
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

        return objs.toArray(new GameObject[0]);
    }

    public GameObject[] getAllGameObjects() {
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

        return objs.toArray(new GameObject[0]);
    }

    /**
     * Returns only players and enemies for ObjectMovePacket.
     * Bullets are excluded because they follow deterministic trajectories
     * and the client simulates them locally from the initial LoadPacket data.
     */
    public GameObject[] getMovableGameObjects() {
        final List<GameObject> objs = new ArrayList<>();
        for (final Player p : this.players.values()) {
            objs.add(p);
        }

        for (final Enemy e : this.enemies.values()) {
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
    
    public UpdatePacket getEnemyAsPacket(long enemyId) {
        final Enemy enemy = this.enemies.get(enemyId);
        UpdatePacket pack = null;
        try {
            pack = UpdatePacket.from(enemy);
        } catch (Exception e) {
            Realm.log.error("Failed to create update packet from Enemy. Reason: {}", e);
        }
        return pack;
    }


    public List<UpdatePacket> getPlayersAsPackets(Rectangle cam) {
        final List<UpdatePacket> playerUpdates = new ArrayList<>();
        for (final Player p : this.players.values()) {
            try {
                final UpdatePacket pack = UpdatePacket.from(p);
                playerUpdates.add(pack);
            } catch (Exception e) {
                Realm.log.error("Failed to create update packet from Player. Reason: {}", e);
            }
        }
        return playerUpdates;
    }

    public LoadPacket getLoadPacket(Rectangle cam) {
        LoadPacket load = null;
        try {
            final List<Player> playersToLoadList = new ArrayList<>();
            for (Player p : this.players.values()) {
                final boolean inViewport = cam.inside((int) p.getPos().x, (int) p.getPos().y);
                if (inViewport) {
                    playersToLoadList.add(p);
                }

            }
            final List<LootContainer> containersToLoad = new ArrayList<>();
            for (LootContainer c : this.loot.values()) {
                final boolean inViewport = cam.inside((int) c.getPos().x, (int) c.getPos().y);
                if (inViewport) {
                    containersToLoad.add(c);
                }
            }

            final List<Bullet> bulletsToLoad = new ArrayList<>();
            for (Bullet b : this.bullets.values()) {
                final boolean inViewport = cam.inside((int) b.getPos().x, (int) b.getPos().y);
                if (inViewport) {
                    bulletsToLoad.add(b);
                }
            }

            final List<Enemy> enemiesToLoad = new ArrayList<>();
            for (Enemy e : this.enemies.values()) {
                final boolean inViewport = cam.inside((int) e.getPos().x, (int) e.getPos().y);
                if (inViewport) {
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

            load = LoadPacket.from(playersToLoadList.toArray(new Player[0]),
                    containersToLoad.toArray(new LootContainer[0]), bulletsToLoad.toArray(new Bullet[0]),
                    enemiesToLoad.toArray(new Enemy[0]), portalsToLoad.toArray(new Portal[0]));
        } catch (Exception e) {
            Realm.log.error("Failed to get load Packet. Reason: {}");
        }
        return load;
    }

    public LoadPacket getLoadPacketCircular(Vector2f center, float radius) {
        final float radiusSq = radius * radius;
        LoadPacket load = null;
        try {
            final List<Player> playersToLoadList = new ArrayList<>();
            for (Player p : this.players.values()) {
                float dx = p.getPos().x - center.x;
                float dy = p.getPos().y - center.y;
                if (dx * dx + dy * dy <= radiusSq) playersToLoadList.add(p);
            }
            final List<LootContainer> containersToLoad = new ArrayList<>();
            for (LootContainer c : this.loot.values()) {
                float dx = c.getPos().x - center.x;
                float dy = c.getPos().y - center.y;
                if (dx * dx + dy * dy <= radiusSq) containersToLoad.add(c);
            }
            final List<Bullet> bulletsToLoad = new ArrayList<>();
            for (Bullet b : this.bullets.values()) {
                float dx = b.getPos().x - center.x;
                float dy = b.getPos().y - center.y;
                if (dx * dx + dy * dy <= radiusSq) bulletsToLoad.add(b);
            }
            final List<Enemy> enemiesToLoad = new ArrayList<>();
            for (Enemy e : this.enemies.values()) {
                float dx = e.getPos().x - center.x;
                float dy = e.getPos().y - center.y;
                if (dx * dx + dy * dy <= radiusSq) enemiesToLoad.add(e);
            }
            final List<Portal> portalsToLoad = new ArrayList<>();
            for (Portal p : this.portals.values()) {
                float dx = p.getPos().x - center.x;
                float dy = p.getPos().y - center.y;
                if (dx * dx + dy * dy <= radiusSq) portalsToLoad.add(p);
            }
            load = LoadPacket.from(playersToLoadList.toArray(new Player[0]),
                    containersToLoad.toArray(new LootContainer[0]), bulletsToLoad.toArray(new Bullet[0]),
                    enemiesToLoad.toArray(new Enemy[0]), portalsToLoad.toArray(new Portal[0]));
        } catch (Exception e) {
            Realm.log.error("Failed to get circular load Packet. Reason: {}", e.getMessage());
        }
        return load;
    }

    public ObjectMovePacket getGameObjectsAsPackets(Rectangle cam) throws Exception {
        final GameObject[] gameObjects = this.getAllGameObjects();
        final List<GameObject> validObjects = new ArrayList<>();
        for (GameObject obj : gameObjects) {
            try {

                final boolean inViewport = cam.inside((int) obj.getPos().x, (int) obj.getPos().y);
                if (inViewport) {
                    validObjects.add(obj);
                }
                if (obj.getTeleported()) {
                    obj.setTeleported(false);
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
        final List<LootContainer> objs = new ArrayList<>();
        for (final LootContainer lc : this.loot.values()) {
            if (cam.inside((int) lc.getPos().x, (int) lc.getPos().y)) {
                objs.add(lc);
            }
        }
        return objs.toArray(new LootContainer[0]);
    }

    public void spawnRandomEnemies(int mapId) {
        if (this.enemies == null) {
            this.enemies = new ConcurrentHashMap<>();
        }

        TerrainGenerationParameters params = GameDataManager.TERRAINS
                .get(GameDataManager.MAPS.get(mapId).getTerrainId());
        if (params == null) {
            log.warn("No Terrain generation params found for MapId {}, using default values", mapId);
            params = GameDataManager.TERRAINS.get(GameDataManager.MAPS.get(4).getTerrainId());
        }

        final boolean hasZones = params.getZones() != null && !params.getZones().isEmpty();

        // Pre-build enemy lists per zone (or single global list for legacy)
        final Map<Integer, List<EnemyModel>> enemiesByGroup = new HashMap<>();
        for (EnemyGroup group : params.getEnemyGroups()) {
            List<EnemyModel> models = new ArrayList<>();
            for (int enemyId : group.getEnemyIds()) {
                EnemyModel m = GameDataManager.ENEMIES.get(enemyId);
                if (m != null) models.add(m);
            }
            enemiesByGroup.put(group.getOrdinal(), models);
        }

        // Legacy fallback: all enemies from group 0
        final List<EnemyModel> defaultEnemies = enemiesByGroup.getOrDefault(0,
                new ArrayList<>(enemiesByGroup.values().iterator().next()));

        final int tileSize = this.tileManager.getMapLayers().get(0).getTileSize();
        final int mapHeight = this.tileManager.getMapLayers().get(0).getHeight();
        final int mapWidth = this.tileManager.getMapLayers().get(0).getWidth();

        for (int i = 1; i < mapHeight; i++) {
            for (int j = 1; j < mapWidth; j++) {
                // ~3x enemy density: spawn chance ~3/mapWidth per tile
                final int doSpawn = Realm.RANDOM.nextInt(mapWidth);
                if (doSpawn <= mapWidth - 4) continue;

                final Vector2f spawnPos = new Vector2f(j * tileSize, i * tileSize);
                if (this.tileManager.isCollisionTile(spawnPos) || this.tileManager.isVoidTile(spawnPos, 0, 0)) {
                    continue;
                }

                // Select enemy list based on zone
                List<EnemyModel> spawnList = defaultEnemies;
                int healthMult = this.getDifficultyMultiplier();

                if (hasZones) {
                    OverworldZone zone = this.tileManager.getZoneForPosition(spawnPos.x, spawnPos.y);
                    if (zone != null) {
                        spawnList = enemiesByGroup.getOrDefault(zone.getEnemyGroupOrdinal(), defaultEnemies);
                        healthMult = Math.max(1, zone.getDifficulty());
                    }
                }

                if (spawnList.isEmpty()) continue;
                final EnemyModel toSpawn = spawnList.get(Realm.RANDOM.nextInt(spawnList.size()));

                final Enemy enemy = new Monster(Realm.RANDOM.nextLong(), toSpawn.getEnemyId(),
                        spawnPos.clone(), toSpawn.getSize(), toSpawn.getAttackId());
                enemy.setSpriteSheet(GameSpriteManager.getSpriteSheet(toSpawn));
                enemy.setHealth(enemy.getHealth() * healthMult);
                enemy.setHealthMultiplier(healthMult);
                enemy.getStats().setHp((short) (enemy.getStats().getHp() * healthMult));
                enemy.setPos(spawnPos);
                this.addEnemy(enemy);
            }
        }
    }

    public void spawnRandomEnemy() {
        final Vector2f spawnPos = this.tileManager.getSafePosition();

        final List<EnemyModel> enemyToSpawn = new ArrayList<>();
        GameDataManager.ENEMIES.values().forEach(enemy -> {
            enemyToSpawn.add(enemy);
        });
        final EnemyModel toSpawn = enemyToSpawn.get(Realm.RANDOM.nextInt(enemyToSpawn.size()));

        final Enemy enemy = new Monster(Realm.RANDOM.nextLong(), toSpawn.getEnemyId(), spawnPos, toSpawn.getSize(),
                toSpawn.getAttackId());
        enemy.setSpriteSheet(GameSpriteManager.getSpriteSheet(toSpawn));

        final int healthMult = this.getDifficultyMultiplier();
        enemy.setHealth(enemy.getHealth() * healthMult);
        enemy.setPos(spawnPos);
        this.addEnemy(enemy);
    }

    public int getDifficultyMultiplier() {
        if (this.nodeId != null && GameDataManager.DUNGEON_GRAPH != null) {
            DungeonGraphNode node = GameDataManager.DUNGEON_GRAPH.get(this.nodeId);
            if (node != null) return Math.max(1, node.getDifficulty());
        }
        // Fallback to legacy depth-based scaling
        return ((this.getDepth() == 0 || this.getDepth() == 999) ? 1 : this.getDepth() + 1);
    }

    private Runnable getStatsThread() {
        final Runnable statsThread = () -> {
            while (!this.shutdown) {
                final double heapSize = Runtime.getRuntime().totalMemory() / 1024.0 / 1024.0;
                final String nodeName = (this.nodeId != null) ? this.nodeId : "legacy";
                Realm.log.info("--- Realm: {} | Node: {} | MapId: {} | Difficulty: {} ---", this.getRealmId(), nodeName, this.getMapId(), this.getDifficultyMultiplier());
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
            log.info("Realm {} destroyed", this.getRealmId());
        };
        return statsThread;
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
