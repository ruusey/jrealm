package com.jrealm.game.tile.decorators;

import com.jrealm.net.realm.Realm;

public interface RealmDecorator {
    public void decorate(final Realm input);

    public Integer getTargetMapId();
}
