package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Combined enum for projectile behavior flags and entity status effects.
 *
 * This enum contains TWO distinct categories — use the helpers to distinguish:
 *
 * <h3>Projectile Behavior Flags</h3>
 * Stored in {@code Projectile.flags} / {@code Bullet.flags}. Control how the
 * projectile moves. Checked via {@code bullet.hasFlag(...)}.
 * <ul>
 *   <li>{@link #PLAYER_PROJECTILE} — marks bullet as player-owned (not enemy)</li>
 *   <li>{@link #PARAMETRIC_PROJECTILE} — wavy/sinusoidal movement</li>
 *   <li>{@link #INVERTED_PARAMETRIC_PROJECTILE} — inverted wavy movement</li>
 *   <li>{@link #ORBITAL} — orbits around a fixed center point</li>
 * </ul>
 * See also: {@link ProjectileFlag} for the dedicated flag-only enum.
 *
 * <h3>Entity Status Effects</h3>
 * Stored in {@code Projectile.effects} / {@code Bullet.effects} as
 * {@link com.jrealm.game.model.ProjectileEffect} (effectId + duration).
 * Applied to entities on-hit via {@code entity.addEffect(...)}.
 * <ul>
 *   <li>{@link #HEALING}, {@link #PARALYZED}, {@link #STUNNED}, {@link #SPEEDY},
 *       {@link #INVINCIBLE}, {@link #DAZED}, {@link #DAMAGING}, {@link #STASIS},
 *       {@link #CURSED}, {@link #POISONED}, {@link #ARMORED}, {@link #BERSERK},
 *       {@link #SLOWED}</li>
 * </ul>
 * See also: {@link StatusEffect} for the dedicated effect-only enum.
 */
public enum ProjectileEffectType {
    // === Status Effects (applied on-hit to entities) ===
    INVISIBLE((short) 0), HEALING((short) 1), PARALYZED((short) 2), STUNNED((short) 3), SPEEDY((short) 4),
    HEAL((short) 5), INVINCIBLE((short) 6), NONE((short) 8), TELEPORT((short) 9),
    DAZED((short) 11), DAMAGING((short) 14),
    STASIS((short) 15), CURSED((short) 16), POISONED((short) 17),
    ARMORED((short) 18), BERSERK((short) 19), SLOWED((short) 21),

    // === Projectile Behavior Flags (control movement, NOT on-hit effects) ===
    PLAYER_PROJECTILE((short) 10),
    PARAMETRIC_PROJECTILE((short) 12),
    INVERTED_PARAMETRIC_PROJECTILE((short) 13),
    ORBITAL((short) 20);

    /** IDs that are projectile behavior flags, not entity status effects. */
    private static final Set<Short> FLAG_IDS = Set.of(
        PLAYER_PROJECTILE.effectId, PARAMETRIC_PROJECTILE.effectId,
        INVERTED_PARAMETRIC_PROJECTILE.effectId, ORBITAL.effectId
    );

    public static Map<Short, ProjectileEffectType> map = new HashMap<>();
    static {
        for (ProjectileEffectType e : ProjectileEffectType.values()) {
            ProjectileEffectType.map.put((short) e.effectId, e);
        }
    }
    public short effectId;

    ProjectileEffectType(short effectId) {
        this.effectId = effectId;
    }

    @JsonCreator
    public static ProjectileEffectType valueOf(short effectId) {
        return ProjectileEffectType.map.get(effectId);
    }

    /** Returns true if this is a projectile behavior flag (movement control), not an on-hit effect. */
    public boolean isFlag() {
        return FLAG_IDS.contains(this.effectId);
    }

    /** Returns true if this is an entity status effect (applied on-hit), not a behavior flag. */
    public boolean isStatusEffect() {
        return !isFlag();
    }
}
