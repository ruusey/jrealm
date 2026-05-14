package com.openrealm.game.metrics;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Serialization-friendly snapshot of one flush window's counter values
 * for a single character. The game server produces this via
 * {@link PlayerMetrics#drainAndReset()} and POSTs it to the data
 * service, which applies it as a Mongo {@code $inc} update against the
 * character's lifetime metrics document.
 *
 * Why every map is nullable: most flush windows have only a handful of
 * non-zero map entries, so empty maps would just bloat the payload.
 * `null` means "no entries to apply for this group"; the data service
 * skips the $inc when the map is null.
 *
 * Field shape mirrors {@code CharacterMetricsEntity} on the data
 * service. Adding a new counter is two edits: a field here, and the
 * matching $inc in the service. Documents without the new field
 * default to 0 on read.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDelta {

    // Combat
    public long projectilesFired;
    public long projectilesHit;
    public long damageDealtTotal;
    public long damageTakenTotal;
    public long deaths;
    public long killsTotal;
    public long bossKills;
    public Map<Short, Long> killsByEnemyId;

    // Items
    public long hpPotionsDrank;
    public long mpPotionsDrank;
    public long itemsPickedUp;
    public Map<Integer, Long> itemsConsumedByItemId;

    // Progression
    public long xpEarned;
    public long xpFromKills;
    public long skillPointsSpent;

    // Abilities
    public Map<Integer, Long> castsStartedByAbility;
    public Map<Integer, Long> castsCompletedByAbility;

    // Social
    public long tradesCompleted;
    public Map<String, Long> tradePartners;
    public long chatMessagesSent;

    /**
     * True if no events were recorded this window — caller can skip the
     * HTTP round-trip entirely.
     */
    public boolean isEmpty() {
        return projectilesFired == 0
            && projectilesHit == 0
            && damageDealtTotal == 0
            && damageTakenTotal == 0
            && deaths == 0
            && killsTotal == 0
            && bossKills == 0
            && (killsByEnemyId == null || killsByEnemyId.isEmpty())
            && hpPotionsDrank == 0
            && mpPotionsDrank == 0
            && itemsPickedUp == 0
            && (itemsConsumedByItemId == null || itemsConsumedByItemId.isEmpty())
            && xpEarned == 0
            && xpFromKills == 0
            && skillPointsSpent == 0
            && (castsStartedByAbility == null || castsStartedByAbility.isEmpty())
            && (castsCompletedByAbility == null || castsCompletedByAbility.isEmpty())
            && tradesCompleted == 0
            && (tradePartners == null || tradePartners.isEmpty())
            && chatMessagesSent == 0;
    }
}
