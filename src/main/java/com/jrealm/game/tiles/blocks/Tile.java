package com.jrealm.game.tiles.blocks;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;

import lombok.Data;

@Data
public abstract class Tile {

	protected boolean discovered = false;

	protected int w;
	protected int h;

	public Sprite img;
	public Vector2f pos;

	public Tile(Sprite img, Vector2f pos, int w, int h) {
		this.img = img;
		this.pos = pos;
		this.w = w;
		this.h = h;
	}

	public int getWidth() { return this.w; }
	public int getHeight() { return this.h; }

	public abstract boolean update(AABB p);
	public abstract boolean isInside(AABB p);

	public abstract Sprite getImage();
	public Vector2f getPos() { return this.pos; }

	public void render(Graphics2D g) {
		g.drawImage(this.img.image, (int) this.pos.getWorldVar().x, (int) this.pos.getWorldVar().y, this.w, this.h, null);

	}
}
