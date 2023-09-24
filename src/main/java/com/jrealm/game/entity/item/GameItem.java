package com.jrealm.game.entity.item;

import java.util.UUID;

import com.jrealm.game.model.SpriteModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
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

	public GameItem() {
		this.uid = UUID.randomUUID().toString();
	}

	@Override
	public GameItem clone() {
		GameItem.GameItemBuilder builder = GameItem.builder().itemId(this.itemId).uid(this.uid).name(this.name)
				.description(this.description).consumable(this.consumable).tier(this.tier).targetSlot(this.targetSlot)
				.targetClass(this.targetClass).fameBonus(this.fameBonus);

		if (this.damage != null) {
			builder = builder.damage(this.damage.clone());
		}

		if (this.stats != null) {
			builder = builder.stats(this.stats.clone());
		}

		GameItem itemFinal = builder.build();
		itemFinal.setAngleOffset(this.getAngleOffset());
		itemFinal.setRow(this.getRow());
		itemFinal.setCol(this.getCol());
		itemFinal.setSpriteKey(this.getSpriteKey());

		return itemFinal;
	}
}
