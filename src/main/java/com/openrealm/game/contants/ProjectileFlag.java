package com.openrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

/**
 * Flags that control projectile movement behavior. Stored in the projectile's
 * {@code flags} list. These are NOT on-hit effects — see {@link StatusEffect}
 * for status effects applied when a projectile hits a target.
 */
public enum ProjectileFlag {
    PLAYER_PROJECTILE((short) 10),
    PARAMETRIC((short) 12),
    INVERTED_PARAMETRIC((short) 13),
    ORBITAL((short) 20),
    ARMOR_PIERCING((short) 23),
    /** Projectile passes through walls and collision tiles without being destroyed. */
    PASS_THROUGH_TERRAIN((short) 24),
    /**
     * Projectile pierces enemies — applies damage to each enemy it overlaps and
     * keeps flying. Per-enemy de-dup is still enforced via Realm.hasHitEnemy()
     * so a single bullet can't damage the same enemy twice. Used for bows,
     * archer quivers, and knight stun shields.
     */
    PASS_THROUGH_ENEMIES((short) 25);

    public static final Map<Short, ProjectileFlag> map = new HashMap<>();
    static {
        for (ProjectileFlag f : ProjectileFlag.values()) {
            map.put(f.flagId, f);
        }
    }

    public final short flagId;

    ProjectileFlag(short flagId) {
        this.flagId = flagId;
    }

    public static ProjectileFlag valueOf(short flagId) {
        return map.get(flagId);
    }
}
