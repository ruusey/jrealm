package com.jrealm.game.util;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Monster;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.net.realm.Realm;

public class GameObjectUtils {
    public static Enemy getEnemyFromId(final int enemyId) {
        EnemyModel toSpawn = GameDataManager.ENEMIES.get(enemyId);
        Enemy enemy = new Monster(Realm.RANDOM.nextLong(), toSpawn.getEnemyId(), new Vector2f(), toSpawn.getSize(),
                toSpawn.getAttackId());
        return enemy;
    }

    public static Enemy getEnemyFromId(final int enemyId, final Vector2f pos) {
        final Enemy enemy = GameObjectUtils.getEnemyFromId(enemyId);
        enemy.setPos(pos);
        return enemy;
    }
}
