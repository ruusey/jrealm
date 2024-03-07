package com.jrealm.game.entity;

import java.awt.Graphics2D;
import java.time.Instant;

import com.jrealm.game.contants.EffectType;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class Entity extends GameObject {
	public int RIGHT = 0;
	public int UP = 1;
	public int LEFT = 2;
	public int DOWN = 3;
	public int ATTACK = 4;
	public int IDLE = 5;

	protected int currentDirection = this.RIGHT;

	protected boolean up = false;
	protected boolean down = false;
	protected boolean right = false;
	protected boolean left = false;
	protected boolean attack = false;
	private boolean fallen = false;

	public boolean xCol = false;
	public boolean yCol = false;

	protected boolean die = false;

	protected int attackSpeed = 1050; // in milliseconds
	protected int attackDuration = 650; // in milliseconds
	protected double attacktime;
	protected boolean canAttack = true;
	protected boolean attacking = false;

	protected int health = 100;
	protected int mana = 100;
	protected float healthpercent = 1;
	protected float manapercent = 1;

	protected Rectangle hitBounds;

	private short[] effectIds;
	private long[] effectTimes;


	public Entity(long id, Vector2f origin, int size) {
		super(id, origin, size);

		this.hitBounds = new Rectangle(origin, size, size);
		this.resetEffects();
	}

	public void removeEffect(short effectId) {
		for (int i = 0; i < this.effectIds.length; i++) {
			if (this.effectIds[i] == effectId) {
				this.effectIds[i] = -1;
				this.effectTimes[i] = -1;
			}
		}
	}

	public void removeExpiredEffects() {
		for (int i = 0; i < this.effectIds.length; i++) {
			if (this.effectIds[i] != -1) {
				if (Instant.now().toEpochMilli() > this.effectTimes[i]) {
					this.effectIds[i] = -1;
					this.effectTimes[i] = -1;
				}
			}
		}
	}

	public boolean hasEffect(EffectType effect) {
		if (this.effectIds == null)
			return false;
		for (int i = 0; i < this.effectIds.length; i++) {
			if (this.effectIds[i] == effect.effectId)
				return true;
		}
		return false;
	}

	public boolean hasNoEffects() {
		for (int i = 0; i < this.effectIds.length; i++) {
			if (this.effectIds[i] > -1)
				return false;
		}
		return true;
	}

	public void resetEffects() {
		this.effectIds = new short[] { -1, -1, -1, -1, -1, -1, -1, -1 };
		this.effectTimes = new long[] { -1l, -1l, -1l, -1l, -1l, -1l, -1l, -1l };
	}

	public void addEffect(EffectType effect, long duration) {
		for (int i = 0; i < this.effectIds.length; i++) {
			if (this.effectIds[i] == -1) {
				this.effectIds[i] = effect.effectId;
				this.effectTimes[i] = (Instant.now().toEpochMilli() + duration);
				return;
			}
		}
	}

	public void setFallen(boolean b) {
		this.fallen = b;
	}

	public boolean getDeath() {
		return this.health <= 0;
	}

	public int getHealth() {
		return this.health;
	}

	public Rectangle getHitBounds() {
		return this.hitBounds;
	}
	public int getDirection() {
		if ((this.currentDirection == this.UP) || (this.currentDirection == this.LEFT))
			return 1;
		return -1;
	}

	public void move() {
		if (this.hasEffect(EffectType.PARALYZED)) {
			this.up = false;
			this.down = false;
			this.right = false;
			this.left = false;
			return;
		}
		if (this.up) {
			this.currentDirection = this.UP;
		} 
		if (this.down) {
			this.currentDirection = this.DOWN;
		}
		if (this.left) {
			this.currentDirection = this.LEFT;
		}
		if (this.right) {
			this.currentDirection = this.RIGHT;
		}
	}

	public void update(double time) {
		if (this.getSpriteSheet() != null) {
			this.getSpriteSheet().animate();
		}
	}

	public void updateAnimation() {
		if(this.dx>0) {
			this.right=true;
		}else if(this.dx<0) {
			this.left=true;
		}else {
			this.right=false;
			this.left=false;
		}

		if(this.dy>0) {
			this.down=true;
		}else if(this.dy<0) {
			this.up=true;
		}else {
			this.down=false;
			this.up=false;
		}
	}

	@Override
	public abstract void render(Graphics2D g);

	public boolean isFallen() {
		return this.fallen;
	}

	public void setUp(boolean up) {
		this.up = up;
	}

	public void setDown(boolean down) {
		this.down = down;
	}

	public void setRight(boolean right) {
		this.right = right;
	}

	public void setLeft(boolean left) {
		this.left = left;
	}

	public boolean isUp() {
		return this.up;
	}

	public boolean isDown() {
		return this.down;
	}

	public boolean isRight() {
		return this.right;
	}

	public boolean isLeft() {
		return this.left;
	}
}
