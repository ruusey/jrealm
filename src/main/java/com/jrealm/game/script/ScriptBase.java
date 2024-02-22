package com.jrealm.game.script;

import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.realm.RealmManagerServer;

import lombok.Data;

@Data
public abstract class ScriptBase implements BulletScript {
	private RealmManagerServer mgr;

	public ScriptBase(final RealmManagerServer mgr) {
		this.mgr = mgr;
	}

	public void sleep(long milis) throws Exception {
		Thread.sleep(milis);
	}

	public void createProjectile(Projectile p, long targetRealmId, long targetPlayerId, Vector2f pos, float angle,
			ProjectileGroup group) {
		this.mgr.addProjectile(targetRealmId, 0l, targetPlayerId, group.getProjectileGroupId(), p.getProjectileId(),
				pos, angle, p.getSize(), p.getMagnitude(), p.getRange(), p.getDamage(), true, p.getFlags(),
				p.getAmplitude(), p.getFrequency());
	}
}
