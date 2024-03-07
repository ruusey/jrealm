package com.jrealm.game.script;

import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.realm.Realm;

public interface EnemyScript {
	public int getTargetEnemyId();
	public void attack(Realm targetRealm, Enemy enemy, Player targetPlayer) throws Exception;
}
