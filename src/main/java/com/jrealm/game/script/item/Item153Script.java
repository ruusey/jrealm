package com.jrealm.game.script.item;

import com.jrealm.game.contants.ProjectileEffectType;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.client.packet.CreateEffectPacket;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

/**
 * Paladin Seal ability — applies HEALING + DAMAGING buff to nearby players.
 * Handles test seal (153) and tiered seals (263-269).
 */
public class Item153Script extends UseableItemScriptBase {

    public Item153Script(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public boolean handles(int itemId) {
        return itemId == 153 || (itemId >= 263 && itemId <= 269);
    }

    @Override
    public void invokeUseItem(final Realm targetRealm, final Player player, final GameItem item) {
    }

    @Override
    public void invokeItemAbility(final Realm targetRealm, final Player player, final GameItem abilityItem) {
        for (final Player other : targetRealm
                .getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(player, 5))) {
            other.addEffect(abilityItem.getEffect().getEffectId(), abilityItem.getEffect().getDuration());
            other.addEffect(ProjectileEffectType.DAMAGING, abilityItem.getEffect().getDuration()*2);
        }
        // Broadcast paladin seal visual (golden heal ring)
        final Vector2f center = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
            CreateEffectPacket.EFFECT_HEAL_RADIUS, center.x, center.y, 160.0f, (short) 800));
    }

    @Override
    public int getTargetItemId() {
        return 153;
    }

}
