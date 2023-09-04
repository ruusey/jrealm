package com.jrealm.game.tiles;

import java.awt.Graphics2D;

import com.jrealm.game.math.AABB;
import com.jrealm.game.tiles.blocks.Block;

public abstract class TileMap {

    public abstract Block[] getBlocks();
    public abstract void render(Graphics2D g, AABB cam);
}
