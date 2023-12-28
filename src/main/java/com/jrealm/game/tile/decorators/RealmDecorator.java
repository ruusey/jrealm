package com.jrealm.game.tile.decorators;

import com.jrealm.game.realm.Realm;

public interface RealmDecorator {
	public void decorate(final Realm input);

	public Integer getTargetMapId();
}
