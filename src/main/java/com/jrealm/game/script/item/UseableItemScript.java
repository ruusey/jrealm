package com.jrealm.game.script.item;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.realm.Realm;

public interface UseableItemScript {
    public void invokeUseItem(Realm targetRealm, Player player, GameItem item);

    public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem);

    public int getTargetItemId();
}
