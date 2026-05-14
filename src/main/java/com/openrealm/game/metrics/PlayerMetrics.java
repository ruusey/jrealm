package com.openrealm.game.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import lombok.extern.slf4j.Slf4j;

/**
 * Per-character in-memory event counters. Each Player owns one of these;
 * gameplay code calls a record() method when an event happens, and the
 * persistence scheduler periodically drains the accumulated deltas via
 * {@link #drainAndReset()} and POSTs them to the data service for
 * lifetime $inc into the character's metrics document.
 *
 * Concurrency: every counter is a {@link LongAdder} so multiple threads
 * (realm tick + bullet update + admin command) can record without
 * serializing on a monitor. Drain reads `sumThenReset()` which is
 * lock-free.
 *
 * What's NOT stored here:
 *   - Absolute lifetime totals. The data service owns those; this class
 *     only buffers deltas since the last flush.
 *   - Anything that needs to survive a game-server crash; that ~12s
 *     window of events is the documented loss bound.
 *
 * See docs/player-metrics-design.md.
 */
@Slf4j
public final class PlayerMetrics {

    // ── Combat ──────────────────────────────────────────────────────
    private final LongAdder projectilesFired   = new LongAdder();
    private final LongAdder projectilesHit     = new LongAdder();
    private final LongAdder damageDealtTotal   = new LongAdder();
    private final LongAdder damageTakenTotal   = new LongAdder();
    private final LongAdder deaths             = new LongAdder();
    private final LongAdder killsTotal         = new LongAdder();
    private final LongAdder bossKills          = new LongAdder();
    /** enemyId → kill count. ConcurrentHashMap so we can computeIfAbsent
     *  safely from multiple threads, and LongAdder lets us increment a
     *  single bucket without locking the whole map. */
    private final Map<Short, LongAdder> killsByEnemyId = new ConcurrentHashMap<>();

    // ── Items ───────────────────────────────────────────────────────
    private final LongAdder hpPotionsDrank     = new LongAdder();
    private final LongAdder mpPotionsDrank     = new LongAdder();
    private final LongAdder itemsPickedUp      = new LongAdder();
    /** itemId → consume count. Same rationale as killsByEnemyId. */
    private final Map<Integer, LongAdder> itemsConsumedByItemId = new ConcurrentHashMap<>();

    // ── Progression ─────────────────────────────────────────────────
    private final LongAdder xpEarned           = new LongAdder();
    private final LongAdder xpFromKills        = new LongAdder();
    private final LongAdder skillPointsSpent   = new LongAdder();

    // ── Abilities ───────────────────────────────────────────────────
    private final Map<Integer, LongAdder> castsStartedByAbility    = new ConcurrentHashMap<>();
    private final Map<Integer, LongAdder> castsCompletedByAbility  = new ConcurrentHashMap<>();

    // ── Social ──────────────────────────────────────────────────────
    private final LongAdder tradesCompleted    = new LongAdder();
    /** otherCharacterUuid → trade count with that partner. */
    private final Map<String, LongAdder> tradePartners = new ConcurrentHashMap<>();
    private final LongAdder chatMessagesSent   = new LongAdder();

    // ── Recorders ───────────────────────────────────────────────────
    // Each method is a one-line LongAdder.increment or .add. Hot-path
    // safe; no locking, no allocation for scalar counters. Map-keyed
    // ones allocate a LongAdder on first miss for that key — amortized
    // cheap since the key set per character is small (~tens of distinct
    // enemy ids over a session).

    public void recordProjectileFired()              { projectilesFired.increment(); }
    public void recordProjectileHit()                { projectilesHit.increment(); }
    public void recordDamageDealt(int amount)        { if (amount > 0) damageDealtTotal.add(amount); }
    public void recordDamageTaken(int amount)        { if (amount > 0) damageTakenTotal.add(amount); }
    public void recordDeath()                        { deaths.increment(); }

    public void recordKill(short enemyId, boolean isBoss) {
        killsTotal.increment();
        if (isBoss) bossKills.increment();
        killsByEnemyId.computeIfAbsent(enemyId, k -> new LongAdder()).increment();
    }

    public void recordPotion(boolean isHp) {
        (isHp ? hpPotionsDrank : mpPotionsDrank).increment();
    }
    public void recordItemPickup()                   { itemsPickedUp.increment(); }
    public void recordItemConsumed(int itemId) {
        itemsConsumedByItemId.computeIfAbsent(itemId, k -> new LongAdder()).increment();
    }

    public void recordXp(long amount, boolean fromKill) {
        if (amount <= 0) return;
        xpEarned.add(amount);
        if (fromKill) xpFromKills.add(amount);
    }
    public void recordSkillPointSpent()              { skillPointsSpent.increment(); }

    public void recordCastStarted(int abilityId) {
        castsStartedByAbility.computeIfAbsent(abilityId, k -> new LongAdder()).increment();
    }
    public void recordCastCompleted(int abilityId) {
        castsCompletedByAbility.computeIfAbsent(abilityId, k -> new LongAdder()).increment();
    }

    public void recordTrade(String otherCharacterUuid) {
        tradesCompleted.increment();
        if (otherCharacterUuid != null && !otherCharacterUuid.isEmpty()) {
            tradePartners.computeIfAbsent(otherCharacterUuid, k -> new LongAdder()).increment();
        }
    }
    public void recordChatMessage()                  { chatMessagesSent.increment(); }

