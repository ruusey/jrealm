package com.jrealm.game.script.item;

import com.jrealm.game.realm.RealmManagerServer;

public abstract class UseableItemScriptBase implements UseableItemScript {
    public RealmManagerServer mgr;

    public UseableItemScriptBase(final RealmManagerServer mgr) {
        this.mgr = mgr;
    }
}
