package com.jrealm.game.entity.item;

import java.util.UUID;

import com.jrealm.game.model.SpriteModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameItem extends SpriteModel {
	private int itemId;
	@Builder.Default
	private String uid = UUID.randomUUID().toString();
	private String name;
	private String description;
	private Stats stats;
	private Damage damage;
	private boolean consumable;
	private byte tier;
	private byte targetSlot;
	private byte targetClass;
	private byte fameBonus;

}
