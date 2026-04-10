package com.openrealm.game.script.item;

import com.openrealm.game.contants.EntityType;
import com.openrealm.game.contants.StatusEffectType;
import com.openrealm.game.contants.TextEffect;
import com.openrealm.game.entity.Player;
import com.openrealm.game.entity.item.GameItem;
import com.openrealm.game.math.Vector2f;
import com.openrealm.net.client.packet.CreateEffectPacket;
import com.openrealm.net.realm.Realm;
import com.openrealm.net.realm.RealmManagerServer;

/**
 * Seal of Blasphemous Prayer (item 287) — Paladin UT seal.
 * Grants brief INVINCIBLE to all nearby players,
 * plus HEALING and DAMAGING to self.
 */
public class SealOfBlasphemousPrayerScript extends UseableItemScriptBase {

    public SealOfBlasphemousPrayerScript(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public boolean handles(int itemId) {
        return itemId == 287;
    }

    @Override
    public void invokeUseItem(final Realm targetRealm, final Player player, final GameItem item) {
    }

    @Override
    public void invokeItemAbility(final Realm targetRealm, final Player player, final GameItem abilityItem) {
        final long duration = abilityItem.getEffect().getDuration(); // 1500ms

        // All nearby players get Invulnerable
        for (final Player target : targetRealm
                .getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(player, 5))) {
            target.addEffect(StatusEffectType.INVINCIBLE, duration);
            this.mgr.broadcastTextEffect(
                EntityType.PLAYER, target, TextEffect.PLAYER_INFO, "INVULNERABLE");
        }

        // Self: also gets Healing + Damaging for double duration
        player.addEffect(StatusEffectType.HEALING, duration * 2);
        player.addEffect(StatusEffectType.DAMAGING, duration * 2);
        this.mgr.broadcastTextEffect(
            EntityType.PLAYER, player, TextEffect.PLAYER_INFO, "HEALING + DAMAGING");

        // Visual: golden heal ring
        final Vector2f center = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
            CreateEffectPacket.EFFECT_HEAL_RADIUS, center.x, center.y, 160.0f, (short) 1500));
    }

    @Override
    public int getTargetItemId() {
        return 287;
    }
}
