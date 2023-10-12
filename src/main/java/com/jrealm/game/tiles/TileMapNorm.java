package com.jrealm.game.tiles;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.tiles.blocks.NormTile;
import com.jrealm.game.tiles.blocks.Tile;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
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

		NormTile[] block = new NormTile[100];

		int i = 0;
		for (int x = 5; x > -5; x--) {
			for (int y = 5; y > -5; y--) {
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

	public Tile[] getBlocksInBounds(AABB cam) {
		int x = (int) ((cam.getPos().x) / this.tileWidth);
		int y = (int) ((cam.getPos().y) / this.tileHeight);
		List<Tile> results = new ArrayList<>();
		for (int i = x; i < (x + (cam.getWidth() / this.tileWidth)); i++) {
			for (int j = y; j < (y + (cam.getHeight() / this.tileHeight)); j++) {
				if (((i + (j * TileMapObj.height)) > -1) && ((i + (j * TileMapObj.height)) < this.blocks.length)
						&& (this.blocks[i + (j * TileMapObj.height)] != null)) {
					Tile toAdd = this.blocks[i + (j * TileMapObj.height)];
					results.add(toAdd);
				}
			}
		}
		return results.toArray(new Tile[0]);
	}

	@Override
	public void render(Graphics2D g, AABB cam) {
		for (Tile t : this.getBlocksInBounds(cam)) {
			t.render(g);
		}
	}
}
