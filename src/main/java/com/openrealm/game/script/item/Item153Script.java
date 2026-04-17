package com.openrealm.game.script.item;

import com.openrealm.game.contants.StatusEffectType;
import com.openrealm.game.entity.Player;
import com.openrealm.game.entity.item.GameItem;
import com.openrealm.game.math.Vector2f;
import com.openrealm.net.client.packet.CreateEffectPacket;
import com.openrealm.net.realm.Realm;
import com.openrealm.net.realm.RealmManagerServer;

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
        final long duration = abilityItem.getEffect().getDuration();
        for (final Player target : targetRealm
                .getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(player, 5))) {
            target.addEffect(abilityItem.getEffect().getEffectId(), duration);
            target.addEffect(StatusEffectType.DAMAGING, duration * 2);
            this.mgr.broadcastTextEffect(
                com.openrealm.game.contants.EntityType.PLAYER, target,
                com.openrealm.game.contants.TextEffect.PLAYER_INFO, "HEALING + DAMAGING");
        }
        // Broadcast paladin seal visual (golden heal ring)
        final Vector2f center = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        this.mgr.enqueueServerPacketToRealm(targetRealm, CreateEffectPacket.aoeEffect(
            CreateEffectPacket.EFFECT_HEAL_RADIUS, center.x, center.y, 160.0f, (short) 1500));
    }

    @Override
    public int getTargetItemId() {
        return 153;
    }

}
