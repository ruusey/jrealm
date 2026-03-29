package com.jrealm.game.script.item;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.realm.Realm;

public interface UseableItemScript {
    public void invokeUseItem(Realm targetRealm, Player player, GameItem item);

    public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem);

    /**
     * Ability activation with target position (where the player clicked/tapped).
     * Default delegates to the position-less overload for backward compatibility.
     */
    default void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem, Vector2f targetPos) {
        invokeItemAbility(targetRealm, player, abilityItem);
    }

    public int getTargetItemId();

    /**
     * Check if this script handles the given item ID.
     * Override for scripts that handle a range of item IDs (e.g., tiered abilities).
     */
    default boolean handles(int itemId) {
        return getTargetItemId() == itemId;
    }
}
