package com.jrealm.game.entity;

import java.util.UUID;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Monster extends Enemy {
    private String uuid;
    
    public Monster() {
    	super(0, 0, null, 0 ,0);
    }
    
    public Monster(long id, int enemyId, Vector2f origin, int size, int weaponId) {
        super(id, enemyId, origin, size, weaponId);
        final EnemyModel model = GameDataManager.ENEMIES.get(enemyId);
        this.chaseRange = (int) model.getChaseRange();
        this.attackRange = (int) model.getAttackRange();

        this.health = model.getHealth();
        this.right = true;
        this.uuid = UUID.randomUUID().toString();
    }
}