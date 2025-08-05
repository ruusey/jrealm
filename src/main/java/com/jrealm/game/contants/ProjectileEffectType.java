package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProjectileEffectType {
    INVISIBLE((short) 0), BUFFED((short) 1), PARALYZED((short) 2), STUNNED((short) 3), SPEEDY((short) 4),
    HEAL((short) 5), INVINCIBLE((short) 6), NONE((short) 8), TELEPORT((short) 9), 
    PLAYER_PROJECTILE((short) 10), DAZED((short) 11), PARAMETRIC_PROJECTILE((short) 12), 
    INVERTED_PARAMETRIC_PROJECTILE((short) 12);

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
    
}
