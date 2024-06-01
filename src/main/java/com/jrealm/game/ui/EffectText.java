package com.jrealm.game.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.jrealm.game.contants.TextEffect;
import com.jrealm.game.math.Vector2f;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EffectText {
    private static final float velY = -1.75f;

    private Vector2f sourcePos;
    private TextEffect effect;
    private String damage;
    @Builder.Default
    private boolean remove = false;
    @Builder.Default
    private float animationDistance = 45.0f;

    public void update() {
	this.animationDistance += EffectText.velY;
	if (this.animationDistance <= 0.0f) {
	    this.remove = true;
	}
    }

    public void render(Graphics2D g) {
	switch (this.effect) {
	case DAMAGE:
	    g.setColor(Color.RED);
	    break;
	case HEAL:
	    g.setColor(Color.GREEN);
	    break;
	case ARMOR_BREAK:
	    g.setColor(Color.BLUE);
	    break;
	case ENVIRONMENT:
	    g.setColor(Color.BLUE);
	    break;
	case PLAYER_INFO:
	    g.setColor(Color.ORANGE);
	    break;
	default:
	    break;

	}

	final Font originalFont = g.getFont();
	final Font newFont = originalFont.deriveFont(originalFont.getSize() * 0.75F);
	g.setFont(newFont);
	g.drawString(this.damage, this.sourcePos.x - (Vector2f.worldX),
		this.sourcePos.y - (Vector2f.worldY) - (64 - this.animationDistance));

	g.setFont(originalFont);

    }

    public boolean getRemove() {
	return this.remove;
    }
}
