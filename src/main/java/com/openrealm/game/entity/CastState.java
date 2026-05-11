package com.openrealm.game.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Runtime state of an in-progress ability cast. Lives on {@link Player}; null
 * when the player is not casting. The server populates this on
 * {@code RealmManagerServer.useAbility()} for non-instant abilities and clears
 * it when the cast resolves (or gets canceled).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CastState {
    /** Id from abilities.json. */
    private int abilityId;
    /** Hotbar slot (0..3) the cast originated from — for cooldown bookkeeping. */
    private int slot;
    /** Server-tick millis when the cast started. */
    private long startTickMs;
    /** Server-tick millis when the cast resolves. */
    private long endTickMs;
    /** World-space target (ground-targeted abilities). 0/0 for non-targeted. */
    private float worldTargetX;
    private float worldTargetY;
    /** Whether incoming damage cancels the cast. Default: false (tank through). */
    private boolean cancelOnDamage;
}
