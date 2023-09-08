package com.jrealm.game.entity.enemy;

import java.util.UUID;

import com.jrealm.game.entity.Enemy;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.util.Camera;

import lombok.Data;

@Data
public class Monster extends Enemy {
	private String uuid;

	public Monster(int id, Camera cam, SpriteSheet sprite, Vector2f origin, int size) {
		super(id, cam, sprite, origin, size);
		//		this.xOffset = size / 4;
		//		this.yOffset = size / 4;

		this.damage = 10;
		this.acc = 1f;
		this.deacc = 2f;
		this.maxSpeed = 1.1f;
		this.r_sense = 512;
		this.r_attackrange = 400;

		this.ATTACK = 0;
		this.IDLE = 0;
		this.FALLEN = 1;
		this.UP = 1;
		this.DOWN = 1;
		this.LEFT = 1;
		this.RIGHT = 1;
		this.health = 1000;
		this.maxHealth = 1000;
		this.hasIdle = true;
		this.useRight = true;

		this.ani.setNumFrames(3, 0);
		this.ani.setNumFrames(5, 1);

		this.currentAnimation = this.IDLE;
		this.right = true;
		this.uuid = UUID.randomUUID().toString();
	}

}