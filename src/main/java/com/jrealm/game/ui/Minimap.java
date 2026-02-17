package com.jrealm.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Player;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.state.PlayState;
import com.jrealm.game.tile.Tile;
import com.jrealm.game.tile.TileManager;
import com.jrealm.game.tile.TileMap;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Minimap {
    private static final int MINIMAP_PX = 200;
    private static final int MARGIN = 10;
    private static final int DISCOVER_RADIUS = 12;

    private static final Color FLOOR_COLOR_0 = new Color(0.45f, 0.38f, 0.30f, 1f);
    private static final Color FLOOR_COLOR_1 = new Color(0.50f, 0.42f, 0.33f, 1f);
    private static final Color FLOOR_COLOR_2 = new Color(0.40f, 0.35f, 0.28f, 1f);
    private static final Color WALL_COLOR = new Color(1f, 1f, 1f, 1f);
    private static final Color PLAYER_COLOR = new Color(0.2f, 1f, 0.2f, 1f);
    private static final Color BG_COLOR = new Color(0f, 0f, 0f, 0.7f);
    private static final Color BORDER_COLOR = new Color(1f, 1f, 1f, 0.8f);

    private PlayState playState;
    private boolean[][] discovered;
    private int mapWidth;
    private int mapHeight;
    private int zoomLevel = 40;
    private boolean visible = true;

    public Minimap(final PlayState playState) {
        this.playState = playState;
    }

    public boolean isInitialized() {
        return this.discovered != null && this.mapWidth > 0 && this.mapHeight > 0;
    }

    public void initializeMap(final Integer mapId) {
        final MapModel mapModel = GameDataManager.MAPS.get(mapId);
        this.mapWidth = mapModel.getWidth();
        this.mapHeight = mapModel.getHeight();
        this.discovered = new boolean[this.mapHeight][this.mapWidth];
        this.zoomLevel = 40;
    }

    public void toggle() {
        this.visible = !this.visible;
    }

    public void zoomIn() {
        this.zoomLevel = Math.max(10, this.zoomLevel - 1);
    }

    public void zoomOut() {
        this.zoomLevel = Math.min(Math.max(this.mapWidth, this.mapHeight) / 2, this.zoomLevel + 1);
    }

    public void update() {
        if (!this.isInitialized()) return;

        final Player player = this.playState.getPlayer();
        if (player == null) return;

        final int tileSize = GlobalConstants.BASE_TILE_SIZE;
        final int playerTileX = (int) (player.getPos().x + player.getSize() / 2f) / tileSize;
        final int playerTileY = (int) (player.getPos().y + player.getSize() / 2f) / tileSize;

        final int minY = Math.max(0, playerTileY - DISCOVER_RADIUS);
        final int maxY = Math.min(this.mapHeight - 1, playerTileY + DISCOVER_RADIUS);
        final int minX = Math.max(0, playerTileX - DISCOVER_RADIUS);
        final int maxX = Math.min(this.mapWidth - 1, playerTileX + DISCOVER_RADIUS);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                this.discovered[y][x] = true;
            }
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes) {
        if (!this.visible || !this.isInitialized()) return;

        final Player player = this.playState.getPlayer();
        if (player == null) return;

        final TileManager tm = this.playState.getRealmManager().getRealm().getTileManager();
        final TileMap baseLayer = tm.getBaseLayer();
        final TileMap collisionLayer = tm.getCollisionLayer();

        final int tileSize = GlobalConstants.BASE_TILE_SIZE;
        final int centerTileX = (int) (player.getPos().x + player.getSize() / 2f) / tileSize;
        final int centerTileY = (int) (player.getPos().y + player.getSize() / 2f) / tileSize;

        // Calculate view bounds clamped to map
        int viewStartX = Math.max(0, centerTileX - this.zoomLevel);
        int viewEndX = Math.min(this.mapWidth, centerTileX + this.zoomLevel);
        int viewStartY = Math.max(0, centerTileY - this.zoomLevel);
        int viewEndY = Math.min(this.mapHeight, centerTileY + this.zoomLevel);

        int viewWidth = viewEndX - viewStartX;
        int viewHeight = viewEndY - viewStartY;
        if (viewWidth <= 0 || viewHeight <= 0) return;

        float pixelsPerTileX = MINIMAP_PX / (float) viewWidth;
        float pixelsPerTileY = MINIMAP_PX / (float) viewHeight;

        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float mapCenterX = MARGIN + MINIMAP_PX / 2f;
        float mapCenterY = MARGIN + MINIMAP_PX / 2f;
        float mapRadius = MINIMAP_PX / 2f;

        // Draw circular dark background
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(BG_COLOR);
        shapes.circle(mapCenterX, mapCenterY, mapRadius);

        // Draw tiles
        final Tile[][] baseTiles = baseLayer.getBlocks();
        final Tile[][] collTiles = collisionLayer.getBlocks();

        for (int y = viewStartY; y < viewEndY; y++) {
            for (int x = viewStartX; x < viewEndX; x++) {
                if (!this.discovered[y][x]) continue;

                float screenX = MARGIN + (x - viewStartX) * pixelsPerTileX;
                float screenY = MARGIN + (y - viewStartY) * pixelsPerTileY;
                float tileCenterX = screenX + pixelsPerTileX / 2f;
                float tileCenterY = screenY + pixelsPerTileY / 2f;
                float dx = tileCenterX - mapCenterX;
                float dy = tileCenterY - mapCenterY;
                if (dx * dx + dy * dy > mapRadius * mapRadius) continue;

                Tile collTile = (y < collTiles.length && x < collTiles[y].length) ? collTiles[y][x] : null;
                Tile baseTile = (y < baseTiles.length && x < baseTiles[y].length) ? baseTiles[y][x] : null;

                if (collTile != null && !collTile.isVoid()) {
                    shapes.setColor(WALL_COLOR);
                } else if (baseTile != null && !baseTile.isVoid()) {
                    int variant = baseTile.getTileId() % 3;
                    if (variant == 0) shapes.setColor(FLOOR_COLOR_0);
                    else if (variant == 1) shapes.setColor(FLOOR_COLOR_1);
                    else shapes.setColor(FLOOR_COLOR_2);
                } else {
                    continue;
                }

                shapes.rect(screenX, screenY, pixelsPerTileX, pixelsPerTileY);
            }
        }

        // Draw player dot
        float playerScreenX = MARGIN + (centerTileX - viewStartX) * pixelsPerTileX;
        float playerScreenY = MARGIN + (centerTileY - viewStartY) * pixelsPerTileY;
        shapes.setColor(PLAYER_COLOR);
        shapes.circle(playerScreenX, playerScreenY, Math.max(3f, pixelsPerTileX));

        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
    }
}
