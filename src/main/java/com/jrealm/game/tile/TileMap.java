package com.jrealm.game.tile;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;

import lombok.Data;

@Data
public class TileMap {
    private short mapId;
    private Tile[][] blocks;
    private int tileSize;
    private int width;
    private int height;

    public TileMap(int tileSize, int width, int height) {
        this.tileSize = tileSize;
        this.width = width;
        this.height = height;
        this.blocks = new Tile[height][width];
    }

    public TileMap(short mapId, int tileSize, int width, int height) {
        this.mapId = mapId;
        this.tileSize = tileSize;
        this.width = width;
        this.height = height;
        this.blocks = new Tile[height][width];
    }

    public TileMap(short mapId, Tile[][] blocks, int tileSize, int width, int height) {
        this.mapId = mapId;
        this.blocks = blocks;
        this.tileSize = tileSize;
        this.width = width;
        this.height = height;
    }

    public Tile[][] getBlocks() {
        return this.blocks;
    }

    public void setBlockAt(int row, int col, short tileId, TileData data) {
        Vector2f tilePos = new Vector2f(col * this.tileSize, row * this.tileSize);
        this.blocks[row][col] = new Tile(tileId, tilePos, data, (short) GlobalConstants.BASE_TILE_SIZE, false);
    }

    public Tile[] getBlocksInBounds(Rectangle cam) {
        int x = (int) ((cam.getPos().x) / this.getTileSize());
        int y = (int) ((cam.getPos().y) / this.getTileSize());
        List<Tile> results = new ArrayList<>();
        for (int i = x; i < (x + (cam.getWidth() / this.getTileSize())); i++) {
            for (int j = y; j < (y + (cam.getHeight() / this.getTileSize())); j++) {
                if (((i + (j * this.getHeight())) > -1) && ((i + (j * this.getHeight())) < this.getBlocks().length)
                        && (this.getBlocks()[i + (j * this.getHeight())] != null)) {
                    Tile toAdd = this.getBlocks()[i][j];
                    results.add(toAdd);
                }
            }
        }
        return results.toArray(new Tile[0]);
    }

    public void render(Graphics2D g, Rectangle cam) {
        for (Tile t : this.getBlocksInBounds(cam)) {
            t.render(g);
        }
    }
}
