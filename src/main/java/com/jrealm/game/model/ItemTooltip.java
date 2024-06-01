package com.jrealm.game.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
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

    private Stats stats;

    public ItemTooltip(GameItem item, Vector2f pos, int width, int height) {
	// int diff = (int) (GamePanel.width - (GamePanel.width - pos.x)) - 20;
	this.pos = pos;
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

	this.stats = item.getStats();
    }

    public void render(Graphics2D g) {
	int spacing = 32;
	Font originalFont = g.getFont();
	Font newFont = originalFont.deriveFont(originalFont.getSize() * 0.5F);
	g.setFont(newFont);
	g.setColor(Color.GRAY);
	g.setFont(null);
	g.fillRect((int) this.pos.x, (int) this.pos.y, this.width, this.height);

	g.setColor(Color.WHITE);
	g.drawString(this.title, this.pos.x, this.pos.y + (1 * spacing));
	g.drawString(this.description, this.pos.x, this.pos.y + (2 * spacing));

	if (this.maxDamage > 0) {
	    g.drawString("Damage: " + this.minDamage + " - " + this.maxDamage, this.pos.x, this.pos.y + (3 * spacing));
	}

	g.drawString("Class: " + this.targetClass + " ", this.pos.x, this.pos.y + (4 * spacing));

	g.drawString("Tier: " + this.tier + " ", this.pos.x, this.pos.y + (5 * spacing));
	if (this.stats != null) {
	    List<String> statsToDraw = new ArrayList<>();
	    if (this.stats.getHp() > 0) {
		statsToDraw.add("HP +" + this.stats.getHp());
	    }
	    if (this.stats.getMp() > 0) {
		statsToDraw.add("MP +" + this.stats.getMp());
	    }
	    if (this.stats.getAtt() > 0) {
		statsToDraw.add("ATT +" + this.stats.getAtt());
	    }
	    if (this.stats.getDef() > 0) {
		statsToDraw.add("DEF +" + this.stats.getDef());

	    }
	    if (this.stats.getSpd() > 0) {
		statsToDraw.add("SPD +" + this.stats.getSpd());
	    }
	    if (this.stats.getDex() > 0) {
		statsToDraw.add("DEX +" + this.stats.getDex());
	    }
	    if (this.stats.getVit() > 0) {
		statsToDraw.add("VIT +" + this.stats.getVit());
	    }
	    if (this.stats.getWis() > 0) {
		statsToDraw.add("WIS +" + this.stats.getWis());
	    }
	    for (int i = 0; i < statsToDraw.size(); i++) {
		String toDraw = statsToDraw.get(i);
		g.drawString(toDraw, this.pos.x, this.pos.y + ((6 + i) * spacing));
	    }
	}

	g.setFont(originalFont);

    }

}
