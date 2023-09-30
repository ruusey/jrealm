package com.jrealm.game.tiles;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.tiles.blocks.ObjTile;
import com.jrealm.game.tiles.blocks.Tile;

public class TileMapObj extends TileMap {

	public Tile[] blocks;

	private int tileWidth;
	private int tileHeight;

	public static int width;
	public static int height;

	public TileMapObj(String data, SpriteSheet sprite, int width, int height, int tileWidth, int tileHeight, int tileColumns) {
		this.blocks = new Tile[width * height];

		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;

		TileMapObj.width = width;
		TileMapObj.height = height;

		String[] block = data.split(",");
		for (int i = 0; i < (width * height); i++) {
			int temp = Integer.parseInt(block[i].replaceAll("\\s+", ""));
			if (temp != 0) {
				this.blocks[i] = new ObjTile(
						sprite.getNewSprite((int) ((temp - 1) % tileColumns), (int) ((temp - 1) / tileColumns)),
						new Vector2f((int) (i % width) * tileWidth, (int) (i / height) * tileHeight), tileWidth,
						tileHeight);
			}
		}
	}

	@Override
	public Tile[] getBlocks() {
		return this.blocks;
	}

	@Override
	public void render(Graphics2D g, AABB cam) {
		int x = (int) ((cam.getPos().x) / this.tileWidth);
		int y = (int) ((cam.getPos().y) / this.tileHeight);

		for(int i = x; i < (x + (cam.getWidth() / this.tileWidth)); i++) {
			for(int j = y; j < (y + (cam.getHeight() / this.tileHeight)); j++) {
				if (((i + (j * TileMapObj.height)) > -1) && ((i + (j * TileMapObj.height)) < this.blocks.length)
						&& (this.blocks[i + (j * TileMapObj.height)] != null)) {
					this.blocks[i + (j * TileMapObj.height)].render(g);
				}
			}
		}
	}
}
