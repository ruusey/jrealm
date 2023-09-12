package com.jrealm.game.entity.item;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.UUID;

import com.jrealm.game.math.Vector2f;
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

	public void drawTooltip(Vector2f pos, int width, int height, Graphics2D g) {
		int spacing = 64;
		g.setColor(Color.GRAY);
		g.fillRect((int) pos.x, (int) pos.y, width, height);

		g.setColor(Color.WHITE);
		g.drawString(this.name, pos.x, pos.y + (0*spacing));
		g.drawString(this.description, pos.x, pos.y + (1 * spacing));

	}

}
