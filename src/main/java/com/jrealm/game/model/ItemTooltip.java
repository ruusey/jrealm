package com.jrealm.game.model;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
        this.stats = item.getStats();
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        int spacing = 16;

        // Background
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.GRAY);
        shapes.rect(this.pos.x, this.pos.y, this.width, this.height);
        shapes.end();
        batch.begin();

        font.setColor(Color.WHITE);
        font.draw(batch, this.title, this.pos.x, this.pos.y + (1 * spacing));
        font.draw(batch, this.description, this.pos.x, this.pos.y + (2 * spacing));

        if (this.maxDamage > 0) {
            font.draw(batch, "Damage: " + this.minDamage + " - " + this.maxDamage, this.pos.x, this.pos.y + (3 * spacing));
        }

        font.draw(batch, "Class: " + this.targetClass, this.pos.x, this.pos.y + (4 * spacing));
        font.draw(batch, "Tier: " + this.tier, this.pos.x, this.pos.y + (5 * spacing));

        if (this.stats != null) {
            List<String> statsToDraw = new ArrayList<>();
            if (this.stats.getHp() > 0) statsToDraw.add("HP +" + this.stats.getHp());
            if (this.stats.getMp() > 0) statsToDraw.add("MP +" + this.stats.getMp());
            if (this.stats.getAtt() > 0) statsToDraw.add("ATT +" + this.stats.getAtt());
            if (this.stats.getDef() > 0) statsToDraw.add("DEF +" + this.stats.getDef());
            if (this.stats.getSpd() > 0) statsToDraw.add("SPD +" + this.stats.getSpd());
            if (this.stats.getDex() > 0) statsToDraw.add("DEX +" + this.stats.getDex());
            if (this.stats.getVit() > 0) statsToDraw.add("VIT +" + this.stats.getVit());
            if (this.stats.getWis() > 0) statsToDraw.add("WIS +" + this.stats.getWis());
            for (int i = 0; i < statsToDraw.size(); i++) {
                font.draw(batch, statsToDraw.get(i), this.pos.x, this.pos.y + ((6 + i) * spacing));
            }
        }
    }
}
