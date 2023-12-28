package com.jrealm.game.entity.enemy;

import java.util.Random;
import java.util.UUID;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Monster extends Enemy {
	private String uuid;

	public Monster(long id, int enemyId, SpriteSheet sprite, Vector2f origin, int size, int weaponId) {
		super(id, enemyId, sprite, origin, size, weaponId);
		EnemyModel model = GameDataManager.ENEMIES.get(enemyId);
		// this.xOffset = size / 4;
		// this.yOffset = size / 4;
		Random r = new Random(System.nanoTime());
		this.acc = 1f;
		this.deacc = 2f;
		this.maxSpeed = 1.4f;
		this.r_sense = (int) model.getChaseRange();
		this.r_attackrange = (int) model.getAttackRange();

		this.ATTACK = 0;
		this.IDLE = 0;
		this.FALLEN = 1;
		this.UP = 1;
		this.DOWN = 1;
		this.LEFT = 1;
		this.RIGHT = 1;
		this.health = r.nextInt(1000) + model.getHealth();
		this.hasIdle = true;
		this.useRight = true;

		this.ani.setNumFrames(3, 0);
		this.ani.setNumFrames(5, 1);

		this.currentAnimation = this.IDLE;
		this.right = true;
		this.uuid = UUID.randomUUID().toString();
	}

	public Monster(long id, int enemyId, Vector2f origin, int size, int weaponId) {
		super(id, enemyId, origin, size, weaponId);
		// this.xOffset = size / 4;
		// this.yOffset = size / 4;
		Random r = new Random(System.nanoTime());
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
		this.hasIdle = true;
		this.useRight = true;


		this.right = true;
		this.uuid = UUID.randomUUID().toString();
	}

}