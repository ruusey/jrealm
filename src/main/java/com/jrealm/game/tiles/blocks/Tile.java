package com.jrealm.game.tiles.blocks;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;

import lombok.Data;

@Data
public abstract class Tile {
	private short tileId;
	private boolean discovered = false;
	private short size;
	private Vector2f pos;
	private TileData data;

	public Sprite img;

	public Tile(short tileId, Sprite img, Vector2f pos, short size) {
		this.tileId = tileId;
		this.img = img;
		this.pos = pos;
		this.size = size;
	}
	
	public Tile(short tileId, Vector2f pos, short size) {
		this.tileId = tileId;
		this.pos = pos;
		this.size = size;
	}

	public int getWidth() { return this.size; }
	public int getHeight() { return this.size; }

	public abstract boolean update(AABB p);
	public abstract boolean isInside(AABB p);

	public abstract Sprite getImage();
	public Vector2f getPos() { return this.pos; }

	public void render(Graphics2D g) {
		g.drawImage(this.img.image, (int) this.pos.getWorldVar().x, (int) this.pos.getWorldVar().y, this.size, this.size, null);

	}
}
