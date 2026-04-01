package com.jrealm.game.script;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

/**
 * Cyclops (Enemy 10) — Highlands brute with a 3-phase attack pattern:
 * 1. Ring burst: 8 projectiles in a circle (area denial)
 * 2. Predictive triple shot: aimed where the player is heading
 * 3. Delayed spiral sweep: rotating arc of projectiles
 */
public class Enemy10Script extends EnemyScriptBase {

    public Enemy10Script(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public int getTargetEnemyId() {
        return 10;
    }

    @Override
    public void attack(final Realm targetRealm, final Enemy enemy, final Player targetPlayer) throws Exception {
        final Vector2f dest = targetPlayer.getBounds().getPos().clone(targetPlayer.getSize() / 2, targetPlayer.getSize() / 2);
        final Vector2f source = enemy.getPos().clone(enemy.getSize() / 2, enemy.getSize() / 2);
        final float angle = Bullet.getAngle(source, dest);
        final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(2);
        final Projectile p = group.getProjectiles().get(0);

        // Phase 1: Ring burst — 8 projectiles evenly spaced in a circle
        for (int i = 0; i < 8; i++) {
            float ringAngle = (float) (i * Math.PI * 2 / 8);
            super.createProjectile(p, targetRealm.getRealmId(), targetPlayer.getId(), source.clone(), ringAngle, group);
        }

        super.sleep(300);

        // Phase 2: Predictive triple shot — aimed ahead of the player's movement
        float leadAngle = angle;
        if (Math.abs(targetPlayer.getDx()) > 0.1f || Math.abs(targetPlayer.getDy()) > 0.1f) {
            // Lead the target: predict where they'll be
            float predX = dest.x + targetPlayer.getDx() * 12;
            float predY = dest.y + targetPlayer.getDy() * 12;
            leadAngle = Bullet.getAngle(source, new Vector2f(predX, predY));
        }
        float spread = 0.15f;
        for (int burst = 0; burst < 2; burst++) {
            super.createProjectile(p, targetRealm.getRealmId(), targetPlayer.getId(), source.clone(), leadAngle - spread, group);
            super.createProjectile(p, targetRealm.getRealmId(), targetPlayer.getId(), source.clone(), leadAngle, group);
            super.createProjectile(p, targetRealm.getRealmId(), targetPlayer.getId(), source.clone(), leadAngle + spread, group);
            super.sleep(150);
        }

        super.sleep(200);

        // Phase 3: Spiral sweep — 6 projectiles rotating outward
        for (int i = 0; i < 6; i++) {
            float sweepAngle = angle + (float) (i * 0.35) - 0.9f;
            super.createProjectile(p, targetRealm.getRealmId(), targetPlayer.getId(), source.clone(), sweepAngle, group);
            super.sleep(80);
        }
    }
}
