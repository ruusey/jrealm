package com.openrealm.game.tile.decorators;

import com.openrealm.net.realm.Realm;

public interface RealmDecorator {
    public void decorate(final Realm input);

    public Integer getTargetMapId();
}
