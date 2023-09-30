package com.jrealm.game.util;

import com.jrealm.game.entity.Entity;
import com.jrealm.game.tiles.TileMapObj;
import com.jrealm.game.tiles.blocks.HoleBlock;
import com.jrealm.game.tiles.blocks.Tile;

public class TileCollision {

	private Entity e;
	private int tileId;

	public TileCollision(Entity e) {
		this.e = e;
	}

	public boolean normalTile(float ax, float ay) {
		int xt;
		int yt;

		xt = (int) ( (this.e.getPos().x + ax) + this.e.getBounds().getXOffset()) / 64;
		yt = (int) ( (this.e.getPos().y + ay) + this.e.getBounds().getYOffset()) / 64;
		this.tileId = (xt + (yt * TileMapObj.height));

		if(this.tileId > (TileMapObj.height * TileMapObj.width)) {
			this.tileId = (TileMapObj.height * TileMapObj.width) - 2;
		}

		return false;
	}

	public boolean collisionTile(Tile[] tiles, float ax, float ay) {
		if(tiles != null) {
			int xt;
			int yt;

			for(int c = 0; c < 4; c++) {

				xt = (int) ( (this.e.getPos().x + ax) + ((c % 2) * this.e.getBounds().getWidth()) + this.e.getBounds().getXOffset()) / 64;
				yt = (int) ( (this.e.getPos().y + ay) + ((c / 2) * this.e.getBounds().getHeight()) + this.e.getBounds().getYOffset()) / 64;

				if((xt <= 0) || (yt <= 0) || ((xt + (yt * TileMapObj.height)) < 0) || ((xt + (yt * TileMapObj.height)) > ((TileMapObj.height * TileMapObj.width) - 2)))
					return true;

				if(tiles[xt + (yt * TileMapObj.height)] instanceof Tile) {
					Tile block = tiles[xt + (yt * TileMapObj.height)];
					if(block instanceof HoleBlock)
						return this.collisionHole(tiles, ax, ay, xt, yt, block);
					return block.update(this.e.getBounds());
				}
			}
		}

		return false;
	}

	public int getTile() { return this.tileId; }

	private boolean collisionHole(Tile[] tiles, float ax, float ay, float xt, float yt, Tile block) {
		int nextXt = (int) ((( (this.e.getPos().x + ax) + this.e.getBounds().getXOffset()) / 64) + (this.e.getBounds().getWidth() / 64));
		int nextYt = (int) ((( (this.e.getPos().y + ay) + this.e.getBounds().getYOffset()) / 64) + (this.e.getBounds().getHeight() / 64));

		if(block.isInside(this.e.getBounds())) {
			this.e.setFallen(true);
			return false;
		}
		if((nextXt == (yt + 1)) || (nextXt == (xt + 1)) || (nextYt == (yt - 1)) || (nextXt == (xt - 1))) {
			if (tiles[nextXt + (nextYt * TileMapObj.height)] instanceof HoleBlock) {
				Tile nextblock = tiles[nextXt + (nextYt * TileMapObj.height)];

				if(((this.e.getPos().x + this.e.getBounds().getXOffset()) > block.getPos().x)
						&& ((this.e.getPos().y + this.e.getBounds().getYOffset()) > block.getPos().y)
						&& ((nextblock.getWidth() + nextblock.getPos().x) > (this.e.getBounds().getWidth() + (this.e.getPos().x + this.e.getBounds().getXOffset())))
						&& ((nextblock.getHeight() + nextblock.getPos().y) > (this.e.getBounds().getHeight() + (this.e.getPos().y + this.e.getBounds().getYOffset())))) {
					this.e.setFallen(true);
				}
				return false;
			}
		}

		this.e.setFallen(false);
		return false;
	}
}
