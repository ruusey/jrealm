package com.jrealm.game.script.item;

import com.jrealm.game.contants.StatusEffectType;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.client.packet.CreateEffectPacket;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

/**
 * Helm of the Juggernaut (UT) — applies Berserk to nearby players
 * and Armored (2x DEF) to self. No Speedy.
 * Duration: 2.5s base + 0.1s per WIS over 30.
 */
public class JuggernautHelmScript extends UseableItemScriptBase {

    private static final int ITEM_ID = 277;

    public JuggernautHelmScript(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public boolean handles(int itemId) {
        return itemId == ITEM_ID;
    }

    @Override
    public int getTargetItemId() {
        return ITEM_ID;
    }

    @Override
    public void invokeUseItem(Realm targetRealm, Player player, GameItem item) {
    }

    @Override
    public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem) {
        // Duration: 2.5s base + 0.1s per WIS over 30
        int wis = player.getComputedStats().getWis();
        long duration = 2500 + Math.max(0, (wis - 30)) * 100;

        // Apply Berserk to all nearby players (increases attack speed)
        for (final Player other : targetRealm
                .getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(player, 5))) {
            other.addEffect(StatusEffectType.BERSERK, duration);
        }

        // Apply Armored to self (doubles DEF)
        player.addEffect(StatusEffectType.ARMORED, duration);
        this.mgr.broadcastTextEffect(com.jrealm.game.contants.EntityType.PLAYER, player,
                com.jrealm.game.contants.TextEffect.PLAYER_INFO, "ARMORED");

        // Broadcast buff visual
        final Vector2f center = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
                CreateEffectPacket.EFFECT_HEAL_RADIUS, center.x, center.y, 160.0f, (short) 1500));
    }
}
