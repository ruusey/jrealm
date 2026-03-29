package com.jrealm.game.script.item;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.client.packet.CreateEffectPacket;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

/**
 * Warrior Helm ability — applies SPEEDY buff to nearby players.
 * Handles test helm (156) and tiered helms (235-241).
 */
public class Item156Script extends UseableItemScriptBase {

    public Item156Script(final RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public boolean handles(int itemId) {
        return itemId == 156 || (itemId >= 235 && itemId <= 241);
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
        // Broadcast warrior buff visual (orange/yellow expanding ring)
        final Vector2f center = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
            CreateEffectPacket.EFFECT_HEAL_RADIUS, center.x, center.y, 160.0f, (short) 1500));
    }

    @Override
    public int getTargetItemId() {
        return 156;
    }

}
