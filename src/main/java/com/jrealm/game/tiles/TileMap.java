package com.jrealm.game.tiles;

import java.awt.Graphics2D;

import com.jrealm.game.math.AABB;
import com.jrealm.game.tiles.blocks.Tile;

import lombok.Data;

@Data
public class TileMap {
	private short mapId;
	private Tile[] blocks;
	private int tileSize;
	private int width;
	private int height;

	public TileMap(short mapId, Tile[] blocks, int tileSize, int width, int height) {
		super();
		this.mapId = mapId;
		this.blocks = blocks;
		this.tileSize = tileSize;
		this.width = width;
		this.height = height;
	}

	public Tile[] getBlocks() {
		return null;
	}

	public void render(Graphics2D g, AABB cam) {

	}

	public Tile[] getBlocksInBounds(AABB cam) {
		return null;
	}

}
