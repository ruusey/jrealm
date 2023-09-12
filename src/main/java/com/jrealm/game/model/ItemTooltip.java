package com.jrealm.game.model;

import java.awt.Color;
import java.awt.Graphics2D;

import com.jrealm.game.GamePanel;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemTooltip {
	private Vector2f pos;
	private int width;
	private int height;

	private String title;
	private String description;

	private int minDamage;
	private int maxDamage;

	private byte targetClass;
	private byte tier;
	public ItemTooltip(GameItem item, Vector2f pos, int width, int height) {
		int diff = (int) (GamePanel.width - (GamePanel.width - pos.x));
		this.pos = pos.clone(-diff, 0);
		this.width = width;
		this.height = height;
		this.title = item.getName();
		this.description = item.getDescription();

		if (item.getDamage() != null) {
			this.minDamage = item.getDamage().getMin();
			this.maxDamage = item.getDamage().getMax();
		}

		this.targetClass = item.getTargetClass();
		this.tier = item.getTier();
		this.targetClass = item.getTargetClass();
	}

	public void render(Graphics2D g) {
		int spacing = 32;
		g.setColor(Color.GRAY);
		g.fillRect((int) this.pos.x, (int) this.pos.y, this.width, this.height);

		g.setColor(Color.WHITE);
		g.drawString(this.title, this.pos.x, this.pos.y + (1 * spacing));
		g.drawString(this.description, this.pos.x, this.pos.y + (2 * spacing));

		g.drawString(this.minDamage + " - " + this.maxDamage, this.pos.x, this.pos.y + (3 * spacing));

		g.drawString(this.targetClass + " ", this.pos.x, this.pos.y + (4 * spacing));

		g.drawString(this.tier + " ", this.pos.x, this.pos.y + (5 * spacing));

	}


}
