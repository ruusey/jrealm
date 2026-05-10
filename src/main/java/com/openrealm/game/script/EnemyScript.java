package com.openrealm.game.script;

import com.openrealm.game.entity.Enemy;
import com.openrealm.game.entity.Player;
import com.openrealm.net.realm.Realm;

public interface EnemyScript {
    public int getTargetEnemyId();

    public void attack(final Realm targetRealm, final Enemy enemy, final Player targetPlayer) throws Exception;

    /**
     * If true, the enemy's data-driven phase attacks ({@code phase.attacks[]})
     * still fire alongside this script. If false (default), the script
     * fully replaces the data-driven attack path — same behavior as the
     * Enemy67 healer, which has no JSON attacks at all.
     */
    default boolean isAdditive() { return false; }
}
