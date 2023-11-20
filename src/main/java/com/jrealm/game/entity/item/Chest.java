package com.jrealm.game.entity.item;

import java.awt.Graphics2D;
import java.util.UUID;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.Vector2f;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class Chest extends LootContainer {
	private boolean visible = true;
	public Chest(Vector2f pos) {
		super(GameDataManager.SPRITE_SHEETS.get("entity/rotmg-projectiles.png").getSprite(2, 0, 8, 8), pos);
		this.setUid(UUID.randomUUID().toString());
	}

	public Chest(Vector2f pos, GameItem loot) {
		super(pos, loot);
		this.setUid(UUID.randomUUID().toString());
		super.setSprite(GameDataManager.SPRITE_SHEETS.get("entity/rotmg-projectiles.png").getSprite(2, 0, 8, 8));
	}
	
	public Chest(LootContainer c) {
		super(c.getPos(), c.getItems());
		this.setLootContainerId(c.getLootContainerId());
		this.visible = true;
	}

	public void addItemAtFirtIdx(GameItem item) {
		int idx = -1;
		for(int i = 0 ; i< this.getItems().length; i++) {
			if(super.getItems()[i]==null) {
				idx = i;
			}
		}
		if (idx > -1) {
			super.setItem(idx, item);
		}
	}

	@Override
	public boolean isExpired() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void render(Graphics2D g) {
		super.render(g);
	}
	
	@Override
	public String toString() {
		return (this.getLootContainerId()+" "+this.getPos()+" isChest="+(this instanceof Chest));
	}

}
