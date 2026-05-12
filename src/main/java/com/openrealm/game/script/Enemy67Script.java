package com.openrealm.game.script;

import com.openrealm.game.contants.EntityType;
import com.openrealm.game.contants.TextEffect;
import com.openrealm.game.entity.Enemy;
import com.openrealm.game.entity.Player;
import com.openrealm.game.math.Vector2f;
import com.openrealm.net.client.packet.CreateEffectPacket;
import com.openrealm.net.realm.Realm;
import com.openrealm.net.realm.RealmManagerServer;

import lombok.extern.slf4j.Slf4j;

/**
 * Healer NPC (Enemy 67) — friendly aura that restores HP/MP to every player
 * within {@link #HEAL_RADIUS}. Runs on the per-tick {@link #tick} hook (NOT
 * the hostile {@code attack} path) so it ignores attackRange, DEX cooldown,
 * AI-tick stagger, closest-player targeting, and the player's INVISIBLE /
 * STASIS effects. The instant a player with missing HP or MP enters the
 * radius, they tick.
 */
@Slf4j
public class Enemy67Script extends EnemyScriptBase {

    private static final int HEAL_AMOUNT = 69;
    /** 2 tiles (BASE_TILE_SIZE * 2 = 64px). */
    private static final float HEAL_RADIUS = 64.0f;
    private static final float HEAL_RADIUS_SQ = HEAL_RADIUS * HEAL_RADIUS;
    /** Pool radius around the statue — sized to sit on a water-tile pool a
     *  mapper would place under it, NOT the heal AoE. */
    private static final float FOUNTAIN_RADIUS = 72.0f;
    /** One full ~1.6s loop; consecutive heal ticks overlap so the stream
     *  reads as continuous on the client. */
    private static final short FOUNTAIN_DURATION_MS = 1600;
    /** Heal application cadence. 10 Hz feels instant on entry (≤100 ms
     *  to first tick) and cuts the +heal floater + PlayerStatePacket
     *  bandwidth 6× vs running at the full 64 Hz tick rate. */
    private static final long HEAL_INTERVAL_MS = 100L;
    /** AoE CreateEffect cadence — throttled separately from the heal so
     *  particle density stays sane on the client. */
    private static final long VFX_INTERVAL_MS = 400L;

    private long lastHealMs = 0L;
    private long lastVfxBroadcastMs = 0L;

    public Enemy67Script(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public int getTargetEnemyId() {
        return 67;
    }

    /** Healer has no hostile attack — heal logic lives in {@link #tick}. */
    @Override
    public void attack(final Realm targetRealm, final Enemy enemy, final Player targetPlayer) throws Exception {
        // intentionally empty
    }

    @Override
    public void tick(final Realm targetRealm, final Enemy enemy) throws Exception {
        if (targetRealm == null) return;
        // Per-healer 10 Hz gate. The realm tick loop calls us at 64 Hz; the
        // first qualifying tick after entry fires within 100 ms (effectively
        // immediate), and subsequent ticks throttle the +heal text floater
        // and PlayerStatePacket churn. This is NOT a DEX-based cooldown —
        // it has nothing to do with attackRange or closest-player gating.
        final long now = System.currentTimeMillis();
        if (now - this.lastHealMs < HEAL_INTERVAL_MS) return;
        this.lastHealMs = now;

        final Vector2f center = enemy.getPos().clone(enemy.getSize() / 2, enemy.getSize() / 2);

        // Spatial-radius query — O(k) on the cells covering the 64 px AoE
        // instead of O(n) over every player in the realm. Critical in
        // nexus where 40+ players sit far outside the heal radius.
        final Player[] nearby = targetRealm.getPlayersInRadiusFast(center, HEAL_RADIUS);
        if (nearby.length == 0) return;

        boolean anyHealed = false;
        for (final Player player : nearby) {
            final float dx = player.getPos().x - center.x;
            final float dy = player.getPos().y - center.y;
            if (dx * dx + dy * dy > HEAL_RADIUS_SQ) continue;

            final int maxHp = player.getComputedStats().getHp();
            final int missingHp = maxHp - player.getHealth();
            if (missingHp > 0) {
                final int toHeal = Math.min(HEAL_AMOUNT, missingHp);
                player.setHealth(player.getHealth() + toHeal);
                this.getMgr().broadcastTextEffect(EntityType.PLAYER, player, TextEffect.HEAL, "+" + toHeal);
                anyHealed = true;
            }

            final int maxMp = player.getComputedStats().getMp();
            final int missingMp = maxMp - player.getMana();
            if (missingMp > 0) {
                final int toRestore = Math.min(HEAL_AMOUNT, missingMp);
                player.setMana(player.getMana() + toRestore);
                this.getMgr().broadcastTextEffect(EntityType.PLAYER, player, TextEffect.HEAL, "+" + toRestore + " MP");
                anyHealed = true;
            }
        }

        // Only broadcast the AoE visuals when at least one player actually
        // received a tick — otherwise an empty healer would spam two
        // CreateEffect packets per tick to every nearby viewer for no
        // visible benefit. Throttle to VFX_INTERVAL_MS so the particles
        // don't stack into a solid wall on the client.
        if (anyHealed) {
            if (now - this.lastVfxBroadcastMs >= VFX_INTERVAL_MS) {
                this.lastVfxBroadcastMs = now;
                this.getMgr().enqueueServerPacketToRealm(targetRealm, CreateEffectPacket.aoeEffect(
                        CreateEffectPacket.EFFECT_HEAL_RADIUS, center.x, center.y, HEAL_RADIUS, (short) 1500));
                this.getMgr().enqueueServerPacketToRealm(targetRealm, CreateEffectPacket.aoeEffect(
                        CreateEffectPacket.EFFECT_WATER_FOUNTAIN, center.x, center.y,
                        FOUNTAIN_RADIUS, FOUNTAIN_DURATION_MS));
            }
        }
    }
}
