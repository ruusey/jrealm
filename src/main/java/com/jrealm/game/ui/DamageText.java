package com.jrealm.game.ui;

import java.awt.Color;
import java.awt.Graphics2D;

import com.jrealm.game.math.Vector2f;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamageText {
	private static final float velY = -2.5f;

	private Vector2f sourcePos;
	private TextEffect effect;
	private String damage;
	@Builder.Default
	private boolean remove = false;
	@Builder.Default
	private float animationDistance = 128.0f;

	public void update() {
		this.animationDistance += DamageText.velY;
		// this.sourcePos.y += DamageText.velY;

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
		default:
			break;

		}

		g.drawString(this.damage, this.sourcePos.x - (Vector2f.worldX),
				this.sourcePos.y - (Vector2f.worldY) - (64 - this.animationDistance));

	}

	public boolean getRemove() {
		return this.remove;
	}

}
