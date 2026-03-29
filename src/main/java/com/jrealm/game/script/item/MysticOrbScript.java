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
 * Mystic Orb ability (items 256-262, T0-T6).
 * AoE at cursor: inner radius applies STASIS (frozen + invulnerable),
 * outer radius applies CURSED (take 25% more damage).
 */
public class MysticOrbScript extends UseableItemScriptBase {

    private static final int MIN_ID = 256;
    private static final int MAX_ID = 262;
    private static final float STASIS_RADIUS = 96.0f;  // ~3 tiles inner zone
    private static final float CURSE_RADIUS = 160.0f;  // ~5 tiles outer zone
    private static final long CURSE_DURATION = 5000;    // 5 seconds

    public MysticOrbScript(RealmManagerServer mgr) {
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
        // Core code applies NONE effect to player (harmless). Script handles real behavior.
        final Vector2f center = (targetPos != null) ? targetPos : player.getPos().clone(player.getSize() / 2, player.getSize() / 2);

        // Broadcast stasis visual (frozen blue ring)
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
            CreateEffectPacket.EFFECT_STASIS_FIELD, center.x, center.y, STASIS_RADIUS, (short) 1000));
        // Broadcast curse visual (dark swirl in outer ring)
        this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
            CreateEffectPacket.EFFECT_CURSE_RADIUS, center.x, center.y, CURSE_RADIUS, (short) 800));
        final long stasisDuration = abilityItem.getEffect().getDuration();

        for (final Enemy enemy : targetRealm.getEnemies().values()) {
            if (enemy.getDeath()) continue;
            // Don't re-stasis enemies already in stasis
            if (enemy.hasEffect(ProjectileEffectType.STASIS)) continue;

            float dx = enemy.getPos().x - center.x;
            float dy = enemy.getPos().y - center.y;
            float distSq = dx * dx + dy * dy;

            if (distSq <= STASIS_RADIUS * STASIS_RADIUS) {
                // Inner zone: STASIS (frozen + invulnerable + can't attack)
                enemy.addEffect(ProjectileEffectType.STASIS, stasisDuration);
                this.mgr.broadcastTextEffect(EntityType.ENEMY, enemy, TextEffect.DAMAGE, "STASIS");
            } else if (distSq <= CURSE_RADIUS * CURSE_RADIUS) {
                // Outer zone: CURSED (take 25% more damage)
                enemy.addEffect(ProjectileEffectType.CURSED, CURSE_DURATION);
                this.mgr.broadcastTextEffect(EntityType.ENEMY, enemy, TextEffect.DAMAGE, "CURSED");
            }
        }
    }
}
