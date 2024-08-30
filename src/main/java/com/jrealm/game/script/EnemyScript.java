package com.jrealm.game.script;

import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.net.realm.Realm;

public interface EnemyScript {
    public int getTargetEnemyId();

    public void attack(final Realm targetRealm, final Enemy enemy, final Player targetPlayer) throws Exception;
}
