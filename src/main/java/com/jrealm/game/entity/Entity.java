package com.jrealm.game.entity;

import java.awt.Graphics2D;

import com.jrealm.game.contants.EffectType;
import com.jrealm.game.graphics.Animation;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class Entity extends GameObject {

	public int IDLE = 6;
	public int ATTACK = 5;
	public int FALLEN = 4;
	public int UP = 2;
	public int DOWN = 1;
	public int LEFT = 3;
	public int RIGHT = 0;

	protected boolean hasIdle = false;

	protected int currentAnimation;
	protected int currentDirection = this.RIGHT;

	public boolean useRight = false;

	protected Animation ani;
	protected int hitsize;

	protected boolean up = false;
	protected boolean down = false;
	protected boolean right = false;
	protected boolean left = false;
	protected boolean attack = false;
	private boolean fallen = false;

	public boolean xCol = false;
	public boolean yCol = false;

	protected int invincible = 500;
	protected double invincibletime;
	protected boolean isInvincible = false;
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

	protected AABB hitBounds;

	private short[] effectIds;
	private long[] effectTimes;

	public Entity(long id, SpriteSheet sprite, Vector2f origin, int size) {
		super(id, sprite, origin, 0, 0, size);
		this.hitsize = size;

		this.hitBounds = new AABB(origin, size, size);
		// this.hitBounds.setXOffset(size / 2);

		this.ani = new Animation();
		this.setAnimation(this.RIGHT, sprite.getSpriteArray(this.RIGHT), 10);

		this.resetEffects();
	}

	public Entity(long id, Vector2f origin, int size) {
		super(id, origin, size);
		this.hitsize = size;

		this.hitBounds = new AABB(origin, size, size);
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
				if (System.currentTimeMillis() > this.effectTimes[i]) {
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
				this.effectTimes[i] = System.currentTimeMillis() + duration;
				return;
			}
		}
	}

	public void setIsInvincible(boolean invincible) {
		this.isInvincible = invincible;
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

	public AABB getHitBounds() {
		return this.hitBounds;
	}
	public int getDirection() {
		if ((this.currentDirection == this.UP) || (this.currentDirection == this.LEFT))
			return 1;
		return -1;
	}

	public Animation getAnimation() {
		return this.ani;
	}

	public void setAnimation(int i, Sprite[] frames, int delay) {
		this.currentAnimation = i;
		this.ani.setFrames(i, frames);
		this.ani.setDelay(delay);
	}

	public void animate() {

		if (this.attacking) {
			if (this.currentAnimation < 5) {
				this.setAnimation(this.currentAnimation + this.ATTACK,
						this.sprite.getSpriteArray(this.currentAnimation + this.ATTACK), this.attackDuration / 100);
			}
		} else if (this.up) {
			if (((this.currentAnimation != this.UP) || (this.ani.getDelay() == -1))) {
				this.setAnimation(this.UP, this.sprite.getSpriteArray(this.UP + this.sprite.getRowOffset(), 1), 5);
			}
		} else if (this.down) {
			if (((this.currentAnimation != this.DOWN) || (this.ani.getDelay() == -1))) {
				this.setAnimation(this.DOWN, this.sprite.getSpriteArray(this.DOWN + this.sprite.getRowOffset(), 1), 5);
			}
		} else if (this.left) {
			if (((this.currentAnimation != this.LEFT) || (this.ani.getDelay() == -1))) {
				this.setAnimation(this.LEFT, this.sprite.getSpriteArray(this.LEFT + this.sprite.getRowOffset()), 5);
			}
		} else if (this.right) {
			if (((this.currentAnimation != this.RIGHT) || (this.ani.getDelay() == -1))) {
				this.setAnimation(this.RIGHT, this.sprite.getSpriteArray(this.RIGHT + this.sprite.getRowOffset()), 5);
			}
		} else if (this.isFallen()) {
			if ((this.currentAnimation != this.FALLEN) || (this.ani.getDelay() == -1)) {
				this.setAnimation(this.FALLEN, this.sprite.getSpriteArray(this.FALLEN + this.sprite.getRowOffset()),
						15);
			}
		} else if (!this.attacking && (this.currentAnimation > 4)) {
			this.setAnimation(this.currentAnimation - this.ATTACK,
					this.sprite.getSpriteArray(this.currentAnimation - this.ATTACK), -1);
		} else if (!this.attacking) {
			if (this.hasIdle && (this.currentAnimation != this.IDLE)) {
				this.setAnimation(this.IDLE, this.sprite.getSpriteArray(this.IDLE + this.sprite.getRowOffset()), 10);
			} else if (!this.hasIdle) {
				this.setAnimation(this.currentAnimation,
						this.sprite.getSpriteArray(this.currentAnimation + this.sprite.getRowOffset()), -1);
			}
		}
	}

	private void setHitBoxDirection() {
		if (this.up && !this.attacking) {
			this.hitBounds.setXOffset((this.size - this.hitBounds.getWidth()) / 2);
			this.hitBounds.setYOffset((-this.hitBounds.getHeight() / 2) + this.hitBounds.getXOffset());
		} else if (this.down && !this.attacking) {
			this.hitBounds.setXOffset((this.size - this.hitBounds.getWidth()) / 2);
			this.hitBounds.setYOffset((this.hitBounds.getHeight() / 2) + this.hitBounds.getXOffset());
		} else if (this.left && !this.attacking) {
			this.hitBounds.setYOffset((this.size - this.hitBounds.getHeight()) / 2);
			this.hitBounds.setXOffset((-this.hitBounds.getWidth() / 2) + this.hitBounds.getYOffset());
		} else if (this.right && !this.attacking) {
			this.hitBounds.setYOffset((this.size - this.hitBounds.getHeight()) / 2);
			this.hitBounds.setXOffset((this.hitBounds.getWidth() / 2) + this.hitBounds.getYOffset());
		}
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
			this.dy -= this.acc;
			if (this.dy < -this.maxSpeed) {
				this.dy = -this.maxSpeed;
			}
		} else if (this.dy < 0) {
			this.dy += this.deacc;
			if (this.dy > 0) {
				this.dy = 0;
			}
		}

		if (this.down) {
			this.currentDirection = this.DOWN;
			this.dy += this.acc;
			if (this.dy > this.maxSpeed) {
				this.dy = this.maxSpeed;
			}
		} else if (this.dy > 0) {
			this.dy -= this.deacc;
			if (this.dy < 0) {
				this.dy = 0;
			}
		}

		if (this.left) {
			this.currentDirection = this.LEFT;
			this.dx -= this.acc;
			if (this.dx < -this.maxSpeed) {
				this.dx = -this.maxSpeed;
			}
		} else if (this.dx < 0) {
			this.dx += this.deacc;
			if (this.dx > 0) {
				this.dx = 0;
			}
		}

		if (this.right) {
			this.currentDirection = this.RIGHT;
			this.dx += this.acc;
			if (this.dx > this.maxSpeed) {
				this.dx = this.maxSpeed;
			}
		} else if (this.dx > 0) {
			this.dx -= this.deacc;
			if (this.dx < 0) {
				this.dx = 0;
			}
		}
	}

	public void update(double time) {
		if (this.isInvincible) {
			if (((this.invincibletime / 1000000) + this.invincible) < (time / 1000000)) {
				this.isInvincible = false;
			}
		}
		this.animate();
		this.setHitBoxDirection();
		this.ani.update();
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
		this.animate();
		int animationSpeed = (int) ((this.getMaxSpeed() / 5) * 100);
		this.ani.setDelay(animationSpeed);
		this.ani.update();
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
