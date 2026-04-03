package com.jrealm.game.tile;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.graphics.ShaderManager;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.DungeonGenerationParams;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.model.OverworldZone;
import com.jrealm.game.model.TerrainGenerationParameters;
import com.jrealm.game.model.TileGroup;
import com.jrealm.game.model.TileModel;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.entity.NetTile;
import com.jrealm.net.realm.Realm;
import com.jrealm.util.Camera;
import com.jrealm.util.Partition;
import com.jrealm.util.WorkerThread;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TileManager {
    private static final Integer VIEWPORT_TILE_MIN = 10;
    private static final Integer VIEWPORT_TILE_MAX = 20;
    private final java.util.concurrent.locks.ReentrantLock mapLock = new java.util.concurrent.locks.ReentrantLock();
    private List<TileMap> mapLayers;
    private Vector2f bossSpawnPos;
    private Vector2f playerSpawnPos;
    private TerrainGenerationParameters terrainParams;
    private int mapId;

    // Server side constructor
    public TileManager(int mapId) {
        this.mapId = mapId;
        MapModel model = GameDataManager.MAPS.get(mapId);
        log.info("[TileManager] Building map {}", model);
        // Three types of maps. Fixed data, generated terrain and generated dungeon
        if (model.getData() != null) {
            this.mapLayers = this.getLayersFromData(model);
        } else if (model.getDungeonId()>-1){
        	final DungeonGenerationParams params = model.getDungeonParams();
			final DungeonGenerator dungeonGenerator = new DungeonGenerator(model.getWidth(), model.getHeight(),
					model.getTileSize(), params.getMinRooms(), params.getMaxRooms(), params.getMinRoomWidth(),
					params.getMaxRoomWidth(), params.getMinRoomHeight(), params.getMaxRoomHeight(),
					params.getShapeTemplates(), params.getFloorTileIds(), params.getWallTileId(),
					params.getHallwayStyles(), params.getBossEnemyId());
            this.mapLayers = dungeonGenerator.generateDungeon();
            if (dungeonGenerator.getBossRoomCenterX() >= 0 && dungeonGenerator.getBossRoomCenterY() >= 0) {
                this.bossSpawnPos = new Vector2f(
                        dungeonGenerator.getBossRoomCenterX() * model.getTileSize(),
                        dungeonGenerator.getBossRoomCenterY() * model.getTileSize());
            }
            if (dungeonGenerator.getSpawnRoomCenterX() >= 0 && dungeonGenerator.getSpawnRoomCenterY() >= 0) {
                this.playerSpawnPos = new Vector2f(
                        dungeonGenerator.getSpawnRoomCenterX() * model.getTileSize(),
                        dungeonGenerator.getSpawnRoomCenterY() * model.getTileSize());
            }
        } else if(model.getTerrainId()>-1){
            final TerrainGenerationParameters params = GameDataManager.TERRAINS.get(model.getTerrainId());
            this.terrainParams = params;
            this.mapLayers = this.getLayersFromTerrain(model.getWidth(), model.getHeight(), model.getTileSize(),
                    params);
        }
    }

    public TileManager(int width, int height, int tileSize, TerrainGenerationParameters params) {
        this.mapLayers = this.getLayersFromTerrain(width, height, tileSize, params);
    }

    // Client side constructor
    public TileManager(MapModel model) {
        this.mapLayers = new ArrayList<>();
        TileMap baseLayer = new TileMap((short) model.getMapId(), model.getTileSize(), model.getWidth(),
                model.getHeight());
        TileMap collisionLayer = new TileMap((short) model.getMapId(), model.getTileSize(), model.getWidth(),
                model.getHeight());
        this.mapLayers.add(baseLayer);
        this.mapLayers.add(collisionLayer);
    }

    // Get the zone for a world position (returns null if no zones defined)
    public OverworldZone getZoneForPosition(float worldX, float worldY) {
        if (this.terrainParams == null || this.terrainParams.getZones() == null
                || this.terrainParams.getZones().isEmpty()) {
            return null;
        }
        final int width = this.getBaseLayer().getWidth();
        final int height = this.getBaseLayer().getHeight();
        final float centerX = width * GlobalConstants.BASE_TILE_SIZE / 2f;
        final float centerY = height * GlobalConstants.BASE_TILE_SIZE / 2f;
        final float maxDist = (float) Math.sqrt(centerX * centerX + centerY * centerY);
        final float dx = worldX - centerX;
        final float dy = worldY - centerY;
        final float dist = (float) Math.sqrt(dx * dx + dy * dy);
        final float normalizedDist = dist / maxDist;

        for (OverworldZone zone : this.terrainParams.getZones()) {
            if (normalizedDist >= zone.getMinRadius() && normalizedDist < zone.getMaxRadius()) {
                return zone;
            }
        }
        // Fallback: return outermost zone
        return this.terrainParams.getZones().get(this.terrainParams.getZones().size() - 1);
    }

    // Generates a random terrain of size with the given parameters
    private List<TileMap> getLayersFromTerrain(int width, int height, int tileSize,
            TerrainGenerationParameters params) {
        final Random random = new Random(Instant.now().toEpochMilli());
        TileMap baseLayer = new TileMap(tileSize, width, height);
        TileMap collisionLayer = new TileMap(tileSize, width, height);

        final boolean hasZones = params.getZones() != null && !params.getZones().isEmpty();

        if (hasZones) {
            // Zone-based terrain: each tile gets its TileGroup from its zone
            final float centerX = width / 2f;
            final float centerY = height / 2f;
            final float maxDist = (float) Math.sqrt(centerX * centerX + centerY * centerY);

            // Pre-resolve tile models per group: base terrain (layer 0) and decorations (layer 1)
            final Map<Integer, List<TileModel>> baseByGroup = new java.util.HashMap<>();
            final Map<Integer, List<TileModel>> decorationByGroup = new java.util.HashMap<>();
            for (TileGroup group : params.getTileGroups()) {
                List<TileModel> baseTiles = group.getTileIds().stream()
                        .map(id -> GameDataManager.TILES.get(id))
                        .filter(tm -> tm != null)
                        .collect(Collectors.toList());
                baseByGroup.put(group.getOrdinal(), baseTiles);

                List<Integer> decoIds = group.getDecorationTileIds();
                List<TileModel> decoTiles = (decoIds != null) ? decoIds.stream()
                        .map(id -> GameDataManager.TILES.get(id))
                        .filter(tm -> tm != null)
                        .collect(Collectors.toList()) : new java.util.ArrayList<>();
                decorationByGroup.put(group.getOrdinal(), decoTiles);
            }

            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    float dx = col - centerX;
                    float dy = row - centerY;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float normalizedDist = dist / maxDist;

                    // Find zone for this tile
                    OverworldZone zone = null;
                    for (OverworldZone z : params.getZones()) {
                        if (normalizedDist >= z.getMinRadius() && normalizedDist < z.getMaxRadius()) {
                            zone = z;
                            break;
                        }
                    }
                    if (zone == null) {
                        zone = params.getZones().get(params.getZones().size() - 1);
                    }

                    int groupOrd = zone.getTileGroupOrdinal();
                    TileGroup group = params.getTileGroups().stream()
                            .filter(g -> g.getOrdinal() == groupOrd).findFirst()
                            .orElse(params.getTileGroups().get(0));

                    // Base layer tile (always placed — opaque ground)
                    List<TileModel> baseTiles = baseByGroup.getOrDefault(groupOrd,
                            baseByGroup.values().iterator().next());
                    if (!baseTiles.isEmpty()) {
                        TileModel tile = baseTiles.get(random.nextInt(baseTiles.size()));
                        float rarity = group.getRarities().getOrDefault(tile.getTileId() + "", 1.0f);
                        if (rarity > 0 && random.nextFloat() <= rarity) {
                            baseLayer.setTileAt(row, col, (short) tile.getTileId(), tile.getData());
                        } else {
                            tile = baseTiles.get(0);
                            baseLayer.setTileAt(row, col, (short) tile.getTileId(), tile.getData());
                        }
                    }

                    // Decoration/collision layer tile (placed on layer 1 over the base)
                    List<TileModel> decoTiles = decorationByGroup.getOrDefault(groupOrd,
                            java.util.Collections.emptyList());
                    if (!decoTiles.isEmpty()) {
                        TileModel tile = decoTiles.get(random.nextInt(decoTiles.size()));
                        float rarity = group.getRarities().getOrDefault(tile.getTileId() + "", 0.0f);
                        if (rarity > 0 && random.nextFloat() <= rarity) {
                            collisionLayer.setTileAt(row, col, (short) tile.getTileId(), tile.getData());
                        }
                    }
                }
            }
        } else {
            // Legacy single-group terrain generation
            for (TileGroup group : params.getTileGroups()) {
                List<TileModel> baseTiles = group.getTileIds().stream()
                        .map(id -> GameDataManager.TILES.get(id))
                        .filter(tm -> tm != null)
                        .collect(Collectors.toList());

                List<Integer> decoIds = group.getDecorationTileIds();
                List<TileModel> decoTiles = (decoIds != null) ? decoIds.stream()
                        .map(id -> GameDataManager.TILES.get(id))
                        .filter(tm -> tm != null)
                        .collect(Collectors.toList()) : new java.util.ArrayList<>();

                // Fill base layer with terrain tiles
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        TileModel tileIdToCreate = baseTiles.get(random.nextInt(baseTiles.size()));
                        float rarity = group.getRarities().getOrDefault(tileIdToCreate.getTileId() + "", 1.0f);
                        if ((rarity > 0.0) && (random.nextFloat() <= rarity)) {
                            baseLayer.setTileAt(i, j, (short) tileIdToCreate.getTileId(), tileIdToCreate.getData());
                        } else {
                            tileIdToCreate = baseTiles.get(0);
                            baseLayer.setTileAt(i, j, (short) tileIdToCreate.getTileId(), tileIdToCreate.getData());
                        }
                    }
                }
                // Fill decoration/collision layer from decorationTileIds
                if (!decoTiles.isEmpty()) {
                    for (int i = 0; i < height; i++) {
                        for (int j = 0; j < width; j++) {
                            TileModel tileIdToCreate = decoTiles.get(random.nextInt(decoTiles.size()));
                            float rarity = group.getRarities().getOrDefault(tileIdToCreate.getTileId() + "", 0.0f);
                            if ((rarity > 0.0) && (random.nextFloat() <= rarity)) {
                                collisionLayer.setTileAt(i, j, (short) tileIdToCreate.getTileId(),
                                        tileIdToCreate.getData());
                            }
                        }
                    }
                }
            }
        }
        return Arrays.asList(baseLayer, collisionLayer);
    }

    // Builds map layers from a map model that has statically defined layers (is not
    // a terrain)
    private List<TileMap> getLayersFromData(MapModel model) {
        Map<String, int[][]> layerMap = model.getData();
        TileMap baseLayer = new TileMap((short) model.getMapId(), model.getTileSize(), model.getWidth(),
                model.getHeight());
        TileMap collisionLayer = new TileMap((short) model.getMapId(), model.getTileSize(), model.getWidth(),
                model.getHeight());

        final int[][] baseData = layerMap.get("0");
        final int[][] collisionData = layerMap.get("1");

        for (int i = 0; i < baseData.length; i++) {
            for (int j = 0; j < baseData[i].length; j++) {
                int tileIdToCreate = baseData[i][j];
                TileData tileData = GameDataManager.TILES.get(tileIdToCreate).getData();
                baseLayer.setTileAt(i, j, (short) tileIdToCreate, tileData);
            }
        }

        for (int i = 0; i < collisionData.length; i++) {
            for (int j = 0; j < collisionData[i].length; j++) {
                int tileIdToCreate = collisionData[i][j];
                TileData tileData = GameDataManager.TILES.get(tileIdToCreate).getData();
                collisionLayer.setTileAt(i, j, (short) tileIdToCreate, tileData);
            }
        }
        return Arrays.asList(baseLayer, collisionLayer);

    }
    
    public Tile[] getBaseTiles(Vector2f pos) {
        Tile[] block = new Tile[144];
        Vector2f posNormalized = new Vector2f(pos.x / GlobalConstants.BASE_TILE_SIZE,
                pos.y / GlobalConstants.BASE_TILE_SIZE);
        this.normalizeToBounds(posNormalized);
        int i = 0;
        for (int x = (int) (posNormalized.x - 5); x < (posNormalized.x + 6); x++) {
            for (int y = (int) (posNormalized.y - 5); y < (int) (posNormalized.y + 6); y++) {
                if ((x >= this.getBaseLayer().getWidth()) || (y >= this.getBaseLayer().getHeight()) || (x < 0)
                        || (y < 0)) {
                    continue;
                }
                try {
                    block[i] = (Tile) this.mapLayers.get(0).getBlocks()[y][x];
                    i++;
                } catch (Exception e) {

                }
            }
        }
        return block;
    }

    public Tile[] getCollisionTiles(Vector2f pos) {
        Tile[] block = new Tile[144];
        Vector2f posNormalized = new Vector2f(pos.x / GlobalConstants.BASE_TILE_SIZE,
                pos.y / GlobalConstants.BASE_TILE_SIZE);
        this.normalizeToBounds(posNormalized);
        int i = 0;
        for (int x = (int) (posNormalized.x - 5); x < (posNormalized.x + 6); x++) {
            for (int y = (int) (posNormalized.y - 5); y < (int) (posNormalized.y + 6); y++) {
                if ((x >= this.getCollisionLayer().getWidth()) || (y >= this.getCollisionLayer().getHeight()) || (x < 0)
                        || (y < 0)) {
                    continue;
                }
                try {
                    block[i] = (Tile) this.mapLayers.get(1).getBlocks()[y][x];
                    i++;
                } catch (Exception e) {

                }
            }
        }
        return block;
    }

    public TileMap getCollisionLayer() {
        return this.mapLayers.get(this.mapLayers.size() - 1);
    }

    public TileMap getBaseLayer() {
        return this.mapLayers.get(0);
    }

    private void normalizeToBounds(Vector2f pos) {
        if (pos.x < 0) {
            pos.x = 0;
        }
        if (pos.x > (this.getBaseLayer().getWidth() - 1)) {
            pos.x = this.getBaseLayer().getWidth() - 1;
        }

        if (pos.y < 0) {
            pos.y = 0;
        }
        if (pos.y > (this.getBaseLayer().getHeight() - 1)) {
            pos.y = this.getBaseLayer().getWidth() - 1;
        }
    }

    public Vector2f getSafePosition() {
        // If the map defines explicit spawn points, pick one randomly
        if (this.mapId > 0) {
            MapModel model = GameDataManager.MAPS.get(this.mapId);
            if (model != null && model.getSpawnPoints() != null && !model.getSpawnPoints().isEmpty()) {
                return model.getRandomSpawnPoint();
            }
        }
        // If zones are defined, spawn in the outermost zone (beach/shore)
        if (this.terrainParams != null && this.terrainParams.getZones() != null
                && !this.terrainParams.getZones().isEmpty()) {
            // Find the zone with the highest maxRadius (outermost)
            OverworldZone outerZone = this.terrainParams.getZones().stream()
                    .max((a, b) -> Float.compare(a.getMaxRadius(), b.getMaxRadius()))
                    .orElse(null);
            if (outerZone != null) {
                return this.getSafePositionInZone(outerZone);
            }
        }
        Vector2f pos = this.randomPos();
        int attempts = 0;
        while ((this.collidesAtPosition(pos, GlobalConstants.BASE_TILE_SIZE) || this.isVoidTile(pos, 0, 0))
                && attempts < 500) {
            pos = this.randomPos();
            attempts++;
        }
        return pos;
    }

    public Vector2f getSafePositionInZone(OverworldZone zone) {
        final int width = this.getBaseLayer().getWidth();
        final int height = this.getBaseLayer().getHeight();
        final float centerX = width * GlobalConstants.BASE_TILE_SIZE / 2f;
        final float centerY = height * GlobalConstants.BASE_TILE_SIZE / 2f;
        final float maxDist = (float) Math.sqrt(centerX * centerX + centerY * centerY);
        final float minDist = zone.getMinRadius() * maxDist;
        final float maxDistZone = zone.getMaxRadius() * maxDist;

        for (int attempts = 0; attempts < 500; attempts++) {
            Vector2f pos = this.randomPos();
            if (this.collidesAtPosition(pos, GlobalConstants.BASE_TILE_SIZE) || this.isVoidTile(pos, 0, 0)) continue;
            float dx = pos.x - centerX;
            float dy = pos.y - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist >= minDist && dist < maxDistZone) {
                return pos;
            }
        }
        // Fallback if zone is too small or all positions blocked
        Vector2f pos = this.randomPos();
        int fallbackAttempts = 0;
        while ((this.collidesAtPosition(pos, GlobalConstants.BASE_TILE_SIZE) || this.isVoidTile(pos, 0, 0))
                && fallbackAttempts < 500) {
            pos = this.randomPos();
            fallbackAttempts++;
        }
        return pos;
    }

    public boolean isCollisionTile(Vector2f pos) {
        final TileMap collisionLayer = this.getCollisionLayer();
        final int tileX = (int) pos.x / collisionLayer.getTileSize();
        final int tileY = (int) pos.y / collisionLayer.getTileSize();
        // If the player clicks off the map
        if(!collisionLayer.isValidPosition(tileX, tileY)){
        	return true;
        }
        final Tile currentTile = collisionLayer.getBlocks()[tileY][tileX];
        return (currentTile != null) && !currentTile.isVoid();
    }
    
    public boolean isVoidTile(Vector2f pos, float dx, float dy) {
        final TileMap collisionLayer = this.getBaseLayer();
        final int tileX = (int) ((float)pos.x + dx) / collisionLayer.getTileSize();
        final int tileY = (int) ((float)pos.y + dy)/ collisionLayer.getTileSize();
        if(tileY>=collisionLayer.getBlocks().length || tileX>=collisionLayer.getBlocks()[0].length) {
            return false;
        }
        final Tile currentTile = collisionLayer.getBlocks()[tileY][tileX];
        if(currentTile==null) {
            return false;
        }
        return currentTile.isVoid();
    }

    public boolean collidesXLimit(Entity e, float ax) {
        final Vector2f futurePos = e.getPos().clone(ax, 0);
        return (futurePos.x <= 0) || ((futurePos.x + e.getSize()) >= (this.getBaseLayer().getWidth()
                * this.getBaseLayer().getTileSize()));

    }

    public boolean collidesYLimit(Entity e, float dy) {
        final Vector2f futurePos = e.getPos().clone(0, dy);
        return (futurePos.y <= 0) || ((futurePos.y + e.getSize()) >= (this.getBaseLayer().getHeight()
                * this.getBaseLayer().getTileSize()));

    }
    
    public boolean collidesVoidTile(Entity e) {
        final Vector2f centerPos = e.getCenteredPosition();
        final int startX = (int) (centerPos.x / (float) this.getBaseLayer().getTileSize());
        final int startY = (int) (centerPos.y / (float) this.getBaseLayer().getTileSize());

        final Tile currentTile = this.getBaseLayer().getBlocks()[startY][startX];
        if(!currentTile.isVoid()) {
            return false;
        }
        final Rectangle tileBounds = new Rectangle(currentTile.getPos(), currentTile.getWidth(),
                currentTile.getHeight());
        final Rectangle futurePosBounds = new Rectangle(e.getPos(), (e.getSize() / 2), e.getSize() / 2);

        return currentTile.isVoid() && tileBounds.intersect(futurePosBounds);
    }

    public boolean collidesSlowTile(Entity e) {
        final Vector2f centerPos = e.getCenteredPosition();
        final int startX = (int) (centerPos.x / (float) this.getBaseLayer().getTileSize());
        final int startY = (int) (centerPos.y / (float) this.getBaseLayer().getTileSize());

        final Tile currentTile = this.getBaseLayer().getBlocks()[startY][startX];

        final Rectangle tileBounds = new Rectangle(currentTile.getPos(), currentTile.getWidth(),
                currentTile.getHeight());
        final Rectangle futurePosBounds = new Rectangle(e.getPos(), (e.getSize()), e.getSize());

        return currentTile.getData().slows() && tileBounds.intersect(futurePosBounds);
    }
    
    public boolean collidesDamagingTile(Entity e) {
        final Vector2f centerPos = e.getCenteredPosition();
        final int startX = (int) (centerPos.x / (float) this.getBaseLayer().getTileSize());
        final int startY = (int) (centerPos.y / (float) this.getBaseLayer().getTileSize());

        final Tile currentTile = this.getBaseLayer().getBlocks()[startY][startX];

        final Rectangle tileBounds = new Rectangle(currentTile.getPos(), currentTile.getWidth(),
                currentTile.getHeight());
        final Rectangle futurePosBounds = new Rectangle(e.getPos(), (e.getSize() / 2), e.getSize() / 2);

        return currentTile.getData().damaging() && tileBounds.intersect(futurePosBounds);
    }

    public boolean collisionTile(Entity e, float ax, float ay) {
        final Vector2f futurePos = e.getPos().clone(ax, ay);
        // Use a tighter hitbox (85% of sprite size) for collision
        final int hitSize = (int) (e.getSize() * 0.85f);
        for (Tile t : this.getCollisionTiles(e.getPos())) {
            if ((t == null) || t.isVoid()) {
                continue;
            }
            Rectangle tileBounds = new Rectangle(t.getPos(), t.getWidth(), t.getHeight());
            Rectangle futurePosBounds = new Rectangle(futurePos, hitSize, hitSize);
            if (tileBounds.intersect(futurePosBounds))
                return true;
        }

        return false;
    }

    /**
     * Hitbox-based collision check at an arbitrary position and size.
     * Use this to validate a destination before placing/teleporting an entity.
     */
    public boolean collidesAtPosition(Vector2f pos, int entitySize) {
        final int hitSize = (int) (entitySize * 0.85f);
        for (Tile t : this.getCollisionTiles(pos)) {
            if (t == null || t.isVoid()) continue;
            Rectangle tileBounds = new Rectangle(t.getPos(), t.getWidth(), t.getHeight());
            Rectangle entityBounds = new Rectangle(pos, hitSize, hitSize);
            if (tileBounds.intersect(entityBounds)) return true;
        }
        return false;
    }

    public Vector2f randomPos() {
        final float x = Realm.RANDOM.nextInt(this.getBaseLayer().getWidth()) * this.getBaseLayer().getTileSize();
        final float y = Realm.RANDOM.nextInt(this.getBaseLayer().getHeight()) * this.getBaseLayer().getTileSize();
        return new Vector2f(x, y);
    }

    public Rectangle getRenderViewPort(Camera cam) {
        final Vector2f tmpPos = VIEWPORT_POS.get();
        tmpPos.x = cam.getTarget().getPos().x - (VIEWPORT_TILE_MIN * GlobalConstants.BASE_TILE_SIZE);
        tmpPos.y = cam.getTarget().getPos().y - (VIEWPORT_TILE_MIN * GlobalConstants.BASE_TILE_SIZE);
        final Rectangle rect = VIEWPORT_RECT.get();
        rect.setBox(tmpPos, VIEWPORT_TILE_MAX * GlobalConstants.BASE_TILE_SIZE,
                VIEWPORT_TILE_MAX * GlobalConstants.BASE_TILE_SIZE);
        return rect;
    }

    // Reusable viewport rectangles to avoid allocation every frame/tick
    private static final ThreadLocal<Rectangle> VIEWPORT_RECT = ThreadLocal.withInitial(
            () -> new Rectangle(new Vector2f(), 0, 0));
    private static final ThreadLocal<Vector2f> VIEWPORT_POS = ThreadLocal.withInitial(Vector2f::new);

    public Rectangle getRenderViewPort(Entity p) {
        final Vector2f tmpPos = VIEWPORT_POS.get();
        tmpPos.x = p.getPos().x - (VIEWPORT_TILE_MIN * GlobalConstants.BASE_TILE_SIZE);
        tmpPos.y = p.getPos().y - (VIEWPORT_TILE_MIN * GlobalConstants.BASE_TILE_SIZE);
        final Rectangle rect = VIEWPORT_RECT.get();
        rect.setBox(tmpPos, VIEWPORT_TILE_MAX * GlobalConstants.BASE_TILE_SIZE,
                VIEWPORT_TILE_MAX * GlobalConstants.BASE_TILE_SIZE);
        return rect;
    }

    public Rectangle getRenderViewPort(Entity p, Integer tiles) {
        final Vector2f tmpPos = VIEWPORT_POS.get();
        tmpPos.x = p.getPos().x - (tiles * GlobalConstants.BASE_TILE_SIZE);
        tmpPos.y = p.getPos().y - (tiles * GlobalConstants.BASE_TILE_SIZE);
        final Rectangle rect = VIEWPORT_RECT.get();
        rect.setBox(tmpPos, tiles * 2 * GlobalConstants.BASE_TILE_SIZE,
                tiles * 2 * GlobalConstants.BASE_TILE_SIZE);
        return rect;
    }

    public NetTile[] getLoadMapTiles(Player player) {
        final int playerSize = player.getSize() / 2;
        final Vector2f pos = player.getPos().clone(playerSize, playerSize);
        final List<NetTile> tiles = new ArrayList<>();
        final Vector2f posNormalized = new Vector2f(pos.x / GlobalConstants.BASE_TILE_SIZE,
                pos.y / GlobalConstants.BASE_TILE_SIZE);
        this.normalizeToBounds(posNormalized);
        final float radiusSq = VIEWPORT_TILE_MIN * VIEWPORT_TILE_MIN;
        for (int x = (int) (posNormalized.x - VIEWPORT_TILE_MIN); x < (posNormalized.x + VIEWPORT_TILE_MIN); x++) {
            for (int y = (int) (posNormalized.y - VIEWPORT_TILE_MIN); y < (int) (posNormalized.y + VIEWPORT_TILE_MIN); y++) {
                if ((x >= this.getBaseLayer().getWidth()) || (y >= this.getBaseLayer().getHeight()) || (x < 0)
                        || (y < 0)) {
                    continue;
                }
                float dx = x - posNormalized.x;
                float dy = y - posNormalized.y;
                if (dx * dx + dy * dy > radiusSq) continue;
                try {
                    Tile collisionTile = (Tile) this.mapLayers.get(1).getBlocks()[y][x];
                    Tile normalTile = (Tile) this.mapLayers.get(0).getBlocks()[y][x];
                    if (collisionTile != null) {
                        NetTile collisionNetTile = new NetTile(collisionTile.getTileId(), (byte) 1, y, x);
                        tiles.add(collisionNetTile);
                    }

                    if (normalTile != null) {
                        NetTile normalNetTile = new NetTile(normalTile.getTileId(), (byte) 0, y, x);
                        tiles.add(normalNetTile);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return tiles.toArray(new NetTile[0]);
    }
    
    public short getMapWidth() {
        return (short) this.getBaseLayer().getWidth();
    }
    
    public short getMapHeight() {
        return (short) this.getBaseLayer().getHeight();
    }

    public void mergeMap(LoadMapPacket packet) {
    	// Acquire the map lock to prevent the render thread from displaying out of 
    	// date tile information
    	this.acquireMapLock();
        // Resize the map on dimension change
        if(this.getMapHeight()!=packet.getMapHeight() || this.getMapWidth()!=packet.getMapWidth()) {
           MapModel model = GameDataManager.MAPS.get((int)packet.getMapId());
           TileMap baseLayer = new TileMap((short) model.getMapId(), model.getTileSize(), model.getWidth(),
                   model.getHeight());
           TileMap collisionLayer = new TileMap((short) model.getMapId(), model.getTileSize(), model.getWidth(),
                   model.getHeight());
           this.mapLayers = new ArrayList<>();
           this.mapLayers.add(baseLayer);
           this.mapLayers.add(collisionLayer);
        }

        for (NetTile tile : packet.getTiles()) {
            TileData data = GameDataManager.TILES.get((int) tile.getTileId()).getData();
            
            this.mapLayers.get((int) tile.getLayer()).setTileAt(tile.getXIndex(), tile.getYIndex(), tile.getTileId(),
                    data);
        }
        this.releaseMapLock();
    }

    public void render(Player player, SpriteBatch batch, ShapeRenderer shapes) {
        this.acquireMapLock();
        final int playerSize = player.getSize() / 2;
        final Vector2f pos = player.getPos().clone(playerSize, playerSize);
        final Vector2f posNormalized = new Vector2f(pos.x / GlobalConstants.BASE_TILE_SIZE,
                pos.y / GlobalConstants.BASE_TILE_SIZE);
        this.normalizeToBounds(posNormalized);

        // Separate collision layer tiles into walls, objects, and decorations
        final List<Tile> wallTiles = new ArrayList<>();
        final List<Tile> objectTiles = new ArrayList<>();
        final List<Tile> decorationTiles = new ArrayList<>();
        final List<Tile> waterTiles = new ArrayList<>();

        // Pass 1: Draw all base tiles (circular viewport) and classify collision layer tiles
        final float radiusSq = VIEWPORT_TILE_MIN * VIEWPORT_TILE_MIN;
        for (int x = (int) (posNormalized.x - VIEWPORT_TILE_MIN); x < (posNormalized.x + VIEWPORT_TILE_MIN); x++) {
            for (int y = (int) (posNormalized.y - VIEWPORT_TILE_MIN); y < (int) (posNormalized.y + VIEWPORT_TILE_MIN); y++) {
                if ((x >= this.getBaseLayer().getWidth()) || (y >= this.getBaseLayer().getHeight()) || (x < 0)
                        || (y < 0)) {
                    continue;
                }
                float dx = x - posNormalized.x;
                float dy = y - posNormalized.y;
                if (dx * dx + dy * dy > radiusSq) continue;
                try {
                    Tile normalTile = (Tile) this.mapLayers.get(0).getBlocks()[y][x];
                    Tile collisionTile = (Tile) this.mapLayers.get(1).getBlocks()[y][x];

                    if (normalTile != null) {
                        normalTile.render(batch);
                        boolean isWaterTile = normalTile.getData() != null && normalTile.getData().slows()
                                && !normalTile.getData().hasCollision();
                        if (isWaterTile) {
                            waterTiles.add(normalTile);
                        }
                    }

                    // Classify collision layer tiles
                    if (collisionTile != null && !collisionTile.isVoid()) {
                        boolean baseIsWater = normalTile != null && normalTile.getData() != null
                                && normalTile.getData().slows() && !normalTile.getData().hasCollision();
                        if (baseIsWater) {
                            // Skip collision effects over water
                        } else if (collisionTile.getData() != null && collisionTile.getData().isWall()) {
                            // Wall tiles get 3D effect (shadow + contour + side face)
                            wallTiles.add(collisionTile);
                        } else if (collisionTile.getData() != null && collisionTile.getData().hasCollision()) {
                            // Non-wall collision tiles get elliptical shadow
                            objectTiles.add(collisionTile);
                        } else {
                            // Non-collision decorative tile on collision layer
                            decorationTiles.add(collisionTile);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Pass 2: Render wall tiles with 3D effect (shadow + side face + contour)
        if (!wallTiles.isEmpty()) {
            // Shadow: small offset, flush with tile bottom
            ShaderManager.applyEffect(batch, Sprite.EffectEnum.SILHOUETTE);
            batch.setColor(1, 1, 1, 0.25f);
            for (Tile t : wallTiles) {
                TextureRegion region = GameSpriteManager.TILE_SPRITES.get((int) t.getTileId());
                if (region == null) continue;
                float wx = t.getPos().getWorldVar().x;
                float wy = t.getPos().getWorldVar().y;
                int sz = t.getWidth();
                batch.draw(region, wx + 1, wy + 1, sz, sz);
            }
            batch.setColor(1, 1, 1, 1);

            // Dark contour outline (1px)
            float ox = 1.0f;
            for (Tile t : wallTiles) {
                TextureRegion region = GameSpriteManager.TILE_SPRITES.get((int) t.getTileId());
                if (region == null) continue;
                float wx = t.getPos().getWorldVar().x;
                float wy = t.getPos().getWorldVar().y;
                int sz = t.getWidth();
                batch.draw(region, wx + ox, wy, sz, sz);
                batch.draw(region, wx - ox, wy, sz, sz);
                batch.draw(region, wx, wy + ox, sz, sz);
                batch.draw(region, wx, wy - ox, sz, sz);
            }
            ShaderManager.clearEffect(batch);

            // Thin darkened side face directly below wall tile
            for (Tile t : wallTiles) {
                TextureRegion region = GameSpriteManager.TILE_SPRITES.get((int) t.getTileId());
                if (region == null) continue;
                float wx = t.getPos().getWorldVar().x;
                float wy = t.getPos().getWorldVar().y;
                int sz = t.getWidth();
                int sideHeight = Math.max(2, sz / 8);
                batch.setColor(0.2f, 0.2f, 0.25f, 1f);
                batch.draw(region, wx, wy + sz, sz, sideHeight);
                batch.setColor(1, 1, 1, 1);
            }

            // Main wall tile on top
            for (Tile t : wallTiles) {
                t.render(batch);
            }
        }

        // Pass 3: Render object tiles (collision decorations) with circular shadow
        if (!objectTiles.isEmpty()) {
            batch.end();
            com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
            com.badlogic.gdx.Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                    com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0f, 0f, 0f, 0.3f);
            for (Tile t : objectTiles) {
                float wx = t.getPos().getWorldVar().x;
                float wy = t.getPos().getWorldVar().y;
                int sz = t.getWidth();
                float cx = wx + sz / 2f;
                float cy = wy + sz - sz * 0.1f;
                shapes.ellipse(cx - sz * 0.35f, cy - sz * 0.08f, sz * 0.7f, sz * 0.16f);
            }
            shapes.end();
            com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
            batch.begin();

            for (Tile t : objectTiles) {
                t.render(batch);
            }
        }

        // Pass 4: Draw decorative (non-collision) tiles from collision layer
        for (Tile t : decorationTiles) {
            t.render(batch);
        }

        // Pass 5: Redraw water tiles on top so shadows don't cover them
        for (Tile t : waterTiles) {
            t.render(batch);
        }

        this.releaseMapLock();
    }
    
    public void releaseMapLock() {
    	this.mapLock.unlock();
    }

    public void acquireMapLock() {
    	this.mapLock.lock();
    }
}
