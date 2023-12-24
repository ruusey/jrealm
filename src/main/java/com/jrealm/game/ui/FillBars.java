package com.jrealm.game.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

import com.jrealm.game.entity.Entity;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;

import lombok.Data;

@Data
public class FillBars {

	private BufferedImage[] bar; // 1: bar, 2: energy, 3: ends

	private Entity e;

	private Vector2f pos;
	private int size;
	private int length;

	private int energyLength;
	private String field;
	private int barWidthRatio;
	private int energyWidthRatio;

	private int barHeightRatio;

	public FillBars(Player e, BufferedImage[] sprite, Vector2f pos, int size, int length, String field) {
		this.e = e;
		this.bar = sprite;
		this.pos = pos;
		this.size = size;
		this.length = length;
		this.field = field;
		Method method;
		try {
			method = e.getClass().getMethod(field);
			float energy = (float) method.invoke(e);

			this.energyLength = (int) ((this.bar[0].getWidth() + size) * (length * energy));
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		this.barWidthRatio = ((this.bar[0].getWidth() + size) * length) / (this.bar[0].getWidth());
		this.energyWidthRatio = this.energyLength / (this.bar[0].getWidth());

		this.barHeightRatio = (this.bar[0].getHeight() + size) / this.bar[0].getHeight();
	}

	public void render(Graphics2D g) {
		int endsWidth = 0;
		int centerHeight = (int) (this.pos.y - this.barHeightRatio - (this.bar[0].getHeight() / 2));

		Method method;
		try {
			method = this.e.getClass().getMethod(this.field);
			float energy = (float) method.invoke(this.e);
			this.energyLength = (int) ((this.bar[0].getWidth() + this.size) * (this.length * energy));
		} catch (Exception e1) {

		}

		this.energyWidthRatio = this.energyLength / (this.bar[0].getWidth());

		if(this.bar[2] != null) {
			endsWidth = this.bar[2].getWidth() + this.size;

			g.drawImage(this.bar[2], (int) (this.pos.x), (int) (this.pos.y), endsWidth, this.bar[2].getHeight() + this.size, null);
			g.drawImage(this.bar[2], (int) (this.pos.x + (endsWidth * 2) + ((this.bar[0].getWidth() + this.size) * this.length)) - this.barWidthRatio, (int) (this.pos.y), -(endsWidth), this.bar[2].getHeight() + this.size, null);
			centerHeight += (this.bar[2].getHeight() / 2) + ((this.bar[2].getHeight() - this.bar[0].getHeight()) / 2);
		}

		g.drawImage(this.bar[0], (int) ((this.pos.x + endsWidth) - this.barWidthRatio), centerHeight, (this.bar[0].getWidth() + this.size) * this.length, this.bar[0].getHeight() + this.size, null);
		g.drawImage(this.bar[1], (int) ((this.pos.x + endsWidth) - this.energyWidthRatio), centerHeight, this.energyLength, (int) (this.bar[0].getHeight() + this.size), null);
	}

}