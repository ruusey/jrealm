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

public class Enemy12Script extends EnemyScriptBase {
    public Enemy12Script(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public int getTargetEnemyId() {
        return 12;
    }

    @Override
    public void attack(Realm targetRealm, Enemy enemy, Player targetPlayer) throws Exception {
        final Player target = targetPlayer;
        final Vector2f dest = target.getBounds().getPos().clone(target.getSize() / 2, target.getSize() / 2);
        final Vector2f source = enemy.getPos().clone(target.getSize() / 2, target.getSize() / 2);
        final float angle = Bullet.getAngle(source, dest);
        final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(27);
        final Projectile p = group.getProjectiles().get(0);
        for (int i = 0; i < 4; i++) {
            super.createProjectile(p, targetRealm.getRealmId(), target.getId(), source.clone(),
                    (float) (angle + ((Math.PI / 5) * i)), group);
            super.createProjectile(p, targetRealm.getRealmId(), target.getId(), source.clone(),
                    (float) (angle + ((Math.PI / 5) * -i)), group);
            super.sleep(100);
        }
    }
}