    // ── Drain ───────────────────────────────────────────────────────
    /**
     * Snapshot every counter into a {@link MetricsDelta} and zero them
     * atomically. Called by the persistence scheduler each flush window.
     * If the resulting delta is empty (nothing happened since the last
     * drain) returns null so callers can skip the HTTP round-trip.
     */
    public MetricsDelta drainAndReset() {
        final MetricsDelta d = new MetricsDelta();

        d.projectilesFired   = projectilesFired.sumThenReset();
        d.projectilesHit     = projectilesHit.sumThenReset();
        d.damageDealtTotal   = damageDealtTotal.sumThenReset();
        d.damageTakenTotal   = damageTakenTotal.sumThenReset();
        d.deaths             = deaths.sumThenReset();
        d.killsTotal         = killsTotal.sumThenReset();
        d.bossKills          = bossKills.sumThenReset();
        d.killsByEnemyId     = drainShortMap(killsByEnemyId);

        d.hpPotionsDrank     = hpPotionsDrank.sumThenReset();
        d.mpPotionsDrank     = mpPotionsDrank.sumThenReset();
        d.itemsPickedUp      = itemsPickedUp.sumThenReset();
        d.itemsConsumedByItemId = drainIntMap(itemsConsumedByItemId);

        d.xpEarned           = xpEarned.sumThenReset();
        d.xpFromKills        = xpFromKills.sumThenReset();
        d.skillPointsSpent   = skillPointsSpent.sumThenReset();

        d.castsStartedByAbility   = drainIntMap(castsStartedByAbility);
        d.castsCompletedByAbility = drainIntMap(castsCompletedByAbility);

        d.tradesCompleted    = tradesCompleted.sumThenReset();
        d.tradePartners      = drainStringMap(tradePartners);
        d.chatMessagesSent   = chatMessagesSent.sumThenReset();

        return d.isEmpty() ? null : d;
    }

    /**
     * Re-apply a delta to the counters. Used when an HTTP flush fails:
     * we hand the delta back so the next window's events stack onto it
     * instead of losing the work.
     */
    public void mergeBack(MetricsDelta d) {
        if (d == null) return;
        projectilesFired.add(d.projectilesFired);
        projectilesHit.add(d.projectilesHit);
        damageDealtTotal.add(d.damageDealtTotal);
        damageTakenTotal.add(d.damageTakenTotal);
        deaths.add(d.deaths);
        killsTotal.add(d.killsTotal);
        bossKills.add(d.bossKills);
        if (d.killsByEnemyId != null) {
            for (Map.Entry<Short, Long> e : d.killsByEnemyId.entrySet()) {
                killsByEnemyId.computeIfAbsent(e.getKey(), k -> new LongAdder()).add(e.getValue());
            }
        }
        hpPotionsDrank.add(d.hpPotionsDrank);
        mpPotionsDrank.add(d.mpPotionsDrank);
        itemsPickedUp.add(d.itemsPickedUp);
        mergeIntMap(itemsConsumedByItemId, d.itemsConsumedByItemId);
        xpEarned.add(d.xpEarned);
        xpFromKills.add(d.xpFromKills);
        skillPointsSpent.add(d.skillPointsSpent);
        mergeIntMap(castsStartedByAbility, d.castsStartedByAbility);
        mergeIntMap(castsCompletedByAbility, d.castsCompletedByAbility);
        tradesCompleted.add(d.tradesCompleted);
        mergeStringMap(tradePartners, d.tradePartners);
        chatMessagesSent.add(d.chatMessagesSent);
    }

    private static Map<Short, Long> drainShortMap(Map<Short, LongAdder> src) {
        if (src.isEmpty()) return null;
        final Map<Short, Long> out = new HashMap<>(src.size());
        for (Map.Entry<Short, LongAdder> e : src.entrySet()) {
            final long v = e.getValue().sumThenReset();
            if (v != 0) out.put(e.getKey(), v);
        }
        return out.isEmpty() ? null : out;
    }
    private static Map<Integer, Long> drainIntMap(Map<Integer, LongAdder> src) {
        if (src.isEmpty()) return null;
        final Map<Integer, Long> out = new HashMap<>(src.size());
        for (Map.Entry<Integer, LongAdder> e : src.entrySet()) {
            final long v = e.getValue().sumThenReset();
            if (v != 0) out.put(e.getKey(), v);
        }
        return out.isEmpty() ? null : out;
    }
    private static Map<String, Long> drainStringMap(Map<String, LongAdder> src) {
        if (src.isEmpty()) return null;
        final Map<String, Long> out = new HashMap<>(src.size());
        for (Map.Entry<String, LongAdder> e : src.entrySet()) {
            final long v = e.getValue().sumThenReset();
            if (v != 0) out.put(e.getKey(), v);
        }
        return out.isEmpty() ? null : out;
    }
    private static void mergeIntMap(Map<Integer, LongAdder> dst, Map<Integer, Long> src) {
        if (src == null) return;
        for (Map.Entry<Integer, Long> e : src.entrySet()) {
            dst.computeIfAbsent(e.getKey(), k -> new LongAdder()).add(e.getValue());
        }
    }
    private static void mergeStringMap(Map<String, LongAdder> dst, Map<String, Long> src) {
        if (src == null) return;
        for (Map.Entry<String, Long> e : src.entrySet()) {
            dst.computeIfAbsent(e.getKey(), k -> new LongAdder()).add(e.getValue());
        }
    }
}
