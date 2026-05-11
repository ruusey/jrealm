package com.openrealm.game.contants;

/**
 * What an {@link com.openrealm.game.model.ability.AbilityScaling} contribution
 * is applied to on an Ability or PassiveAbility. Backed by string names so the
 * JSON data files stay readable; resolved to enum at load time.
 *
 * Keep in sync with the design doc (combat-rework.md §3.3) — every new target
 * needs the ability-resolution code path that consumes it.
 */
public enum ScalingTarget {
    /** Pre-mitigation damage roll added to the ability's base damage. */
    DAMAGE,
    /** Heal magnitude (for self-heals, ally heals, regen ticks). */
    HEAL,
    /** Shield / barrier HP. */
    SHIELD_AMOUNT,
    /** Duration (ms) of a STATUS_APPLY effect — requires {@code effectIndex}. */
    STATUS_DURATION_MS,
    /** Magnitude of a status (slow %, dmg-amp %, etc.) — requires {@code effectIndex}. */
    STATUS_MAGNITUDE,
    /** AoE radius in tiles. */
    RADIUS,
    /** Projectile / teleport range in tiles. */
    RANGE,
    /** Number of projectiles fired (rounded floor). */
    PROJECTILE_COUNT,
    /** Cooldown multiplier (0..1). Stacks additively across scalings; usually DEX-driven. */
    COOLDOWN_REDUCTION_MUL,
    /** Cast-time multiplier (0..1). Stacks additively across scalings; usually SPD-driven. */
    CAST_TIME_MUL,
    /** Passive proc chance (0..1). Passive-only. */
    PROC_CHANCE,
    /** Bonus damage multiplier on reflected projectiles (Knight's Deflect). Passive-only. */
    REFLECT_DAMAGE_MUL,
    /** Flat MP cost reduction. */
    MP_COST_REDUCTION,
    /** Channeled-ability duration in ms. */
    CHANNEL_DURATION_MS,
    /** Bonus damage % the party deals to a marked target (Archer's Hunter's Mark). */
    MARK_AMPLIFY_PCT,
    /** Inverse "every Nth attack" frequency for empowered-basic passives (Wizard's Arcane Surge). */
    EMPOWER_FREQUENCY,
    /** Catch-all for forward-compat — a scaling with UNKNOWN is ignored, not crashed on. */
    UNKNOWN;

    public static ScalingTarget parse(String s) {
        if (s == null) return UNKNOWN;
        try { return ScalingTarget.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return UNKNOWN; }
    }
}
