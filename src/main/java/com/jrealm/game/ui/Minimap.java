package com.jrealm.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.state.PlayState;
import com.jrealm.game.tile.TileManager;
import com.jrealm.net.entity.NetTile;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Minimap {
    private static int MINIMAP_SIZE = 350;
    private static final int X_PADDING = 0;
    private static final int Y_PADDING = 0;

    private PlayState playState;
    private NetTile[][] currentBaseTiles;
    private NetTile[][] currentCollisionTiles;

    private Integer mapWidth;
    private Integer mapHeight;

    public Minimap(final PlayState playState) {
        this.playState = playState;
    }

    public boolean isInitialized() {
        return (this.currentBaseTiles != null) && (this.mapWidth != null) && (this.mapHeight != null);
    }

    public void initializeMap(final Integer mapId) {
        final MapModel mapModel = GameDataManager.MAPS.get(mapId);
        this.mapWidth = mapModel.getWidth();
        this.mapHeight = mapModel.getHeight();
        Minimap.MINIMAP_SIZE = 3 * mapModel.getWidth();
        this.currentBaseTiles = new NetTile[mapModel.getHeight()][mapModel.getWidth()];
        this.currentCollisionTiles = new NetTile[mapModel.getHeight()][mapModel.getWidth()];
    }

    public void update() {
        if (this.currentBaseTiles == null) {
            Minimap.log.warn("Minimap is not yet initialized, please call initializeMap()");
            return;
        }
        final TileManager tm = this.playState.getRealmManager().getRealm().getTileManager();
        final Player currentPlayer = this.playState.getPlayer();
        final NetTile[] newTiles = tm.getLoadMapTiles(currentPlayer);
        for (final NetTile tile : newTiles) {
            if (tile.getLayer() == (byte) 0) {
                this.currentBaseTiles[tile.getYIndex()][tile.getXIndex()] = tile;
            } else {
                this.currentCollisionTiles[tile.getYIndex()][tile.getXIndex()] = tile;
            }
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes) {
        // Background
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.GRAY);
        shapes.rect(0, 0, Minimap.MINIMAP_SIZE, Minimap.MINIMAP_SIZE);
        shapes.end();
        batch.begin();

        final int tileSize = Minimap.MINIMAP_SIZE / this.getMapWidth();

        for (NetTile[] row : this.currentBaseTiles) {
            if (row == null) continue;
            for (NetTile col : row) {
                if (col == null) continue;
                TextureRegion region = GameSpriteManager.TILE_SPRITES.get((int) col.getTileId());
                if (region != null) {
                    batch.draw(region, Minimap.X_PADDING + (col.getYIndex() * tileSize),
                            Minimap.Y_PADDING + (col.getXIndex() * tileSize), tileSize, tileSize);
                }
            }
        }

        for (NetTile[] row : this.currentCollisionTiles) {
            if (row == null) continue;
            for (NetTile col : row) {
                if ((col == null) || (col.getTileId() == (short) 0)) continue;
                TextureRegion region = GameSpriteManager.TILE_SPRITES.get((int) col.getTileId());
                if (region != null) {
                    batch.draw(region, Minimap.X_PADDING + (col.getYIndex() * tileSize),
                            Minimap.Y_PADDING + (col.getXIndex() * tileSize), tileSize, tileSize);
                }
            }
        }

        // Player dot
        final Vector2f playerPos = this.playState.getPlayer().getPos().clone();
        final TileManager tm = this.playState.getRealmManager().getRealm().getTileManager();
        int mw = tm.getBaseLayer().getWidth() * tm.getBaseLayer().getTileSize();
        int mh = tm.getBaseLayer().getHeight() * tm.getBaseLayer().getTileSize();

        float xRatio = playerPos.x / (float) mw;
        float yRatio = playerPos.y / (float) mh;

        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.RED);
        shapes.circle(Minimap.X_PADDING + xRatio * Minimap.MINIMAP_SIZE,
                Minimap.Y_PADDING + yRatio * Minimap.MINIMAP_SIZE, tileSize / 2f);
        shapes.end();
        batch.begin();
    }
}
