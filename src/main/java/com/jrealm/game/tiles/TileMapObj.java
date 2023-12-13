package com.jrealm.game.tiles;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.tiles.blocks.Tile;
import com.jrealm.game.tiles.blocks.TileData;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper=false)
public class TileMapObj extends TileMap {
	private Tile[] blocks;
	private int tileSize;
	private int width;
	private int height;

	public TileMapObj(String data, SpriteSheet sprite, int width, int height, int tileSize) {
		this.blocks = new Tile[width * height];

		this.tileSize = tileSize;

		this.width = width;
		this.height = height;

		String[] block = data.split(",");
		for (int i = 0; i < (width * height); i++) {
			int temp = Integer.parseInt(block[i].replaceAll("\\s+", ""));
			if (temp != 0) {
				short tileId = (short) 0;
				boolean discovered = false;
				short size = (short)tileSize;
				Vector2f pos = new Vector2f((int) (i % width) * tileSize, (int) (i / height) * tileSize);
				Sprite image =  sprite.getNewSprite((int) ((temp - 1) % this.width), (int) ((temp - 1) / this.width) );
				this.blocks[i] = new Tile(tileId, image, pos, TileData.withCollision(), size, discovered);
			}
		}
	}

	public Tile[] getBlocksInBounds(AABB cam) {
		int x = (int) ((cam.getPos().x) / this.tileSize);
		int y = (int) ((cam.getPos().y) / this.tileSize);
		List<Tile> results = new ArrayList<>();
		for (int i = x; i < (x + (cam.getWidth() / this.tileSize)); i++) {
			for (int j = y; j < (y + (cam.getHeight() / this.tileSize)); j++) {
				if (((i + (j * this.height)) > -1) && ((i + (j * this.height)) < this.blocks.length)
						&& (this.blocks[i + (j * this.height)] != null)) {
					Tile toAdd = this.blocks[i + (j * this.height)];
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
