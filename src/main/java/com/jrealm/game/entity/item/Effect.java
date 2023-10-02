package com.jrealm.game.entity.item;

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
	private short effectId;
	private long duration;
	private short mpCost;

}
