package com.jrealm.game.ui;

import java.awt.Color;
import java.awt.Graphics2D;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.state.PlayState;
import com.jrealm.game.tile.NetTile;
import com.jrealm.game.tile.TileManager;

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
        TileManager tm = this.playState.getRealmManager().getRealm().getTileManager();
        Player currentPlayer = this.playState.getPlayer();
        NetTile[] newTiles = tm.getLoadMapTiles(currentPlayer);
        for (NetTile tile : newTiles) {
            if (tile.getLayer() == (byte) 0) {
                this.currentBaseTiles[tile.getYIndex()][tile.getXIndex()] = tile;
            } else {
                this.currentCollisionTiles[tile.getYIndex()][tile.getXIndex()] = tile;
            }
        }
    }

    public void render(Graphics2D g) {
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, Minimap.MINIMAP_SIZE, Minimap.MINIMAP_SIZE);
        final int tileSize = Minimap.MINIMAP_SIZE / this.getMapWidth();

        for (NetTile[] row : this.currentBaseTiles) {
            if (row == null) {
                continue;
            }
            for (NetTile col : row) {
                if (col == null) {
                    continue;
                }
                g.drawImage(GameSpriteManager.TILE_SPRITES.get((int) col.getTileId()),
                        Minimap.X_PADDING + (col.getYIndex() * tileSize),
                        Minimap.Y_PADDING + (col.getXIndex() * tileSize), tileSize, tileSize, null);
            }
        }

        for (NetTile[] row : this.currentCollisionTiles) {
            if (row == null) {
                continue;
            }
            for (NetTile col : row) {
                if ((col == null) || (col.getTileId() == (short) 0)) {
                    continue;
                }
                g.drawImage(GameSpriteManager.TILE_SPRITES.get((int) col.getTileId()),
                        Minimap.X_PADDING + (col.getYIndex() * tileSize),
                        Minimap.Y_PADDING + (col.getXIndex() * tileSize), tileSize, tileSize, null);
            }
        }
        final Vector2f playerPos = this.playState.getPlayer().getPos().clone();
        final TileManager tm = this.playState.getRealmManager().getRealm().getTileManager();
        int mapWidth = tm.getBaseLayer().getWidth() * tm.getBaseLayer().getTileSize();
        int mapHeight = tm.getBaseLayer().getHeight() * tm.getBaseLayer().getTileSize();

        float xRatio = playerPos.x / (float) mapWidth;
        float yRatio = playerPos.y / (float) mapHeight;

        Vector2f projectedPos = new Vector2f(xRatio * (+Minimap.MINIMAP_SIZE), yRatio * (Minimap.MINIMAP_SIZE));

        g.setColor(Color.RED);

        g.fillOval((int) (Minimap.X_PADDING + projectedPos.x), (int) (Minimap.Y_PADDING + projectedPos.y), tileSize,
                tileSize);

        // g.drawString(playerPos.toString(), Minimap.MINIMAP_SIZE - 42,
        // Minimap.MINIMAP_SIZE - 32);

    }
}
