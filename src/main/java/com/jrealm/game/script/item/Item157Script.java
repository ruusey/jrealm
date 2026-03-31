package com.jrealm.game.script.item;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.client.packet.CreateEffectPacket;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

/**
 * Priest Tome ability — heals self and nearby players.
 * Handles test tome (157) and tiered tomes (228-234).
 */
public class Item157Script extends UseableItemScriptBase {

    public Item157Script(final RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public boolean handles(int itemId) {
        return itemId == 157 || (itemId >= 228 && itemId <= 234);
    }

    @Override
    public void invokeUseItem(final Realm targetRealm, final Player player, final GameItem item) {
    }

    // RotMG wiki heal values: T0=50, T1=90, T2=100, T3=110, T4=120, T5=130, T6=140
    private static final int[] HEAL_BY_TIER = { 50, 90, 100, 110, 120, 130, 140 };

    @Override
    public void invokeItemAbility(final Realm targetRealm, final Player player, final GameItem abilityItem) {
        final Effect effect = abilityItem.getEffect();

        // Calculate heal amount based on tier
        int tier = 0;
        if (abilityItem.getItemId() >= 228 && abilityItem.getItemId() <= 234) {
            tier = abilityItem.getItemId() - 228;
        }
        final int healAmount = HEAL_BY_TIER[Math.min(tier, HEAL_BY_TIER.length - 1)];

        // Broadcast heal radius visual
        final Vector2f center = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
            CreateEffectPacket.EFFECT_HEAL_RADIUS, center.x, center.y, 224.0f, (short) 1500));

        // Heal ALL players in range (including self) and apply healing effect
        for (final Player target : targetRealm
                .getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(player, 7))) {
            target.addEffect(effect.getEffectId(), effect.getDuration());
            int maxHp = target.getComputedStats().getHp();
            int missing = maxHp - target.getHealth();
            if (missing > 0) {
                int toHeal = Math.min(healAmount, missing);
                target.setHealth(target.getHealth() + toHeal);
                this.mgr.broadcastTextEffect(
                    com.jrealm.game.contants.EntityType.PLAYER, target,
                    com.jrealm.game.contants.TextEffect.HEAL, "+" + toHeal);
            }
        }
    }

    @Override
    public int getTargetItemId() {
        return 157;
    }
}
