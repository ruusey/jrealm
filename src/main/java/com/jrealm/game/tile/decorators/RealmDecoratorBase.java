package com.jrealm.game.tile.decorators;

import com.jrealm.game.realm.RealmManagerServer;

import lombok.Data;

@Data
public abstract class RealmDecoratorBase implements RealmDecorator {
    private RealmManagerServer mgr;

    public RealmDecoratorBase(RealmManagerServer mgr) {
        this.mgr = mgr;
    }
}
