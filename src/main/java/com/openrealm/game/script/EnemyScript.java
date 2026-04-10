package com.openrealm.game.script;

import com.openrealm.game.entity.Enemy;
import com.openrealm.game.entity.Player;
import com.openrealm.net.realm.Realm;

public interface EnemyScript {
    public int getTargetEnemyId();

    public void attack(final Realm targetRealm, final Enemy enemy, final Player targetPlayer) throws Exception;
}
