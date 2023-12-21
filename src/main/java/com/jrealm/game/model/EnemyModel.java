package com.jrealm.game.model;

import com.jrealm.game.entity.item.Stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper=false)
public class EnemyModel extends SpriteModel {
	private int enemyId;
	private int size;
	private int spriteSize;
	private int attackId;
	private String name;
	private int xp;
	private Stats stats;
	private int health;
	private float maxSpeed;
	private float chaseRange;
	private float attackRange;

}
