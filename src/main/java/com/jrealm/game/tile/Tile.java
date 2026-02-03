package com.jrealm.game.tile;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Tile {
	private short tileId;
	private boolean discovered = false;
	private short size;
	private Vector2f pos;
	private TileData data;

	public Tile(short tileId, Vector2f pos, TileData data, short size, boolean discovered) {
		this.tileId = tileId;
		this.pos = pos;
		this.size = size;
		this.data = data;
		this.discovered = discovered;
	}

	public boolean update(Rectangle bounds) {
		return false;
	}

	public int getWidth() {
		return this.size;
	}

	public int getHeight() {
		return this.size;
	}

	public Vector2f getPos() {
		return this.pos;
	}

	public boolean isVoid() {
		return this.tileId == 0;
	}

	public void render(SpriteBatch batch) {
		TextureRegion region = GameSpriteManager.TILE_SPRITES.get((int) this.tileId);
		if (region != null) {
			batch.draw(region, this.pos.getWorldVar().x, this.pos.getWorldVar().y, this.size, this.size);
		}
	}
}
