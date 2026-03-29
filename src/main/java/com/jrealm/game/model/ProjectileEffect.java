package com.jrealm.game.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An on-hit status effect applied by a projectile when it hits a target.
 * Separate from projectile flags (PARAMETRIC, PLAYER_PROJECTILE, etc.)
 * which control projectile behavior, not hit effects.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectileEffect {
    private short effectId;  // ProjectileEffectType ordinal
    private long duration;   // milliseconds
}
