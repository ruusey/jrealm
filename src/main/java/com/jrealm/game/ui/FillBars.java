package com.jrealm.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;

import lombok.Data;

@Data
public class FillBars {

    private Entity e;
    private Vector2f pos;
    private int size;
    private int length;
    private String field;
    private int barWidth;

    private Color bgColor;
    private Color fgColor;

    public FillBars(Player e, Vector2f pos, int size, int length, String field, Color bgColor, Color fgColor) {
        this.e = e;
        this.pos = pos;
        this.size = size;
        this.length = length;
        this.field = field;
        this.barWidth = size * length;
        this.bgColor = bgColor;
        this.fgColor = fgColor;
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes) {
        float energy = 0.0f;
        try {
            switch (this.field) {
            case "getHealthPercent":
                energy = e.getHealthpercent();
                break;
            case "getManaPercent":
                energy = e.getManapercent();
                break;
            case "getExperiencePercent":
                energy = ((Player) e).getExperiencePercent();
                break;
            }
        } catch (Exception e1) {
            // Ignore
        }

        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Background bar
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(this.pos.x, this.pos.y, this.barWidth, this.size);
        // Energy bar
        shapes.setColor(this.fgColor);
        shapes.rect(this.pos.x, this.pos.y, this.barWidth * energy, this.size);
        shapes.end();
        batch.begin();
    }
}
