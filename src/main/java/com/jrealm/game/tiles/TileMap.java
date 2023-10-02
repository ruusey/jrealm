package com.jrealm.game.tiles;

import java.awt.Graphics2D;

import com.jrealm.game.math.AABB;
import com.jrealm.game.tiles.blocks.Tile;

public abstract class TileMap {

	public abstract Tile[] getBlocks();
	public abstract void render(Graphics2D g, AABB cam);

	public abstract Tile[] getBlocksInBounds(AABB cam);
}
