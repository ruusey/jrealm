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
    /** Lob travel time. Doubled from the assassin's 800ms so the player has
     *  a full second-and-a-half to read the red landing marker and clear out. */
    private static final long THROW_DURATION_MS = 1600L;
    /** 4x4 grid; cell-center offsets along each axis from boss center.
     *  Span is 384 px = a 12x12 tile square at 32 px/tile, kept tight around
     *  the boss so grenades stay in the engagement zone. */
    private static final float[] CELL_OFFSETS = { -192f, -64f, 64f, 192f };
    /** Number of shrapnel projectiles fired in a ring on impact. */
    private static final int SHRAPNEL_COUNT = 16;
    /** Visual radius of the red landing-warning marker during the throw.
     *  Sized to ~1.5 tile diameter (24 px radius at 32 px/tile) so it lines
     *  up exactly with the shrapnel range and reads as a tight, lethal
     *  blast zone. */
    private static final float WARNING_RADIUS = 24f;
    /** Visual radius of the impact splash on detonation — same as warning
     *  so the danger zone the player saw before the boom is exactly where
     *  the boom hits. */
    private static final float IMPACT_RADIUS = 24f;
    /** How long the impact splash sticks around. */
    private static final short IMPACT_DURATION_MS = 700;
    /** Tier sentinel passed in CreateEffectPacket so the native client paints
     *  the lob arc red instead of the default assassin-vial green. Anything
     *  >= 10 triggers the red palette in renderPoisonThrow. */
    private static final byte RED_GRENADE_TIER = 10;
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

        // Throw arc visual — re-uses the assassin's poison-vial line effect
        // for the parabolic lob on the client, but with tier=10 so the
        // native client's renderPoisonThrow paints the trail / blob red
        // instead of the default green.
        this.getMgr().enqueueServerPacketToRealm(targetRealm, CreateEffectPacket.lineEffect(
                CreateEffectPacket.EFFECT_POISON_SPLASH,
                bossCenter.x, bossCenter.y, landX, landY,
                (short) THROW_DURATION_MS, RED_GRENADE_TIER));

        // Red landing-zone warning — CURSE_RADIUS AoE that pulses on the
        // ground for the full throw duration. tier=10 sentinel triggers
        // the boss-grenade-specific renderer on the native client (much
        // higher fill opacity + pulsing outline) so the danger zone is
        // unmissable through the spiral / ground tile clutter.
        this.getMgr().enqueueServerPacketToRealm(targetRealm, CreateEffectPacket.aoeEffect(
                CreateEffectPacket.EFFECT_CURSE_RADIUS,
                landX, landY, WARNING_RADIUS, (short) THROW_DURATION_MS, RED_GRENADE_TIER));

        final long realmId = targetRealm.getRealmId();

        // Schedule the impact. We intentionally use the worker pool rather
        // than blocking the tick thread; a missed realm at impact is OK
        // (we'd just no-op).
        WorkerThread.doAsync(() -> {
            try {
                Thread.sleep(THROW_DURATION_MS);
                final Realm r = Enemy26Script.this.getMgr().getRealms().get(realmId);
                if (r == null) return;

                // Impact splash — same radius as the warning ring (so the
                // detonation lands exactly inside the zone the player was
                // told to clear) and same tier=10 high-opacity red renderer.
                Enemy26Script.this.getMgr().enqueueServerPacketToRealm(r,
                        CreateEffectPacket.aoeEffect(
                                CreateEffectPacket.EFFECT_CURSE_RADIUS,
                                landX, landY, IMPACT_RADIUS, IMPACT_DURATION_MS, RED_GRENADE_TIER));

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
