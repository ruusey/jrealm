package com.jrealm.game.entity;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.util.TileCollision;

import lombok.Data;

@Data
public abstract class GameObject {
	protected int id;
	protected SpriteSheet sprite;
	protected Sprite image;
	protected AABB bounds;
	protected Vector2f pos;
	protected int size;
	protected int spriteX;
	protected int spriteY;

	// used for moving objects like boxes and such
	protected float dx;
	protected float dy;

	protected float maxSpeed = 4f;
	protected float acc = 2f;
	protected float deacc = 1.0f;
	protected float force = 25f;

	protected boolean teleported = false;
	protected TileCollision tc;
	protected String name = "";

	public boolean discovered;
	public GameObject(int id, SpriteSheet sprite, Vector2f origin, int spriteX, int spriteY, int size) {
		this(id, origin, size);
		this.sprite = sprite;
	}

	public GameObject(int id, Sprite image, Vector2f origin, int size) {
		this(id, origin, size);
		this.image = image;
	}

	private GameObject(int id, Vector2f origin, int size) {
		this.id = id;
		this.bounds = new AABB(origin, size, size);
		this.pos = origin;
		this.size = size;
	}

	public void setPos(Vector2f pos) {
		this.pos = pos;
		// pos.clone(this.size / 2, this.size / 2)
		//pos.addX(this.size / 2)
		this.bounds = new AABB(pos, this.size, this.size);
		this.teleported = true;
	}


	public void addForce(float a, boolean vertical) {
		if(!vertical) {
			this.dx -= a;
		} else {
			this.dy -= a;
		}
	}

	public void update() {

	}

	@Override
	public Vector2f clone() {
		Vector2f newVector = new Vector2f(this.pos.x, this.pos.y);
		return newVector;
	}

	public void render(Graphics2D g) {
		// Top Left -> Top Right
		g.drawImage(this.image.image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y), this.size, this.size, null);
	}

	@Override
	public String toString() {
		return "$" + this.name;
	}

}