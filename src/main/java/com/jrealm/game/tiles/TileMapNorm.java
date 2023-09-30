package com.jrealm.game.tiles;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.tiles.blocks.NormTile;
import com.jrealm.game.tiles.blocks.Tile;

public class TileMapNorm extends TileMap {

	public Tile[] blocks;

	private int tileWidth;
	private int tileHeight;

	private int height;

	public TileMapNorm(String data, SpriteSheet sprite, int width, int height, int tileWidth, int tileHeight, int tileColumns) {
		this.blocks = new Tile[width * height];

		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;

		this.height = height;

		String[] block = data.split(",");

		for(int i = 0; i < (width * height); i++) {
			int temp = Integer.parseInt(block[i].replaceAll("\\s+",""));
			if(temp != 0) {
				this.blocks[i] = new NormTile(sprite.getNewSprite((int) ((temp - 1) % tileColumns), (int) ((temp - 1) / tileColumns) ), new Vector2f((int) (i % width) * tileWidth, (int) (i / height) * tileHeight), tileWidth, tileHeight);
			}
		}
	}

	public synchronized NormTile[] getNormalTile(int id) {
		int normMap = 0;
		// if (TileManager.tm.size() < 2) {
		// normMap = 0;
		// }
		NormTile[] block = new NormTile[64];

		int i = 0;
		for (int x = 4; x > -4; x--) {
			for (int y = 4; y > -4; y--) {
				if (((id + (y + (x * this.tileHeight))) < 0)
						|| ((id + (y + (x * this.tileHeight))) > ((this.tileWidth * this.tileHeight) - 2))) {
					continue;
				}

				block[i] = (NormTile) this.getBlocks()[id + (y + (x * this.tileHeight))];
				i++;

			}
		}

		return block;
	}

	@Override
	public Tile[] getBlocks() { return this.blocks; }

	@Override
	public void render(Graphics2D g, AABB cam) {
		int x = (int) ((cam.getPos().x) / this.tileWidth);
		int y = (int) ((cam.getPos().y) / this.tileHeight);
		int blocksRendered = 0;
		for(int i = x; i < (x + (cam.getWidth() / this.tileWidth)); i++) {
			for(int j = y; j < (y + (cam.getHeight() / this.tileHeight)); j++) {
				if(((i + (j * this.height)) > -1) && ((i + (j * this.height)) < this.blocks.length) && (this.blocks[i + (j * this.height)] != null)) {
					this.blocks[i + (j * this.height)].render(g);
					blocksRendered++;
				}
			}
		}
	}
}
