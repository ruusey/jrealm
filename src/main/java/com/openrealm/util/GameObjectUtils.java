package com.openrealm.util;

import com.openrealm.game.data.GameDataManager;
import com.openrealm.game.entity.Enemy;
import com.openrealm.game.entity.Monster;
import com.openrealm.game.math.Vector2f;
import com.openrealm.game.model.EnemyModel;
import com.openrealm.net.realm.Realm;

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
