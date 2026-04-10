package com.openrealm.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.openrealm.game.data.GameDataManager;
import com.openrealm.game.entity.Entity;
import com.openrealm.game.entity.Player;
import com.openrealm.game.math.Vector2f;

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

    private float cachedEnergy = 0f;
    private String cachedValueText = "";

    private void updateValues() {
        try {
            Player player = (Player) this.e;
            switch (this.field) {
            case "getHealthPercent":
                this.cachedEnergy = player.getHealthpercent();
                this.cachedValueText = player.getHealth() + " (" + player.getComputedStats().getHp() + ")";
                break;
            case "getManaPercent":
                this.cachedEnergy = player.getManapercent();
                this.cachedValueText = player.getMana() + " (" + player.getComputedStats().getMp() + ")";
                break;
            case "getExperiencePercent":
                this.cachedEnergy = player.getExperiencePercent();
                long fame = GameDataManager.EXPERIENCE_LVLS.getBaseFame(player.getExperience());
                if (fame > 0) {
                    this.cachedValueText = "Fame: " + fame;
                } else {
                    this.cachedValueText = player.getExperience() + " (" + player.getUpperExperienceBound() + ")";
                }
                break;
            }
        } catch (Exception e1) {
        }
    }

    /** Render bar shapes. Call while ShapeRenderer is active. */
    public void renderShapes(ShapeRenderer shapes) {
        this.updateValues();
        shapes.setColor(this.bgColor);
        shapes.rect(this.pos.x, this.pos.y, this.barWidth, this.barHeight);
        shapes.setColor(this.fgColor);
        shapes.rect(this.pos.x, this.pos.y, this.barWidth * this.cachedEnergy, this.barHeight);
    }

    /** Render bar text. Call while SpriteBatch is active. */
    public void renderText(SpriteBatch batch, BitmapFont font) {
        if (!this.cachedValueText.isEmpty()) {
            font.setColor(Color.WHITE);
            GlyphLayout layout = new GlyphLayout(font, this.cachedValueText);
            float textX = this.pos.x + (this.barWidth - layout.width) / 2f;
            float textY = this.pos.y + (this.barHeight - layout.height) / 2f;
            font.draw(batch, this.cachedValueText, textX, textY);
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        this.updateValues();
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(this.bgColor);
        shapes.rect(this.pos.x, this.pos.y, this.barWidth, this.barHeight);
        shapes.setColor(this.fgColor);
        shapes.rect(this.pos.x, this.pos.y, this.barWidth * this.cachedEnergy, this.barHeight);
        shapes.end();
        batch.begin();

        if (!this.cachedValueText.isEmpty()) {
            font.setColor(Color.WHITE);
            GlyphLayout layout = new GlyphLayout(font, this.cachedValueText);
            float textX = this.pos.x + (this.barWidth - layout.width) / 2f;
            float textY = this.pos.y + (this.barHeight - layout.height) / 2f;
            font.draw(batch, this.cachedValueText, textX, textY);
        }
    }
}
