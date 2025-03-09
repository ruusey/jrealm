package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EffectType {
    INVISIBLE((short) 0), BUFFED((short) 1), PARALYZED((short) 2), STUNNED((short) 3), SPEEDY((short) 4),
    HEAL((short) 5), INVINCIBLE((short) 6), NONE((short) 8), TELEPORT((short) 9);

    public static Map<Short, EffectType> map = new HashMap<>();
    static {
        for (EffectType e : EffectType.values()) {
            EffectType.map.put((short) e.effectId, e);
        }
    }
    public short effectId;

    EffectType(short effectId) {
        this.effectId = effectId;
    }

    @JsonCreator
    public static EffectType valueOf(short effectId) {
        return EffectType.map.get(effectId);
    }
    
}
