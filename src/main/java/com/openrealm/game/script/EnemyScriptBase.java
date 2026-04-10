package com.openrealm.game.script;

import com.openrealm.game.math.Vector2f;
import com.openrealm.game.model.Projectile;
import com.openrealm.game.model.ProjectileGroup;
import com.openrealm.net.realm.RealmManagerServer;

import lombok.Data;

@Data
public abstract class EnemyScriptBase implements EnemyScript {
    private RealmManagerServer mgr;

    public EnemyScriptBase(final RealmManagerServer mgr) {
        this.mgr = mgr;
    }

    public void sleep(final long milis) throws Exception {
        Thread.sleep(milis);
    }

    public void createProjectile(final Projectile p, final long targetRealmId, final long targetPlayerId,
            final Vector2f pos, final float angle, final ProjectileGroup group) {
        this.mgr.addProjectile(targetRealmId, 0l, targetPlayerId, group.getProjectileGroupId(), p.getProjectileId(),
                pos, angle, p.getSize(), p.getMagnitude(), p.getRange(), p.getDamage(), true, p.getFlags(),
                p.getAmplitude(), p.getFrequency(), 0l);
    }
}
