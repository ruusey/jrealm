package com.jrealm.game.script;

import com.jrealm.game.entity.Bullet;

import lombok.Data;

@Data
abstract class ScriptBase implements BulletScript{
	private Bullet target;
	public ScriptBase(final Bullet target) {
		this.target = target;
	}

}
