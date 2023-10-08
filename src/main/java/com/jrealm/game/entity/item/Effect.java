package com.jrealm.game.entity.item;

import com.jrealm.game.contants.EffectType;

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
	private EffectType effectId;
	private long duration;
	private short mpCost;

}
