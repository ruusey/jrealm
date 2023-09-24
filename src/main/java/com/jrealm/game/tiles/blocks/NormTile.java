package com.jrealm.game.tiles.blocks;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;

import lombok.Data;

@Data

public class NormTile extends Tile {

	public NormTile(Sprite img, Vector2f pos, int w, int h) {
		super(img, pos, w, h);

		img.setEffect(Sprite.effect.DECAY);
	}

	@Override
	public boolean update(AABB p) {
		return false;
	}

	@Override
	public Sprite getImage() {
		return this.img;
	}

	@Override
	public boolean isInside(AABB p) {
		return false;
	}

	@Override
	public void render(Graphics2D g) {
		super.render(g);
	}

	@Override
	public String toString() {
		return "position: " + this.pos;
	}
}
