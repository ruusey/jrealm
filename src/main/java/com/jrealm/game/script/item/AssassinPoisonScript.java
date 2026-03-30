package com.jrealm.game.script.item;

import com.jrealm.game.contants.EntityType;
import com.jrealm.game.contants.ProjectileEffectType;
import com.jrealm.game.contants.TextEffect;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.client.packet.CreateEffectPacket;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

/**
 * Assassin Poison ability (items 249-255, T0-T6).
 * Throws a poison vial at cursor position. Enemies in the AoE radius
 * receive POISONED status and take damage over time (ignores defense).
 * Total damage and duration scale with tier.
 */
public class AssassinPoisonScript extends UseableItemScriptBase {

    private static final int MIN_ID = 249;
    private static final int MAX_ID = 255;
    private static final float POISON_RADIUS = 128.0f; // ~4 tiles AoE radius

    public AssassinPoisonScript(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public boolean handles(int itemId) {
        return itemId >= MIN_ID && itemId <= MAX_ID;
    }

    @Override
    public int getTargetItemId() {
        return MIN_ID;
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

        int tier = abilityItem.getItemId() - MIN_ID;

        // Total poison damage scales with tier: T0=100, T6=700
        int totalDamage = 100 + tier * 100;
        // Poison duration: T0=4s, T6=2.5s (higher tiers deal damage faster)
        long poisonDuration = 4000 - tier * 250;
        // Add ATT stat bonus to total damage
        totalDamage += player.getComputedStats().getAtt();

        // Broadcast poison splash visual at cursor position
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
                CreateEffectPacket.EFFECT_POISON_SPLASH,
                center.x, center.y, POISON_RADIUS, (short) 1500));

        // Apply poison to all enemies in radius
        for (final Enemy enemy : targetRealm.getEnemies().values()) {
            if (enemy.getDeath()) continue;
            if (enemy.hasEffect(ProjectileEffectType.STASIS)) continue;

            float dx = enemy.getPos().x - center.x;
            float dy = enemy.getPos().y - center.y;
            float distSq = dx * dx + dy * dy;

            if (distSq <= POISON_RADIUS * POISON_RADIUS) {
                // Apply POISONED visual effect
                enemy.addEffect(ProjectileEffectType.POISONED, poisonDuration);

                // Register poison DoT with the server
                this.mgr.registerPoisonDot(targetRealm.getRealmId(), enemy.getId(),
                        totalDamage, poisonDuration, player.getId());

                this.mgr.broadcastTextEffect(EntityType.ENEMY, enemy, TextEffect.DAMAGE, "POISONED");
            }
        }
    }
}
