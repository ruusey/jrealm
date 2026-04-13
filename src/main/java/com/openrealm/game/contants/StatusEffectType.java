package com.openrealm.game.contants;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Status effects applied to entities (players, enemies) on-hit or from abilities.
 *
 * Also contains projectile behavior flag IDs for backwards compatibility with
 * data files that store both in the same numeric space. Use {@link #isFlag()}
 * to distinguish, or use {@link ProjectileFlag} directly for bullet behavior.
 *
 * <h3>Entity Status Effects</h3>
 * Applied via {@code entity.addEffect(type, duration)}. Stored in
 * {@code Projectile.effects} as {@link com.openrealm.game.model.ProjectileEffect}.
 *
 * <h3>Projectile Behavior Flags (for compat only)</h3>
 * Stored in {@code Projectile.flags} / {@code Bullet.flags}.
 * Prefer using {@link ProjectileFlag} enum for flag checks on bullets.
 */
public enum StatusEffectType {
    // === Entity Status Effects ===
    INVISIBLE((short) 0),
    HEALING((short) 1),
    PARALYZED((short) 2),
    STUNNED((short) 3),
    SPEEDY((short) 4),
    HEAL((short) 5),
    INVINCIBLE((short) 6),
    NONE((short) 8),
    TELEPORT((short) 9),
    DAZED((short) 11),
    DAMAGING((short) 14),
    STASIS((short) 15),
    CURSED((short) 16),
    POISONED((short) 17),
    ARMORED((short) 18),
    BERSERK((short) 19),
    SLOWED((short) 21),
    ARMOR_BROKEN((short) 22),

    // === Projectile Behavior Flags (prefer ProjectileFlag enum) ===
    PLAYER_PROJECTILE((short) 10),
    PARAMETRIC_PROJECTILE((short) 12),
    INVERTED_PARAMETRIC_PROJECTILE((short) 13),
    ORBITAL((short) 20);

    private static final Set<Short> FLAG_IDS = Set.of(
        PLAYER_PROJECTILE.effectId, PARAMETRIC_PROJECTILE.effectId,
        INVERTED_PARAMETRIC_PROJECTILE.effectId, ORBITAL.effectId
    );

    public static Map<Short, StatusEffectType> map = new HashMap<>();
    static {
        for (StatusEffectType e : StatusEffectType.values()) {
            map.put(e.effectId, e);
        }
    }

    public short effectId;

    StatusEffectType(short effectId) {
        this.effectId = effectId;
    }

    @JsonCreator
    public static StatusEffectType valueOf(short effectId) {
        return map.get(effectId);
    }

    /** Returns true if this is a projectile behavior flag, not an entity status effect. */
    public boolean isFlag() {
        return FLAG_IDS.contains(this.effectId);
    }

    /** Returns true if this is an entity status effect, not a projectile behavior flag. */
    public boolean isStatusEffect() {
        return !isFlag();
    }
}
