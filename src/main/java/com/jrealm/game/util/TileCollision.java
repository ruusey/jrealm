package com.jrealm.game.util;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.tiles.TileMapObj;
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

		xt = (int) ((this.e.getPos().x + ax) + this.e.getBounds().getXOffset()) / GlobalConstants.BASE_SIZE;
		yt = (int) ((this.e.getPos().y + ay) + this.e.getBounds().getYOffset()) / GlobalConstants.BASE_SIZE;
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

				xt = (int) ((this.e.getPos().x + ax) + ((c % 2) * this.e.getBounds().getWidth())
						+ this.e.getBounds().getXOffset()) / GlobalConstants.BASE_SIZE;
				yt = (int) ((this.e.getPos().y + ay) + ((c / 2) * this.e.getBounds().getHeight())
						+ this.e.getBounds().getYOffset()) / GlobalConstants.BASE_SIZE;

				if((xt <= 0) || (yt <= 0) || ((xt + (yt * TileMapObj.height)) < 0) || ((xt + (yt * TileMapObj.height)) > ((TileMapObj.height * TileMapObj.width) - 2)))
					return true;

				if(tiles[xt + (yt * TileMapObj.height)] instanceof Tile) {
					Tile block = tiles[xt + (yt * TileMapObj.height)];

					return block.update(this.e.getBounds());
				}
			}
		}
		return false;
	}
	public int getTile() { return this.tileId; }	
}
