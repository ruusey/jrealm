package com.jrealm.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;

import lombok.Data;

@Data
public class FillBars {

    private Entity e;
    private Vector2f pos;
    private int barWidth;
    private int barHeight;
    private String field;

    private Color bgColor;
    private Color fgColor;

    public FillBars(Player e, Vector2f pos, int barWidth, int barHeight, String field, Color bgColor, Color fgColor) {
        this.e = e;
        this.pos = pos;
        this.barWidth = barWidth;
        this.barHeight = barHeight;
        this.field = field;
        this.bgColor = bgColor;
        this.fgColor = fgColor;
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        float energy = 0.0f;
        String valueText = "";
        try {
            Player player = (Player) this.e;
            switch (this.field) {
            case "getHealthPercent":
                energy = player.getHealthpercent();
                valueText = player.getHealth() + " (" + player.getComputedStats().getHp() + ")";
                break;
            case "getManaPercent":
                energy = player.getManapercent();
                valueText = player.getMana() + " (" + player.getComputedStats().getMp() + ")";
                break;
            case "getExperiencePercent":
                energy = player.getExperiencePercent();
                long fame = GameDataManager.EXPERIENCE_LVLS.getBaseFame(player.getExperience());
                if (fame > 0) {
                    valueText = "Fame: " + fame;
                } else {
                    valueText = player.getExperience() + " (" + player.getUpperExperienceBound() + ")";
                }
                break;
            }
        } catch (Exception e1) {
            // Ignore
        }

        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Background bar
        shapes.setColor(this.bgColor);
        shapes.rect(this.pos.x, this.pos.y, this.barWidth, this.barHeight);
        // Energy fill bar
        shapes.setColor(this.fgColor);
        shapes.rect(this.pos.x, this.pos.y, this.barWidth * energy, this.barHeight);
        shapes.end();
        batch.begin();

        // Draw centered value text inside bar
        if (!valueText.isEmpty()) {
            font.setColor(Color.WHITE);
            GlyphLayout layout = new GlyphLayout(font, valueText);
            float textX = this.pos.x + (this.barWidth - layout.width) / 2f;
            float textY = this.pos.y + (this.barHeight - layout.height) / 2f;
            font.draw(batch, valueText, textX, textY);
        }
    }
}
