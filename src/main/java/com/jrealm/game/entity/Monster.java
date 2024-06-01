package com.jrealm.game.entity;

import java.util.UUID;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.realm.Realm;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Monster extends Enemy {
    private String uuid;

    public Monster(long id, int enemyId, Vector2f origin, int size, int weaponId) {
	super(id, enemyId, origin, size, weaponId);
	EnemyModel model = GameDataManager.ENEMIES.get(enemyId);
	this.acc = 1f;
	this.deacc = 2f;
	this.maxSpeed = 1.4f;
	this.chaseRange = (int) model.getChaseRange();
	this.attackRange = (int) model.getAttackRange();

	this.health = Realm.RANDOM.nextInt(1000) + model.getHealth();
	this.right = true;
	this.uuid = UUID.randomUUID().toString();
    }

}