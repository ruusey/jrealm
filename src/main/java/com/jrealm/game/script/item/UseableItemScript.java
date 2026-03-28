package com.jrealm.game.script.item;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.net.realm.Realm;

public interface UseableItemScript {
    public void invokeUseItem(Realm targetRealm, Player player, GameItem item);

    public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem);

    public int getTargetItemId();

    /**
     * Check if this script handles the given item ID.
     * Override for scripts that handle a range of item IDs (e.g., tiered abilities).
     */
    default boolean handles(int itemId) {
        return getTargetItemId() == itemId;
    }
}
