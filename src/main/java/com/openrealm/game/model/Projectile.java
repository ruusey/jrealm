package com.openrealm.game.model;

import java.util.List;

import com.openrealm.game.contants.ProjectilePositionMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projectile {
    private int projectileId;
    private ProjectilePositionMode positionMode;
    private String angle;
    private short range;
    private float magnitude;
    private short size;
    private short damage;

    private short amplitude;
    private short frequency;

    // Spawn position offset relative to enemy center (rotated by firing angle)
    private float spawnOffsetX;
    private float spawnOffsetY;

    // Delay in ms before this projectile spawns within its group (stagger effect)
    private int spawnDelayMs;

    /**
     * Projectile behavior flags — control HOW the projectile moves/behaves.
     * Values are {@link ProjectileFlag} IDs: PLAYER_PROJECTILE(10), PARAMETRIC(12),
     * INVERTED_PARAMETRIC(13), ORBITAL(20). NOT status effects.
     */
    private List<Short> flags;
    /**
     * On-hit status effects — applied to the target entity when this projectile hits.
     * Each entry has an effectId ({@link StatusEffect}) and a duration in ms.
     * NOT behavior flags — those go in {@link #flags}.
     */
    private List<ProjectileEffect> effects;

    public boolean hasFlag(short flag) {
        return (this.flags != null) && this.flags.contains(flag);
    }

}
