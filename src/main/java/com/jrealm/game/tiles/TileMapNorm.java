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
public class TileMapNorm extends TileMap {

	public TileMapNorm(String data, SpriteSheet sprite, int width, int height, int tileSize) {
		super((short)1, new Tile[width * height], tileSize, width, height );
		String[] block = data.split(",");

		for(int i = 0; i < (width * height); i++) {
			int temp = Integer.parseInt(block[i].replaceAll("\\s+",""));
			if(temp != 0) {
				short tileId = (short) 0;
				boolean discovered = false;
				short size = (short)tileSize;
				Vector2f pos = new Vector2f((int) (i % width) * tileSize, (int) (i / height) * tileSize);
				Sprite image =  sprite.getNewSprite((int) ((temp - 1) % this.getWidth()), (int) ((temp - 1) / this.getWidth()) );
				this.getBlocks()[i] = new Tile(tileId, image, pos, TileData.withoutCollision(), size, discovered);
			}
		}
	}

	public synchronized Tile[] getNormalTile(int id) {
		final Tile[] block = new Tile[100];
		int i = 0;
		for (int x = 5; x > -5; x--) {
			for (int y = 5; y > -5; y--) {
				if (((id + (y + (x * this.getTileSize()))) < 0)
						|| ((id + (y + (x * this.getTileSize()))) > ((this.getTileSize() * this.getWidth()) - 2))) {
					continue;
				}

				block[i] = (Tile) this.getBlocks()[id + (y + (x * this.getTileSize()))];
				i++;
			}
		}
		return block;
	}

	@Override
	public Tile[] getBlocks() { return this.getBlocks(); }

	public Tile[] getBlocksInBounds(AABB cam) {
		int x = (int) ((cam.getPos().x) / this.getTileSize());
		int y = (int) ((cam.getPos().y) / this.getTileSize());
		List<Tile> results = new ArrayList<>();
		for (int i = x; i < (x + (cam.getWidth() / this.getTileSize())); i++) {
			for (int j = y; j < (y + (cam.getHeight() / this.getTileSize())); j++) {
				if (((i + (j * this.getHeight())) > -1) && ((i + (j * this.getHeight())) < this.getBlocks().length)
						&& (this.getBlocks()[i + (j * this.getHeight())] != null)) {
					Tile toAdd = this.getBlocks()[i + (j * this.getHeight())];
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
