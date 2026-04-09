package com.jrealm.game.script.item;

import com.jrealm.game.contants.EntityType;
import com.jrealm.game.contants.StatusEffectType;
import com.jrealm.game.contants.TextEffect;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.client.packet.CreateEffectPacket;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

/**
 * Orb of Conflict (UT, itemId 278) — Mystic untiered orb.
 * AoE at cursor: applies STASIS to enemies within range (7s).
 * Self: grants SPEEDY + DAMAGING for 2s (+0.1s per WIS over 50).
 * Does NOT apply Curse (unlike regular orbs).
 */
public class OrbOfConflictScript extends UseableItemScriptBase {

    private static final int ITEM_ID = 278;
    private static final float STASIS_RADIUS = 96.0f;   // ~3 tiles
    private static final long SELF_BUFF_BASE = 2000;     // 2 seconds base

    public OrbOfConflictScript(RealmManagerServer mgr) {
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
        invokeItemAbility(targetRealm, player, abilityItem,
                player.getPos().clone(player.getSize() / 2, player.getSize() / 2));
    }

    @Override
    public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem, Vector2f targetPos) {
        final Vector2f center = (targetPos != null) ? targetPos
                : player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        final long stasisDuration = abilityItem.getEffect().getDuration();

        // --- Enemy stasis at cursor position ---
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
                CreateEffectPacket.EFFECT_STASIS_FIELD, center.x, center.y, STASIS_RADIUS, (short) 1800));

        for (final Enemy enemy : targetRealm.getEnemies().values()) {
            if (enemy.getDeath()) continue;
            if (enemy.hasEffect(StatusEffectType.STASIS)) continue;

            float dx = enemy.getPos().x - center.x;
            float dy = enemy.getPos().y - center.y;
            float distSq = dx * dx + dy * dy;

            if (distSq <= STASIS_RADIUS * STASIS_RADIUS) {
                enemy.addEffect(StatusEffectType.STASIS, stasisDuration);
                this.mgr.broadcastTextEffect(EntityType.ENEMY, enemy, TextEffect.DAMAGE, "STASIS");
            }
        }

        // --- Self buffs: Speedy + Damaging ---
        int wis = player.getComputedStats().getWis();
        long selfDuration = SELF_BUFF_BASE + Math.max(0, (wis - 50)) * 100;

        player.addEffect(StatusEffectType.SPEEDY, selfDuration);
        player.addEffect(StatusEffectType.DAMAGING, selfDuration);
        this.mgr.broadcastTextEffect(EntityType.PLAYER, player,
                TextEffect.PLAYER_INFO, "SPEEDY + DAMAGING");

        // Broadcast self-buff visual (green heal ring on player)
        final Vector2f playerCenter = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
                CreateEffectPacket.EFFECT_HEAL_RADIUS, playerCenter.x, playerCenter.y, 64.0f, (short) 1200));
    }
}
