package com.jrealm.game.script;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;

public class Enemy15Script extends EnemyScriptBase {

	public Enemy15Script(RealmManagerServer mgr) {
		super(mgr);
	}

	@Override
	public int getTargetEnemyId() {
		return 15;
	}

	@Override
	public void attack(final Realm targetRealm, final Enemy enemy, final Player targetPlayer) throws Exception {
		final Player target = targetPlayer;
		final Vector2f dest = target.getBounds().getPos().clone(target.getSize() / 2, target.getSize() / 2);

		final Vector2f source = enemy.getPos().clone(target.getSize() / 2, target.getSize() / 2);
		final float angle = Bullet.getAngle(source, dest);
		final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(34);
		final Projectile p = group.getProjectiles().get(0);
		for (int i = 0; i < 6; i++) {
			this.createProjectile(p, targetRealm.getRealmId(), target.getId(), source.withNoise(120, 120),
					angle + (float) Math.PI * i, group);

		}
	}
}
