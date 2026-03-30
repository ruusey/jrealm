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
import com.jrealm.util.WorkerThread;

/**
 * Assassin Poison ability (items 249-255, T0-T6).
 * Throws a poison vial at cursor position with a 0.8s travel time.
 * On landing, enemies in the AoE get POISONED and take DoT (ignores defense).
 */
public class AssassinPoisonScript extends UseableItemScriptBase {

    private static final int MIN_ID = 249;
    private static final int MAX_ID = 255;
    private static final float POISON_RADIUS = 128.0f;
    private static final long THROW_DURATION_MS = 800;

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
        // RotMG values: T0=150, +150 per tier, T6=1050
        final int totalDamage = 150 + tier * 150 + player.getComputedStats().getAtt();
        // Duration: T0=3.0s, T6=4.5s
        final long poisonDuration = 3000 + tier * 250;

        // Broadcast the throw arc (800ms travel time)
        final Vector2f playerCenter = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        this.mgr.enqueueServerPacket(CreateEffectPacket.lineEffect(
                CreateEffectPacket.EFFECT_POISON_SPLASH,
                playerCenter.x, playerCenter.y, center.x, center.y, (short) THROW_DURATION_MS));

        // Schedule the landing effect and damage after the throw completes
        final long realmId = targetRealm.getRealmId();
        final long playerId = player.getId();
        final float landX = center.x;
        final float landY = center.y;

        WorkerThread.submitAndForkRun(() -> {
            try {
                Thread.sleep(THROW_DURATION_MS);
            } catch (InterruptedException e) {
                return;
            }

            final Realm realm = this.mgr.getRealms().get(realmId);
            if (realm == null) return;

            // Broadcast splash AoE on landing
            this.mgr.enqueueServerPacket(CreateEffectPacket.aoeEffect(
                    CreateEffectPacket.EFFECT_POISON_SPLASH,
                    landX, landY, POISON_RADIUS, (short) 1500));

            // Apply poison to enemies in radius at the moment of landing
            for (final Enemy enemy : realm.getEnemies().values()) {
                if (enemy.getDeath()) continue;
                if (enemy.hasEffect(ProjectileEffectType.STASIS)) continue;

                float dx = enemy.getPos().x - landX;
                float dy = enemy.getPos().y - landY;
                if (dx * dx + dy * dy <= POISON_RADIUS * POISON_RADIUS) {
                    enemy.addEffect(ProjectileEffectType.POISONED, poisonDuration);
                    this.mgr.registerPoisonDot(realmId, enemy.getId(),
                            totalDamage, poisonDuration, playerId);
                    this.mgr.broadcastTextEffect(EntityType.ENEMY, enemy, TextEffect.DAMAGE, "POISONED");
                }
            }
        });
    }
}
