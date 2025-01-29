package com.jrealm.game.entity.item;

import com.jrealm.game.contants.EffectType;
import com.jrealm.net.core.nettypes.game.SerializableEffect;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Effect extends SerializableEffect {
    private boolean self;
    private EffectType effectId;
    private long duration;
    private long cooldownDuration;
    private short mpCost;


}
