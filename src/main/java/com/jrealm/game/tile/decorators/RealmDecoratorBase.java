package com.jrealm.game.tile.decorators;

import com.jrealm.net.realm.RealmManagerServer;

import lombok.Data;

@Data
public abstract class RealmDecoratorBase implements RealmDecorator {
    public RealmManagerServer mgr;

    public RealmDecoratorBase(RealmManagerServer mgr) {
        this.mgr = mgr;
    }
}
