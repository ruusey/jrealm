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
    /** Lob travel time. 3.2s gives the player generous reaction window to
     *  read the red landing marker and clear out before the bomb lands. */
    private static final long THROW_DURATION_MS = 2100L;
    /** Once the bomb has landed it sits on the ground for this long before
     *  detonating. Lets a second-look dodge clear out. */
    private static final long LAND_DELAY_MS = 1000L;
    /** Total pre-explode duration (warning ring stays up the whole time). */
    private static final short WARNING_DURATION_MS = (short) (THROW_DURATION_MS + LAND_DELAY_MS);
    /** 4x4 grid; cell-center offsets along each axis from boss center.
     *  Span is 384 px = a 12x12 tile square at 32 px/tile, kept tight around
     *  the boss so grenades stay in the engagement zone. */
    private static final float[] CELL_OFFSETS = { -192f, -64f, 64f, 192f };
    /** Number of shrapnel projectiles fired in a ring on impact. */
    private static final int SHRAPNEL_COUNT = 8;
    /** Visual radius of the red landing marker during the throw.
     *  Small (~0.75 tile diameter) so it's a "the bomb lands HERE" point
     *  marker rather than covering the explosion area — the explosion
     *  area is read by watching the shrapnel actually fly out. */
    private static final float WARNING_RADIUS = 24f;
    /** Impact splash on detonation — same small size as the landing marker
     *  so the bullets that fan out are clearly visible above it. */
    private static final float IMPACT_RADIUS = 24f;
    /** "The bomb has landed" indicator during the LAND_DELAY_MS window
     *  before it explodes. Slightly more solid than the warning. */
    private static final float BOMB_ON_GROUND_RADIUS = 14f;
    /** How long the impact splash sticks around. */
    private static final short IMPACT_DURATION_MS = 700;
    /** Tier sentinel passed in CreateEffectPacket so the native client paints
     *  the lob arc red instead of the default assassin-vial green. Anything
     *  >= 10 triggers the red palette in renderPoisonThrow / renderAoeEffect. */
    private static final byte RED_GRENADE_TIER = 10;
    /** Phase name (in enemies.json) the grenade grid is gated to.
     *  null = no gate (grenades thrown in every phase). */
    private static final String GATED_PHASE = null;

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
        // Optional phase gate. When GATED_PHASE is null, grenades throw
        // in every phase (current intent); set it to a phase name to
        // restrict to a single phase later if needed.
        if (GATED_PHASE != null) {
            final String phase = enemy.getLastPhaseName();
            if (phase == null || !GATED_PHASE.equals(phase)) return;
        }

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
        // ground for the entire pre-explode window (throw + 1s land delay).
        // tier=10 sentinel triggers the boss-grenade renderer on the native
        // client (~85% fill opacity + pulsing outline) so the danger zone
        // is unmissable through spiral arms / ground clutter.
        this.getMgr().enqueueServerPacketToRealm(targetRealm, CreateEffectPacket.aoeEffect(
                CreateEffectPacket.EFFECT_CURSE_RADIUS,
                landX, landY, WARNING_RADIUS, WARNING_DURATION_MS, RED_GRENADE_TIER));

        final long realmId = targetRealm.getRealmId();

        // Schedule the impact. We intentionally use the worker pool rather
        // than blocking the tick thread; a missed realm at impact is OK
        // (we'd just no-op).
        WorkerThread.doAsync(() -> {
            try {
                // Phase 1: wait for the lob to land.
                Thread.sleep(THROW_DURATION_MS);
                final Realm r = Enemy26Script.this.getMgr().getRealms().get(realmId);
                if (r == null) return;

                // Phase 2: bomb sits on the ground. Render a small solid
                // red dot at the landing point — the "bomb has landed,
                // it's about to go off" tell. The big warning ring is
                // still up around it for the same duration.
                Enemy26Script.this.getMgr().enqueueServerPacketToRealm(r,
                        CreateEffectPacket.aoeEffect(
                                CreateEffectPacket.EFFECT_CURSE_RADIUS,
                                landX, landY, BOMB_ON_GROUND_RADIUS, (short) LAND_DELAY_MS, RED_GRENADE_TIER));

                // Phase 3: detonation delay — 1 sec for last-second dodge.
                Thread.sleep(LAND_DELAY_MS);

                // Phase 4: explosion.
                // Impact splash matches the warning ring radius so the
                // boom lands exactly in the zone the player was told to
                // clear.
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
