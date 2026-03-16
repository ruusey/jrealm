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
	// Row and column in the tile grid — position computed as (col*size, row*size)
	// Eliminates per-tile Vector2f allocation (saves ~8MB on 512x512 maps)
	private short row;
	private short col;
	private TileData data;

	// Shared no-collision TileData instance to avoid per-tile allocation
	private static final TileData SHARED_NO_COLLISION = TileData.withoutCollision();
	private static final TileData SHARED_COLLISION = TileData.withCollision();

	public Tile(short tileId, Vector2f pos, TileData data, short size, boolean discovered) {
		this.tileId = tileId;
		this.size = size;
		this.data = data;
		this.discovered = discovered;
		// Derive row/col from position
		this.col = (short) (pos.x / size);
		this.row = (short) (pos.y / size);
	}

	public Tile(short tileId, short row, short col, TileData data, short size) {
		this.tileId = tileId;
		this.row = row;
		this.col = col;
		this.size = size;
		this.data = data;
	}

	public static TileData sharedData(boolean collision) {
		return collision ? SHARED_COLLISION : SHARED_NO_COLLISION;
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

	/** Returns a computed position. Use sparingly — prefer getWorldX()/getWorldY() for rendering. */
	public Vector2f getPos() {
		return new Vector2f(this.col * this.size, this.row * this.size);
	}

	public float getWorldX() {
		return this.col * this.size;
	}

	public float getWorldY() {
		return this.row * this.size;
	}

	public boolean isVoid() {
		return this.tileId == 0;
	}

	public void render(SpriteBatch batch) {
		TextureRegion region = GameSpriteManager.TILE_SPRITES.get((int) this.tileId);
		if (region != null) {
			float wx = (this.col * this.size) - Vector2f.worldX;
			float wy = (this.row * this.size) - Vector2f.worldY;
			batch.draw(region, wx, wy, this.size, this.size);
		}
	}
}
