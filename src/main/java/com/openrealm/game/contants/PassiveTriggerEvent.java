package com.openrealm.game.contants;

/**
 * When a PassiveAbility's trigger fires. The combat tick / damage / cast
 * pipelines all check passive triggers against the relevant events on the
 * acting player and any party members in range.
 *
 * See design doc §3.2.
 */
public enum PassiveTriggerEvent {
    /** Incoming projectile struck self. Knight's Deflect. */
    ON_PROJECTILE_HIT_SELF,
    /** Self fired a basic attack. Wizard's Arcane Surge. */
    ON_BASIC_ATTACK,
    /** Self cast any active ability. */
    ON_ABILITY_CAST,
    /** Self killed an enemy. */
    ON_KILL,
    /** Self took damage from any source. */
    ON_TAKE_DAMAGE,
    /** Self received heal from any source. */
    ON_HEAL_RECEIVED,
    /** Periodic — fires every {@code tickMs} (aura-style). */
    ON_TICK,
    /** Self HP crossed a band (fires once per crossing). */
    ON_HP_THRESHOLD,
    /** A party member cast an ability. */
    ON_ALLY_CAST,
    /** A party member got a kill nearby. */
    ON_ALLY_KILL,
    UNKNOWN;

    public static PassiveTriggerEvent parse(String s) {
        if (s == null) return UNKNOWN;
        try { return PassiveTriggerEvent.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return UNKNOWN; }
    }
}
