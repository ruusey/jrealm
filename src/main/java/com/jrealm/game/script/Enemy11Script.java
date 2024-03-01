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

public class Enemy11Script extends EnemyScriptBase {

	public Enemy11Script(RealmManagerServer mgr) {
		super(mgr);
	}

	@Override
	public int getTargetEnemyId() {
		return 11;
	}

	@Override
	public void attack(Realm targetRealm, Enemy enemy, Player targetPlayer) throws Exception {

		Player target = targetPlayer;
		Vector2f dest = target.getBounds().getPos().clone(target.getSize() / 2, target.getSize() / 2);

		Vector2f source = enemy.getPos().clone(target.getSize() / 2, target.getSize() / 2);
		float angle = Bullet.getAngle(source, dest);
		ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(26);
		Projectile p = group.getProjectiles().get(0);
		for (int i = 0; i < 6; i++) {
			this.createProjectile(p, targetRealm.getRealmId(), target.getId(), source.clone(),
					(float) (angle + ((Math.PI / 6) * i)), group);
			this.sleep(150);

		}
	}
}
