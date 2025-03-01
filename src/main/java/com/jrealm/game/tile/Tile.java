package com.jrealm.game.tile;

import java.awt.Graphics2D;
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

	public void render(Graphics2D g) {
		g.drawImage(GameSpriteManager.TILE_SPRITES.get((int) this.tileId), (int) this.pos.getWorldVar().x,
				(int) this.pos.getWorldVar().y, this.size, this.size, null);
	}
}
