package com.jrealm.game.script.item;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.client.packet.CreateEffectPacket;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

public class Item157Script extends UseableItemScriptBase {

    public Item157Script(final RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public void invokeUseItem(final Realm targetRealm, final Player player, final GameItem item) {
    }

    @Override
    public void invokeItemAbility(final Realm targetRealm, final Player player, final GameItem abilityItem) {
        final Effect effect = abilityItem.getEffect();
        player.addEffect(effect.getEffectId(), effect.getDuration());
        // Broadcast heal radius visual effect
        final Vector2f center = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
            CreateEffectPacket.EFFECT_HEAL_RADIUS, center.x, center.y, 224.0f, (short) 800));
        int healthDiff = player.getComputedStats().getHp() - player.getHealth();
        if (healthDiff > 0) {
            int healthToAdd = healthDiff < 50 ? healthDiff : 50;
            player.setHealth(player.getHealth() + healthToAdd);
        }
        for (final Player other : targetRealm
                .getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(player, 7))) {
            healthDiff = player.getComputedStats().getHp() - player.getHealth();
            if (healthDiff > 0) {
                int healthToAdd = healthDiff < 50 ? healthDiff : 50;
                other.setHealth(other.getHealth() + healthToAdd);
            }
        }
    }

    @Override
    public int getTargetItemId() {
        return 157;
    }
}
