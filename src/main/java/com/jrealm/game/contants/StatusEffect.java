package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Status effects that can be applied to entities (players, enemies).
 * Applied on-hit from a projectile's {@code effects} list, or from ability use.
 * These are NOT projectile behavior flags — see {@link ProjectileFlag} for flags
 * that control how a projectile moves (parametric, orbital, etc.).
 */
public enum StatusEffect {
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
    SLOWED((short) 21);

    public static final Map<Short, StatusEffect> map = new HashMap<>();
    static {
        for (StatusEffect e : StatusEffect.values()) {
            map.put(e.effectId, e);
        }
    }

    public final short effectId;

    StatusEffect(short effectId) {
        this.effectId = effectId;
    }

    @JsonCreator
    public static StatusEffect valueOf(short effectId) {
        return map.get(effectId);
    }
}
