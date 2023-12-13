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

	public boolean normalTile(TileMapObj tileMap, float ax, float ay) {
		int xt;
		int yt;

		xt = (int) ((this.e.getPos().x + ax) + this.e.getBounds().getXOffset()) / GlobalConstants.BASE_SIZE;
		yt = (int) ((this.e.getPos().y + ay) + this.e.getBounds().getYOffset()) / GlobalConstants.BASE_SIZE;
		this.tileId = (xt + (yt * tileMap.getHeight()));

		if (this.tileId > (tileMap.getHeight() * tileMap.getWidth())) {
			this.tileId = (tileMap.getHeight() * tileMap.getWidth()) - 2;
		}

		return false;
	}

	public boolean collisionTile(TileMapObj tileMap, Tile[] tiles, float ax, float ay) {
		if (tiles != null) {
			int xt;
			int yt;
			for (int c = 0; c < 4; c++) {

				xt = (int) ((this.e.getPos().x + ax) + ((c % 2) * this.e.getBounds().getWidth())
						+ this.e.getBounds().getXOffset()) / GlobalConstants.BASE_SIZE;
				yt = (int) ((this.e.getPos().y + ay) + ((c / 2) * this.e.getBounds().getHeight())
						+ this.e.getBounds().getYOffset()) / GlobalConstants.BASE_SIZE;

				if ((xt <= 0) || (yt <= 0) || ((xt + (yt * tileMap.getHeight())) < 0)
						|| ((xt + (yt * tileMap.getHeight())) > ((tileMap.getHeight() * tileMap.getWidth()) - 2)))
					return true;

				if (tiles[xt + (yt * tileMap.getHeight())] instanceof Tile) {
					Tile block = tiles[xt + (yt * tileMap.getHeight())];

					return block.update(this.e.getBounds());
				}
			}
		}
		return false;
	}

	public int getTile() {
		return this.tileId;
	}
}
