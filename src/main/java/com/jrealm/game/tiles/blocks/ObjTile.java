package com.jrealm.game.tiles.blocks;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class ObjTile extends Tile {

	public ObjTile(short tileId, Sprite img, Vector2f pos, short size) {
		super(tileId, img, pos, size);
	}

	@Override
	public boolean update(AABB p) {
		return true;
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
	public void render(Graphics2D g){
		super.render(g);
	}
}
