package com.jrealm.game.entity.enemy;

import java.util.Random;
import java.util.UUID;

import com.jrealm.game.entity.Enemy;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Vector2f;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Monster extends Enemy {
	private String uuid;

	public Monster(long id, SpriteSheet sprite, Vector2f origin, int size, int weaponId) {
		super(id, sprite, origin, size, weaponId);
		// this.xOffset = size / 4;
		// this.yOffset = size / 4;
		Random r = new Random(System.nanoTime());
		this.damage = 10;
		this.acc = 1f;
		this.deacc = 2f;
		this.maxSpeed = 1.4f;
		this.r_sense = 650;
		this.r_attackrange = 450;

		this.ATTACK = 0;
		this.IDLE = 0;
		this.FALLEN = 1;
		this.UP = 1;
		this.DOWN = 1;
		this.LEFT = 1;
		this.RIGHT = 1;
		this.health = r.nextInt(1000) + 1000;
		this.maxHealth = this.health;
		this.hasIdle = true;
		this.useRight = true;

		this.ani.setNumFrames(3, 0);
		this.ani.setNumFrames(5, 1);

		this.currentAnimation = this.IDLE;
		this.right = true;
		this.uuid = UUID.randomUUID().toString();
	}

}