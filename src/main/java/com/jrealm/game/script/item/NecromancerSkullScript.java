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
 * Necromancer Skull ability (items 200-206, T0-T6).
 * AoE damage at cursor position + vampirism heal to self.
 * Damage is dealt directly (not via projectiles) to enemies within radius.
 * Heal amount = percentage of total damage dealt.
 */
public class NecromancerSkullScript extends UseableItemScriptBase {

    // Skull item IDs: 200 (T0) through 206 (T6)
    private static final int MIN_ID = 200;
    private static final int MAX_ID = 206;
    private static final float DAMAGE_RADIUS = 80.0f; // ~2.5 tiles
    private static final float HEAL_RATIO = 0.25f; // heal 25% of damage dealt

    public NecromancerSkullScript(RealmManagerServer mgr) {
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
        invokeItemAbility(targetRealm, player, abilityItem, player.getPos().clone(player.getSize() / 2, player.getSize() / 2));
    }

    @Override
    public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem, Vector2f targetPos) {
        // The core ability code already applied HEAL effect to self.
        // This script adds: AoE damage at cursor position + vampirism heal.
        final Vector2f center = (targetPos != null) ? targetPos : player.getPos().clone(player.getSize() / 2, player.getSize() / 2);

        // Base damage scales with tier (T0=65, T6=320) + attack stat.
        // Tier is encoded in itemId: 200=T0, 201=T1, ..., 206=T6
        int tier = abilityItem.getItemId() - MIN_ID;
        short baseDamage = (short) (65 + tier * 42);
        baseDamage += player.getComputedStats().getAtt();

        int totalDamageDealt = 0;
        for (final Enemy enemy : targetRealm.getEnemies().values()) {
            if (enemy.getDeath()) continue;
            float dx = enemy.getPos().x - center.x;
            float dy = enemy.getPos().y - center.y;
            float distSq = dx * dx + dy * dy;
            if (distSq <= DAMAGE_RADIUS * DAMAGE_RADIUS) {
                // Skip stasis-immune enemies
                if (enemy.hasEffect(ProjectileEffectType.STASIS)) continue;

                short dmg = (short) Math.max(baseDamage - enemy.getStats().getDef(), baseDamage * 0.15);
                enemy.setHealth(enemy.getHealth() - dmg);
                totalDamageDealt += dmg;

                this.mgr.broadcastTextEffect(EntityType.ENEMY, enemy, TextEffect.DAMAGE, "-" + dmg);
                if (enemy.getDeath()) {
                    this.mgr.enemyDeath(targetRealm, enemy);
                }
            }
        }

        // Broadcast vampirism visual effect (inward-sucking particles at player center)
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
            CreateEffectPacket.EFFECT_VAMPIRISM, center.x, center.y, DAMAGE_RADIUS, (short) 1500));

        // Vampirism heal: heal self based on damage dealt
        if (totalDamageDealt > 0) {
            int healAmount = (int) (totalDamageDealt * HEAL_RATIO);
            int maxHp = player.getComputedStats().getHp();
            int newHealth = Math.min(player.getHealth() + healAmount, maxHp);
            player.setHealth(newHealth);
            this.mgr.broadcastTextEffect(EntityType.PLAYER, player, TextEffect.HEAL, "+" + healAmount);
        }
    }
}
