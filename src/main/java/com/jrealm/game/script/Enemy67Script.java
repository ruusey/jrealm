package com.jrealm.game.script;

import java.util.Arrays;

import com.jrealm.game.contants.EntityType;
import com.jrealm.game.contants.ProjectileEffectType;
import com.jrealm.game.contants.TextEffect;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.net.client.packet.CreateEffectPacket;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

/**
 * Healer NPC (Enemy 67) — friendly entity that heals nearby players
 * and spawns orbiting orbs around itself.
 * Spawns in the vault via static spawns. Does not deal damage.
 */
public class Enemy67Script extends EnemyScriptBase {

    private static final int HEAL_AMOUNT = 80;
    private static final float HEAL_RADIUS = 224.0f;
    private static final long HEAL_DURATION = 3000;

    // Orbital orb config
    private static final int ORB_COUNT = 9;
    private static final float ORB_ORBIT_RADIUS = 48.0f;
    private static final short ORB_FREQUENCY = 3;  // degrees per tick — slow orbit
    private static final float ORB_RANGE = 9999.0f; // long-lived
    private static final int ORB_PROJECTILE_GROUP_ID = 106; // projectile group for the orb sprite

    public Enemy67Script(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public int getTargetEnemyId() {
        return 67;
    }

    @Override
    public void attack(final Realm targetRealm, final Enemy enemy, final Player targetPlayer) throws Exception {
        final Vector2f center = enemy.getPos().clone(enemy.getSize() / 2, enemy.getSize() / 2);

        // Broadcast heal radius visual
        this.getMgr().enqueueServerPacket(CreateEffectPacket.aoeEffect(
                CreateEffectPacket.EFFECT_HEAL_RADIUS, center.x, center.y, HEAL_RADIUS, (short) 1500));

        // Heal all players within range
        final float radiusSq = HEAL_RADIUS * HEAL_RADIUS;
        for (final Player player : targetRealm.getPlayers().values()) {
            float dx = player.getPos().x - center.x;
            float dy = player.getPos().y - center.y;
            if (dx * dx + dy * dy > radiusSq) continue;

            player.addEffect(ProjectileEffectType.HEALING, HEAL_DURATION);
            int maxHp = player.getComputedStats().getHp();
            int missing = maxHp - player.getHealth();
            if (missing > 0) {
                int toHeal = Math.min(HEAL_AMOUNT, missing);
                player.setHealth(player.getHealth() + toHeal);
                this.getMgr().broadcastTextEffect(EntityType.PLAYER, player, TextEffect.HEAL, "+" + toHeal);
            }
        }

        // Spawn 9 orbital orbs around the healer (if not already orbiting)
        // Only spawn if there aren't already orbs from this enemy
        long existingOrbs = targetRealm.getBullets().values().stream()
                .filter(b -> b.getSrcEntityId() == enemy.getId() && b.hasFlag(ProjectileEffectType.ORBITAL))
                .count();
        if (existingOrbs < ORB_COUNT) {
            spawnOrbitalOrbs(targetRealm, enemy, targetPlayer);
        }
    }

    private void spawnOrbitalOrbs(final Realm targetRealm, final Enemy enemy, final Player targetPlayer) {
        final Vector2f center = enemy.getPos().clone(enemy.getSize() / 2, enemy.getSize() / 2);
        final java.util.List<Short> orbFlags = Arrays.asList(
                ProjectileEffectType.ORBITAL.effectId
        );

        for (int i = 0; i < ORB_COUNT; i++) {
            float startPhase = (float) (i * 2 * Math.PI / ORB_COUNT);
            // Spawn position on the orbit circle
            Vector2f spawnPos = new Vector2f(
                    center.x + ORB_ORBIT_RADIUS * (float) Math.cos(startPhase),
                    center.y + ORB_ORBIT_RADIUS * (float) Math.sin(startPhase));

            Bullet orb = this.getMgr().addProjectile(
                    targetRealm.getRealmId(), 0L, targetPlayer.getId(),
                    ORB_PROJECTILE_GROUP_ID, 0, spawnPos, startPhase,
                    (short) 16, 0f, ORB_RANGE, (short) 0, false,
                    orbFlags, (short) ORB_ORBIT_RADIUS, ORB_FREQUENCY, enemy.getId());
            if (orb != null) {
                orb.setupOrbital(center.x, center.y, ORB_ORBIT_RADIUS, startPhase);
            }
        }
    }
}
