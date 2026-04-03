package com.jrealm.net.realm;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
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
import com.jrealm.game.model.MapModel;
import com.jrealm.game.model.OverworldZone;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.model.TerrainGenerationParameters;
import com.jrealm.game.tile.TileManager;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.ServerGameLogic;
import com.jrealm.util.GameObjectUtils;
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
    // Compact short ID allocator for bandwidth-efficient movement packets
    private ShortIdAllocator shortIdAllocator = new ShortIdAllocator();
    private final java.util.concurrent.locks.ReentrantLock playerLock = new java.util.concurrent.locks.ReentrantLock();

    // Spatial hash grid for O(1) neighbor lookups (cell size = viewport radius)
    private transient SpatialHashGrid spatialGrid;

    // Overseer AI for ecosystem management (enemy population, events, taunts)
    private transient RealmOverseer overseer;

    // Poison damage-over-time tracking. Each entry ticks independently (poisons stack).
    private final List<PoisonDotState> activePoisonDots = new java.util.ArrayList<>();
    private static final long POISON_TICK_INTERVAL_MS = 200;

    // Pending poison throws — tracked per tick instead of blocking a thread pool thread.
    private final List<PoisonThrowState> pendingPoisonThrows = new java.util.ArrayList<>();

    // Active decoys — lightweight tick-driven entities for Trickster prism ability.
    private final List<DecoyState> activeDecoys = new java.util.ArrayList<>();

    static class PoisonThrowState {
        final long landTime;
        final long sourcePlayerId;
        final float landX;
        final float landY;
        final float radius;
        final int totalDamage;
        final long poisonDuration;

        PoisonThrowState(long delayMs, long sourcePlayerId, float landX, float landY,
                         float radius, int totalDamage, long poisonDuration) {
            this.landTime = java.time.Instant.now().toEpochMilli() + delayMs;
            this.sourcePlayerId = sourcePlayerId;
            this.landX = landX;
            this.landY = landY;
            this.radius = radius;
            this.totalDamage = totalDamage;
            this.poisonDuration = poisonDuration;
        }

        boolean hasLanded() {
            return java.time.Instant.now().toEpochMilli() >= landTime;
        }
    }

    static class PoisonDotState {
        final long enemyId;
        final int totalDamage;
        final long duration;
        final long startTime;
        final long sourcePlayerId;
        int damageApplied;
        long lastTickTime;

        PoisonDotState(long enemyId, int totalDamage, long duration, long sourcePlayerId) {
            this.enemyId = enemyId;
            this.totalDamage = totalDamage;
            this.duration = duration;
            this.startTime = java.time.Instant.now().toEpochMilli();
            this.sourcePlayerId = sourcePlayerId;
            this.damageApplied = 0;
            this.lastTickTime = this.startTime;
        }

        boolean isExpired() {
            return java.time.Instant.now().toEpochMilli() - startTime >= duration;
        }
    }

    static class DecoyState {
        final long enemyId;
        final long sourcePlayerId;
        final long spawnTime;
        final long durationMs;
        final float dx;
        final float dy;
        final float maxTravelDistSq;
        final float originX;
        final float originY;
        boolean stopped;

        DecoyState(long enemyId, long sourcePlayerId, float originX, float originY,
                   float dx, float dy, float maxTravelDist, long durationMs) {
            this.enemyId = enemyId;
            this.sourcePlayerId = sourcePlayerId;
            this.spawnTime = java.time.Instant.now().toEpochMilli();
            this.durationMs = durationMs;
            this.dx = dx;
            this.dy = dy;
            this.maxTravelDistSq = maxTravelDist * maxTravelDist;
            this.originX = originX;
            this.originY = originY;
            this.stopped = false;
        }

        boolean isExpired() {
            return java.time.Instant.now().toEpochMilli() - spawnTime >= durationMs;
        }
    }

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
        try {
            final PlayerAccountDto account = ServerGameLogic.DATA_SERVICE
                    .executeGet("/data/account/" + player.getAccountUuid(), null, PlayerAccountDto.class);
            final List<ChestDto> vaultChests = account.getPlayerVault();
            final int count = vaultChests.size();
            if (count == 0) return;

            // Layout: 2-column grid centered in the vault room
            // Vault map is 32x32 tiles (32px each). Inner room roughly tiles 10-22 x 8-24.
            // Center of room: tile (16, 16) = pixel (512, 512)
            final int cols = 2;
            final int rows = (int) Math.ceil(count / (double) cols);
            final int spacingX = 64;  // horizontal gap between columns
            final int spacingY = 48;  // vertical gap between rows
            final float centerX = 16 * 32;  // map center X
            final float startY = 16 * 32 - (rows * spacingY) / 2f + spacingY / 2f; // vertically centered
            final float leftColX = centerX - spacingX;
            final float rightColX = centerX + spacingX;

            for (int i = 0; i < count; i++) {
                final ChestDto chest = vaultChests.get(i);
                final int col = i % cols;
                final int row = i / cols;
                final float x = col == 0 ? leftColX : rightColX;
                final float y = startY + row * spacingY;

                final List<GameItem> itemsInChest = chest.getItems().stream()
                        .map(GameItem::fromGameItemRef).collect(Collectors.toList());
                final Chest toSpawn = new Chest(new Vector2f(x, y),
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
        this.shortIdAllocator.getOrAssign(player.getId());
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
        this.shortIdAllocator.release(player.getId());
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
        this.shortIdAllocator.release(playerId);
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
        this.shortIdAllocator.getOrAssign(enemy.getId());
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
        this.shortIdAllocator.release(enemy.getId());
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
    private static final int MAX_BULLETS_PER_LOAD = 60;
    private static final int MAX_ENEMIES_PER_LOAD = 80;

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
                Player p = this.players.get(id);
                if (p != null) {
                    float dx = p.getPos().x - center.x;
                    float dy = p.getPos().y - center.y;
                    if (dx * dx + dy * dy <= radiusSq) playersToLoadList.add(p);
                    continue;
                }
                Enemy e = this.enemies.get(id);
                if (e != null) {
                    if (enemiesToLoad.size() >= MAX_ENEMIES_PER_LOAD) continue;
                    float dx = e.getPos().x - center.x;
                    float dy = e.getPos().y - center.y;
                    if (dx * dx + dy * dy <= radiusSq) enemiesToLoad.add(e);
                    continue;
                }
                Bullet b = this.bullets.get(id);
                if (b != null) {
                    if (bulletsToLoad.size() >= MAX_BULLETS_PER_LOAD) continue;
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
                    enemiesToLoad.toArray(new Enemy[0]), portalsToLoad.toArray(new Portal[0]),
                    this.shortIdAllocator);
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
                    enemiesToLoad.toArray(new Enemy[0]), portalsToLoad.toArray(new Portal[0]),
                    this.shortIdAllocator);
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
                    enemiesToLoad.toArray(new Enemy[0]), portalsToLoad.toArray(new Portal[0]),
                    this.shortIdAllocator);
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

        // Use per-terrain enemyDensity if set, otherwise fall back to legacy thresholds.
        // enemyDensity is a 0.0-1.0 probability that each eligible tile spawns an enemy.
        final float density;
        if (params.getEnemyDensity() > 0f) {
            density = params.getEnemyDensity();
        } else {
            // Legacy fallback: ~0.8% for overworld, ~0.4% for dungeons (smaller maps)
            density = hasZones ? 0.011f : 0.004f;
        }

        // Spawn caps for rare/unique enemies
        final Map<Integer, Integer> spawnCaps = new java.util.HashMap<>();
        final Map<Integer, Integer> spawnCounts = new java.util.HashMap<>();
        spawnCaps.put(13, 3);  // The Man: max 3 per realm (summit only)

        for (int i = 1; i < mapHeight; i++) {
            for (int j = 1; j < mapWidth; j++) {
                if (Realm.RANDOM.nextFloat() >= density) continue;

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

                // Enforce spawn caps for rare enemies (e.g., The Man = max 2)
                if (spawnCaps.containsKey(toSpawn.getEnemyId())) {
                    int current = spawnCounts.getOrDefault(toSpawn.getEnemyId(), 0);
                    if (current >= spawnCaps.get(toSpawn.getEnemyId())) continue;
                    spawnCounts.merge(toSpawn.getEnemyId(), 1, Integer::sum);
                }

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

        this.spawnStaticEnemies(mapId);
    }

    /**
     * Register a poison DoT on an enemy in this realm. Poisons stack.
     */
    public void registerPoisonDot(long enemyId, int totalDamage, long duration, long sourcePlayerId) {
        this.activePoisonDots.add(new PoisonDotState(enemyId, totalDamage, duration, sourcePlayerId));
    }

    /**
     * Remove all poison DoTs sourced by a specific player (called on disconnect).
     */
    public void removePlayerPoisonDots(long playerId) {
        this.activePoisonDots.removeIf(dot -> dot.sourcePlayerId == playerId);
    }

    /**
     * Process all active poison DoTs for this realm. Called every server tick.
     * @param mgr the server manager, used for enemyDeath and broadcastTextEffect callbacks
     */
    public void processPoisonDots(RealmManagerServer mgr) {
        if (this.activePoisonDots.isEmpty()) return;
        final long now = java.time.Instant.now().toEpochMilli();
        final java.util.Iterator<PoisonDotState> it = this.activePoisonDots.iterator();
        while (it.hasNext()) {
            final PoisonDotState dot = it.next();
            final Enemy enemy = this.getEnemy(dot.enemyId);
            if (enemy == null || enemy.getDeath()) { it.remove(); continue; }
            if (dot.isExpired()) { it.remove(); continue; }

            if (now - dot.lastTickTime < POISON_TICK_INTERVAL_MS) continue;
            dot.lastTickTime = now;

            int totalTicks = (int) (dot.duration / POISON_TICK_INTERVAL_MS);
            int tickDamage = Math.max(1, dot.totalDamage / Math.max(1, totalTicks));

            if (dot.damageApplied + tickDamage > dot.totalDamage) {
                tickDamage = dot.totalDamage - dot.damageApplied;
            }
            if (tickDamage <= 0) continue;

            dot.damageApplied += tickDamage;
            enemy.setHealth(enemy.getHealth() - tickDamage);
            mgr.broadcastTextEffect(com.jrealm.game.contants.EntityType.ENEMY, enemy,
                    com.jrealm.game.contants.TextEffect.DAMAGE, "-" + tickDamage);

            if (enemy.getDeath()) {
                mgr.enemyDeath(this, enemy);
                it.remove();
            }
        }
    }

    /**
     * Register a pending poison throw. The landing effect will be applied after the delay
     * elapses, checked each tick — no threads are blocked.
     */
    public void registerPoisonThrow(long delayMs, long sourcePlayerId, float landX, float landY,
                                     float radius, int totalDamage, long poisonDuration) {
        this.pendingPoisonThrows.add(new PoisonThrowState(delayMs, sourcePlayerId, landX, landY,
                radius, totalDamage, poisonDuration));
    }

    /**
     * Process pending poison throws. When a throw's travel time has elapsed, apply the
     * splash AoE and poison DoT to enemies in range. Called every server tick.
     */
    public void processPoisonThrows(RealmManagerServer mgr) {
        if (this.pendingPoisonThrows.isEmpty()) return;
        final java.util.Iterator<PoisonThrowState> it = this.pendingPoisonThrows.iterator();
        while (it.hasNext()) {
            final PoisonThrowState t = it.next();
            if (!t.hasLanded()) continue;
            it.remove();

            // Broadcast splash AoE on landing
            mgr.enqueueServerPacket(com.jrealm.net.client.packet.CreateEffectPacket.aoeEffect(
                    com.jrealm.net.client.packet.CreateEffectPacket.EFFECT_POISON_SPLASH,
                    t.landX, t.landY, t.radius, (short) 1500));

            // Apply poison to enemies in radius
            final float radiusSq = t.radius * t.radius;
            for (final Enemy enemy : this.enemies.values()) {
                if (enemy.getDeath()) continue;
                if (enemy.hasEffect(com.jrealm.game.contants.ProjectileEffectType.STASIS)) continue;
                float dx = enemy.getPos().x - t.landX;
                float dy = enemy.getPos().y - t.landY;
                if (dx * dx + dy * dy <= radiusSq) {
                    enemy.addEffect(com.jrealm.game.contants.ProjectileEffectType.POISONED, t.poisonDuration);
                    this.registerPoisonDot(enemy.getId(), t.totalDamage, t.poisonDuration, t.sourcePlayerId);
                    mgr.broadcastTextEffect(com.jrealm.game.contants.EntityType.ENEMY, enemy,
                            com.jrealm.game.contants.TextEffect.DAMAGE, "POISONED");
                }
            }
        }
    }

    /**
     * Remove pending poison throws from a disconnecting player.
     */
    public void removePlayerPoisonThrows(long playerId) {
        this.pendingPoisonThrows.removeIf(t -> t.sourcePlayerId == playerId);
    }

    /**
     * Return a proxy Player positioned at the closest active decoy if it is
     * nearer than {@code currentBestDist}. Used by enemy targeting so decoys
     * draw aggro the same way real players do.
     */
    public Player getClosestDecoyTarget(final com.jrealm.game.math.Vector2f pos, float currentBestDist) {
        Player best = null;
        for (final DecoyState d : this.activeDecoys) {
            final Enemy decoy = this.enemies.get(d.enemyId);
            if (decoy == null) continue;
            final float dist = decoy.getPos().distanceTo(pos);
            if (dist < currentBestDist) {
                currentBestDist = dist;
                best = new Player(d.enemyId, decoy.getPos().clone(),
                        decoy.getSize(), CharacterClass.TRICKSTER);
            }
        }
        return best;
    }

    /**
     * Register a decoy entity. The decoy walks in the given direction until it
     * covers maxTravelDist pixels, then stands still until durationMs expires.
     */
    public void registerDecoy(long enemyId, long sourcePlayerId, float originX, float originY,
                               float dx, float dy, float maxTravelDist, long durationMs) {
        this.activeDecoys.add(new DecoyState(enemyId, sourcePlayerId, originX, originY,
                dx, dy, maxTravelDist, durationMs));
    }

    /**
     * Process active decoys: move them each tick, stop after travel distance,
     * remove after duration expires. Called every server tick.
     */
    public void processDecoys(RealmManagerServer mgr) {
        if (this.activeDecoys.isEmpty()) return;
        final java.util.Iterator<DecoyState> it = this.activeDecoys.iterator();
        while (it.hasNext()) {
            final DecoyState d = it.next();
            final Enemy decoy = this.enemies.get(d.enemyId);

            // Decoy was killed or realm cleaned up
            if (decoy == null || decoy.getDeath()) {
                it.remove();
                continue;
            }

            // Duration expired — remove decoy
            if (d.isExpired()) {
                it.remove();
                this.expiredEnemies.add(d.enemyId);
                this.removeEnemy(decoy);
                continue;
            }

            // Move decoy if it hasn't reached travel distance
            if (!d.stopped) {
                float traveled_x = decoy.getPos().x - d.originX;
                float traveled_y = decoy.getPos().y - d.originY;
                if (traveled_x * traveled_x + traveled_y * traveled_y >= d.maxTravelDistSq) {
                    d.stopped = true;
                    // Stop movement and clear direction flags so walk animation stops
                    decoy.setDx(0);
                    decoy.setDy(0);
                    decoy.setUp(false);
                    decoy.setDown(false);
                    decoy.setLeft(false);
                    decoy.setRight(false);
                } else {
                    decoy.getPos().x += d.dx;
                    decoy.getPos().y += d.dy;
                }
            }
        }
    }

    /**
     * Remove decoys spawned by a disconnecting player.
     */
    public void removePlayerDecoys(long playerId) {
        final java.util.Iterator<DecoyState> it = this.activeDecoys.iterator();
        while (it.hasNext()) {
            final DecoyState d = it.next();
            if (d.sourcePlayerId == playerId) {
                final Enemy decoy = this.enemies.get(d.enemyId);
                if (decoy != null) {
                    this.expiredEnemies.add(d.enemyId);
                    this.removeEnemy(decoy);
                }
                it.remove();
            }
        }
    }

    /**
     * Called automatically when a realm is added, regardless of whether a decorator exists.
     */
    public void spawnStaticEnemies(int mapId) {
        final MapModel mapModel = GameDataManager.MAPS.get(mapId);
        if (mapModel == null || mapModel.getStaticSpawns() == null) return;
        for (final com.jrealm.game.model.StaticSpawn ss : mapModel.getStaticSpawns()) {
            final EnemyModel model = GameDataManager.ENEMIES.get(ss.getEnemyId());
            if (model == null) {
                Realm.log.warn("Static spawn references unknown enemyId={}, skipping", ss.getEnemyId());
                continue;
            }
            final Vector2f pos = new Vector2f(ss.getX(), ss.getY());
            final Enemy enemy = GameObjectUtils.getEnemyFromId(ss.getEnemyId(), pos);
            int healthMult = this.getDifficultyMultiplier();
            enemy.setHealth(enemy.getHealth() * Math.max(1, healthMult));
            enemy.setHealthMultiplier(Math.max(1, healthMult));
            this.addEnemy(enemy);
            Realm.log.info("Static spawn: {} at ({}, {}) in realm mapId={}", model.getName(), ss.getX(), ss.getY(), mapId);
        }
    }

    /**
     * Place set piece structures on the terrain (ruins, graveyards, watchtowers, etc.)
     * Each set piece has a base floor tile and a collision layout that stamps tiles onto the map.
     * Placement uses collision avoidance to prevent overlapping.
     */
    public void placeSetPieces(TerrainGenerationParameters params) {
        if (params.getSetPieces() == null || params.getSetPieces().isEmpty()) return;
        final boolean hasZones = params.getZones() != null && !params.getZones().isEmpty();
        final int tileSize = this.tileManager.getMapLayers().get(0).getTileSize();
        final int mapW = this.tileManager.getMapLayers().get(0).getWidth();
        final int mapH = this.tileManager.getMapLayers().get(0).getHeight();
        final java.util.Set<Long> occupied = new java.util.HashSet<>();

        Realm.log.info("[SET_PIECES] Map {}x{}, tileSize={}, hasZones={}, {} set piece types",
            mapW, mapH, tileSize, hasZones, params.getSetPieces().size());

        for (com.jrealm.game.model.SetPiece sp : params.getSetPieces()) {
            int count = sp.getMinCount() + Realm.RANDOM.nextInt(Math.max(1, sp.getMaxCount() - sp.getMinCount() + 1));
            int placed = 0;
            int zoneRejects = 0, collRejects = 0;

            for (int attempt = 0; attempt < count * 100 && placed < count; attempt++) {
                int px = 4 + Realm.RANDOM.nextInt(Math.max(1, mapW - sp.getWidth() - 8));
                int py = 4 + Realm.RANDOM.nextInt(Math.max(1, mapH - sp.getHeight() - 8));

                // Zone check
                if (hasZones && sp.getAllowedZones() != null) {
                    Vector2f worldPos = new Vector2f(px * tileSize, py * tileSize);
                    OverworldZone zone = this.tileManager.getZoneForPosition(worldPos.x, worldPos.y);
                    if (zone == null || !sp.getAllowedZones().contains(zone.getZoneId())) {
                        zoneRejects++;
                        continue;
                    }
                }

                // Only check for overlap with other set pieces — don't check terrain collision
                // (terrain has too many obstacles, would reject almost everything)
                boolean fits = true;
                for (int dy = 0; dy < sp.getHeight() && fits; dy++) {
                    for (int dx = 0; dx < sp.getWidth() && fits; dx++) {
                        long key = ((long)(py + dy) << 32) | (px + dx);
                        if (occupied.contains(key)) { fits = false; collRejects++; }
                    }
                }
                // Also check the tile isn't void
                Vector2f center = new Vector2f(px * tileSize + tileSize, py * tileSize + tileSize);
                if (this.tileManager.isVoidTile(center, 0, 0)) { fits = false; collRejects++; }
                if (!fits) continue;

                // Place the set piece
                for (int dy = 0; dy < sp.getHeight(); dy++) {
                    for (int dx = 0; dx < sp.getWidth(); dx++) {
                        int tx = px + dx, ty = py + dy;
                        long key = ((long)ty << 32) | tx;
                        occupied.add(key);

                        // Set base tile
                        if (sp.getBaseTileId() > 0) {
                            try {
                                com.jrealm.game.tile.TileData data = GameDataManager.TILES.get(sp.getBaseTileId()) != null
                                    ? GameDataManager.TILES.get(sp.getBaseTileId()).getData() : null;
                                this.tileManager.getMapLayers().get(0).setTileAt(ty, tx,
                                    (short) sp.getBaseTileId(), data);
                            } catch (Exception e) { /* skip */ }
                        }

                        // Set collision tile from layout
                        int[][] layout = sp.getCollisionLayout();
                        if (layout != null && dy < layout.length && dx < layout[dy].length) {
                            int collTileId = layout[dy][dx];
                            if (collTileId > 0) {
                                try {
                                    com.jrealm.game.tile.TileData data = GameDataManager.TILES.get(collTileId) != null
                                        ? GameDataManager.TILES.get(collTileId).getData() : null;
                                    this.tileManager.getMapLayers().get(1).setTileAt(ty, tx,
                                        (short) collTileId, data);
                                } catch (Exception e) { /* skip */ }
                            }
                        }
                    }
                }
                placed++;
            }
            Realm.log.info("[SET_PIECES] '{}': placed {}/{}, zoneRejects={}, collRejects={}",
                sp.getName(), placed, count, zoneRejects, collRejects);
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
        this.playerLock.lock();
    }

    private void releasePlayerLock() {
        this.playerLock.unlock();
    }
}
