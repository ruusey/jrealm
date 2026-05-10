package com.openrealm.game.script;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.openrealm.game.data.GameDataManager;
import com.openrealm.game.entity.Enemy;
import com.openrealm.game.entity.Player;
import com.openrealm.game.math.Vector2f;
import com.openrealm.game.model.Projectile;
import com.openrealm.game.model.ProjectileGroup;
import com.openrealm.net.client.packet.CreateEffectPacket;
import com.openrealm.net.realm.Realm;
import com.openrealm.net.realm.RealmManagerServer;
import com.openrealm.util.WorkerThread;

import lombok.extern.slf4j.Slf4j;

/**
 * Inferno Demon (enemy 26) — admin-arena testbed boss. Layers a "grenade
 * grid" mechanic on top of the data-driven JSON attacks: every {@link
 * #BOMB_INTERVAL_MS} the boss lobs a grenade (assassin-style arc) at a
 * random cell of a 4x4 grid centered on its spawn point, and on impact
 * spawns a 16-projectile ring of shrapnel from {@link #SHRAPNEL_GROUP_ID}.
 *
 * Additive: the JSON-defined phase attacks (e.g. spiral ring in P1) keep
 * firing alongside this script.
 */
@Slf4j
public class Enemy26Script extends EnemyScriptBase {

    private static final int TARGET_ENEMY_ID = 26;
    private static final int SHRAPNEL_GROUP_ID = 301;

    /** Cadence between grenade throws while in the gated phase. */
    private static final long BOMB_INTERVAL_MS = 2000L;
    /** Lob travel time — same feel as the assassin poison vial. */
    private static final long THROW_DURATION_MS = 800L;
    /** 4x4 grid; cell-center offsets along each axis from boss center. */
    private static final float[] CELL_OFFSETS = { -300f, -100f, 100f, 300f };
    /** Number of shrapnel projectiles fired in a ring on impact. */
    private static final int SHRAPNEL_COUNT = 16;
    /** Phase name (in enemies.json) the grenade grid is gated to. */
    private static final String GATED_PHASE = "spiral_ring";

    /** Per-enemy last-bomb timestamp. Concurrent because attack() is invoked
     *  on a worker thread and multiple instances may share this script. */
    private final Map<Long, Long> lastBombByEnemyId = new ConcurrentHashMap<>();
    private final Random rng = new Random();

    public Enemy26Script(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public int getTargetEnemyId() {
        return TARGET_ENEMY_ID;
    }

    @Override
    public boolean isAdditive() {
        return true;
    }

    @Override
    public void attack(final Realm targetRealm, final Enemy enemy, final Player targetPlayer) throws Exception {
        // Only fire grenades in the gated phase. Other phases run pure JSON.
        final String phase = enemy.getLastPhaseName();
        if (phase == null || !GATED_PHASE.equals(phase)) return;

        final long now = System.currentTimeMillis();
        final Long lastBomb = lastBombByEnemyId.get(enemy.getId());
        if (lastBomb != null && (now - lastBomb) < BOMB_INTERVAL_MS) return;
        lastBombByEnemyId.put(enemy.getId(), now);

        final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(SHRAPNEL_GROUP_ID);
        if (group == null || group.getProjectiles() == null || group.getProjectiles().isEmpty()) {
            log.warn("[ENEMY26] shrapnel group {} missing — skipping grenade", SHRAPNEL_GROUP_ID);
            return;
        }
        final Projectile shrapnel = group.getProjectiles().get(0);

        // Boss center (geometric, not top-left). Grid is anchored here.
        final Vector2f bossCenter = enemy.getPos().clone(enemy.getSize() / 2f, enemy.getSize() / 2f);
        final int cellCol = rng.nextInt(CELL_OFFSETS.length);
        final int cellRow = rng.nextInt(CELL_OFFSETS.length);
        final float landX = bossCenter.x + CELL_OFFSETS[cellCol];
        final float landY = bossCenter.y + CELL_OFFSETS[cellRow];

        // Throw arc visual — same line-effect as the assassin poison vial.
        // The native client already renders this as a parabolic lob.
        this.getMgr().enqueueServerPacketToRealm(targetRealm, CreateEffectPacket.lineEffect(
                CreateEffectPacket.EFFECT_POISON_SPLASH,
                bossCenter.x, bossCenter.y, landX, landY,
                (short) THROW_DURATION_MS));

        final long realmId = targetRealm.getRealmId();

        // Schedule the impact. We intentionally use the worker pool rather
        // than blocking the tick thread; a missed realm at impact is OK
        // (we'd just no-op).
        WorkerThread.doAsync(() -> {
            try {
                Thread.sleep(THROW_DURATION_MS);
                final Realm r = Enemy26Script.this.getMgr().getRealms().get(realmId);
                if (r == null) return;

                // Impact splash — short-lived AoE pulse to read the landing.
                final float splashRadius = 100f;
                Enemy26Script.this.getMgr().enqueueServerPacketToRealm(r,
                        CreateEffectPacket.aoeEffect(
                                CreateEffectPacket.EFFECT_POISON_SPLASH,
                                landX, landY, splashRadius, (short) 350));

                // 16 shrapnel projectiles in an even ring from the impact point.
                final Vector2f impactPos = new Vector2f(landX, landY);
                final List<Short> flags = shrapnel.getFlags();
                for (int i = 0; i < SHRAPNEL_COUNT; i++) {
                    final float angle = (float) (i * 2.0 * Math.PI / SHRAPNEL_COUNT);
                    Enemy26Script.this.getMgr().addProjectile(
                            realmId, 0L, 0L,
                            shrapnel.getProjectileId(), SHRAPNEL_GROUP_ID,
                            impactPos.clone(), angle,
                            shrapnel.getSize(), shrapnel.getMagnitude(), shrapnel.getRange(),
                            shrapnel.getDamage(), true, flags,
                            shrapnel.getAmplitude(), shrapnel.getFrequency(), 0L);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("[ENEMY26] grenade impact failed", e);
            }
        });
    }
}
