package com.jrealm.game.util;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Monster;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.realm.Realm;

public class GameObjectUtils {
	public static Enemy getEnemyFromId(final int enemyId) {
		EnemyModel toSpawn = GameDataManager.ENEMIES.get(13);
		SpriteSheet enemySheet = GameDataManager.SPRITE_SHEETS.get(toSpawn.getSpriteKey());
		SpriteSheet enemySprite = new SpriteSheet(
				enemySheet.getSprite(toSpawn.getCol(), toSpawn.getRow(), toSpawn.getSpriteSize(),
						toSpawn.getSpriteSize()),
				toSpawn.getName(), toSpawn.getSpriteSize(), toSpawn.getSpriteSize(), 0);
		Enemy enemy = new Monster(Realm.RANDOM.nextLong(), toSpawn.getEnemyId(), enemySprite, new Vector2f(),
				toSpawn.getSize(), toSpawn.getAttackId());
		return enemy;
	}

	public static Enemy getEnemyFromId(final int enemyId, final Vector2f pos) {
		final Enemy enemy = GameObjectUtils.getEnemyFromId(enemyId);
		enemy.setPos(pos);
		return enemy;
	}

}
