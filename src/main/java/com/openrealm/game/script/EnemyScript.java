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

    /**
     * Called every server tick for every awake enemy bound to this script,
     * independent of the {@link #attack} hook. Unlike attack(), this is not
     * gated by attackRange, DEX cooldown, AI-tick stagger, closest-player
     * targeting, or the player's INVISIBLE/STASIS effects — making it the
     * right place for friendly auras (Enemy67 vault healer) where the
     * effect must apply the instant a player enters the radius regardless
     * of combat state.
     */
    default void tick(final Realm targetRealm, final Enemy enemy) throws Exception {}
}
