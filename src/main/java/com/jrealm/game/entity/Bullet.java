package com.jrealm.game.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;

import lombok.Data;


@Data
public class Bullet extends GameObject {
	private float angle;
	private float magnitude;
	private float range;
	private short damage;
	private boolean isEnemy;
	private boolean playerHit;

	public Bullet(int id, Sprite image, Vector2f origin, int size) {
		super(id, image, origin, size);
		// TODO Auto-generated constructor stub
	}

	public Bullet(int id, Sprite image, Vector2f origin, Vector2f dest, short size, float magnitude, float range,
			short damage,
			boolean isEnemy) {
		super(id, image, origin, size);
		this.magnitude = magnitude;
		this.range = range;
		this.damage = damage;
		this.angle = -Bullet.getAngle(origin, dest);
		this.isEnemy = isEnemy;

	}

	public Bullet(int id, Sprite image, Vector2f origin, float angle, short size, float magnitude, float range,
			short damage,
			boolean isEnemy) {
		super(id, image, origin, size);
		this.magnitude = magnitude;
		this.range = range;
		this.damage = damage;

		this.angle = -angle;
		this.isEnemy = isEnemy;

	}

	public static float getAngle(Vector2f source, Vector2f target) {
		double angle = (Math.atan2(target.y - source.y, target.x - source.x));

		angle -= Math.PI / 2;

		return (float) angle;
	}

	public boolean isEnemy() {
		return this.isEnemy;
	}

	public float getAngle() {
		return this.angle;
	}

	public float getMagnitude() {
		return this.magnitude;
	}

	public boolean remove() {
		return this.range <= 0.0;
	}

	public short getDamage() {
		return this.damage;
	}

	@Override
	public void update() {

		Vector2f vel = new Vector2f((float) (Math.sin(this.angle) * this.magnitude),
				(float) (Math.cos(this.angle) * this.magnitude));
		double dist = Math.sqrt((vel.x * vel.x) + (vel.y * vel.y));
		this.range -= dist;
		this.pos.addX(vel.x);
		this.pos.addY(vel.y);
	}

	@Override
	public void render(Graphics2D g) {
		AffineTransform original = g.getTransform();

		AffineTransform t = new AffineTransform();
		t.setToRotation(this.angle);
		// g.setTransform(t);
		g.setColor(Color.RED);
		g.fillOval((int) this.pos.getWorldVar().x, (int) this.pos.getWorldVar().y, this.size, this.size);

		g.setTransform(original);
	}

}
