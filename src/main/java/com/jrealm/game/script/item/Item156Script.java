package com.jrealm.game.script.item;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

public class Item156Script extends UseableItemScriptBase {

    public Item156Script(final RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public void invokeUseItem(final Realm targetRealm, final Player player, final GameItem item) {
    }

    @Override
    public void invokeItemAbility(final Realm targetRealm, final Player player, final GameItem abilityItem) {
        for (final Player other : targetRealm
                .getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(player, 5))) {
            other.addEffect(abilityItem.getEffect().getEffectId(), abilityItem.getEffect().getDuration());
        }
    }

    @Override
    public int getTargetItemId() {
        return 156;
    }

}
