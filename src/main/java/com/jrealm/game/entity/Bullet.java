package com.jrealm.game.entity;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.List;

import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;

import lombok.Data;


@Data
public class Bullet extends GameObject {
	private long bulletId;
	private float angle;
	private float magnitude;
	private float range;
	private short damage;
	private boolean isEnemy;
	private boolean playerHit;
	private boolean enemyHit;
	private float tfAngle = (float) (Math.PI / 2);

	private List<Short> flags;

	public boolean invert = false;

	public long timeStep = 0;
	public int amplitude = 4;
	public int increment = 25;

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

	public boolean hasFlag(short flag) {
		return (this.flags != null) && (this.flags.contains(flag));
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
		if (this.hasFlag((short) 12)) {
			this.update(1);
		} else {
			Vector2f vel = new Vector2f((float) (Math.sin(this.angle) * this.magnitude),
					(float) (Math.cos(this.angle) * this.magnitude));
			double dist = Math.sqrt((vel.x * vel.x) + (vel.y * vel.y));
			this.range -= dist;
			this.pos.addX(vel.x);
			this.pos.addY(vel.y);
		}

	}

	public void update(int i) {


		this.timeStep = (this.timeStep + this.increment) % 360;


		Vector2f vel = new Vector2f((float) (Math.sin(this.angle) * this.magnitude),
				(float) (Math.cos(this.angle) * this.magnitude));
		double dist = Math.sqrt((vel.x * vel.x) + (vel.y * vel.y));
		this.range -= dist;
		if (this.hasFlag((short) 13)) {
			double shift = -this.amplitude * Math.sin(Math.toRadians(this.timeStep));
			double shift2 = -this.amplitude * Math.cos(Math.toRadians(this.timeStep));
			this.pos.addX((float) (vel.x + shift2));
			this.pos.addY((float) (vel.y + shift));
		} else {
			double shift = this.amplitude * Math.sin(Math.toRadians(this.timeStep));
			double shift2 = this.amplitude * Math.cos(Math.toRadians(this.timeStep));
			this.pos.addX((float) (vel.x + shift2));
			this.pos.addY((float) (vel.y + shift));
		}




	}

	@Override
	public void render(Graphics2D g) {
		AffineTransform original = g.getTransform();
		AffineTransform t = new AffineTransform();
		if (this.image.getAngleOffset() > 0.0f) {
			t.rotate(-this.getAngle() + (this.tfAngle + this.image.getAngleOffset()),
					this.pos.getWorldVar().x + (this.size / 2), this.pos.getWorldVar().y + (this.size / 2));
		} else {
			t.rotate(-this.getAngle() + this.tfAngle, this.pos.getWorldVar().x + (this.size / 2),
					this.pos.getWorldVar().y + (this.size / 2));
		}

		g.setTransform(t);
		g.drawImage(this.image.image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y), this.size,
				this.size, null);

		g.setTransform(original);
	}

}
