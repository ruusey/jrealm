package com.jrealm.game.entity.item;

import com.jrealm.game.contants.StatusEffectType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Effect {
    private boolean self;
    private StatusEffectType effectId;
    private long duration;
    private long cooldownDuration;
    private short mpCost;
}
