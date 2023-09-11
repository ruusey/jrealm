package com.jrealm.game.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jrealm.game.GamePanel;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

import lombok.Data;

@Data
public class PlayerUI {

	private FillBars hp;
	private FillBars mp;
	private Slots[] equipment;

	private Slots[] inventory;

	private Slots[] groundLoot;

	public PlayerUI(Player p) {

		SpriteSheet bars = new SpriteSheet("ui/fillbars.png", 0);
		BufferedImage[] barSpritesHp = {
				bars.getSubimage(12, 2, 7, 16),
				bars.getSubimage(39, 0, 7, 14), // red health bar
				bars.getSubimage(0, 0, 12, 20) };

		BufferedImage[] barSpritesMp = { bars.getSubimage(12, 2, 7, 16), bars.getSubimage(39, 16, 7, 14),
				bars.getSubimage(0, 0, 12, 20) };
		Vector2f posHp = new Vector2f(GamePanel.width - 256, 128);
		Vector2f posMp = posHp.clone(0, 64);

		// Vector2f pos = new Vector2f(GamePanel.width - 128, 128);

		this.hp = new FillBars(p, barSpritesHp, posHp, 16, 16);
		this.mp = new FillBars(p, barSpritesMp, posMp, 16, 16);

		// BuildOptionUI boUI = new BuildOptionUI();
		this.equipment = new Slots[4];
		this.groundLoot = new Slots[8];
		this.inventory = new Slots[16];
	}

	public void setEquipment(GameItem[] loot) {
		int lootLength = 0;

		for (GameItem item : loot) {
			if (item != null) {
				lootLength++;
			}
		}
		this.equipment = new Slots[lootLength];

		for (int i = 0; i < lootLength; i++) {
			this.equipment[i] = new Slots(null, loot[i]);
			// this.equipment[i].setItem(loot[i]);
		}
	}

	public void setGroundLoot(GameItem[] loot) {
		int lootLength = 0;

		for (GameItem item : loot) {
			if (item != null) {
				lootLength++;
			}
		}
		this.groundLoot = new Slots[lootLength];

		for (int i = 0; i < lootLength; i++) {
			this.groundLoot[i] = new Slots(null, loot[i]);
			// this.equipment[i].setItem(loot[i]);
		}
	}


	public void update(double time) {
		for (int i = 0; i < this.equipment.length; i++) {
			Slots curr = this.equipment[i];
			if (curr != null) {
				curr.update(time);
			}
		}
	}

	public void input(MouseHandler mouse, KeyHandler key) {
		for (int i = 0; i < this.equipment.length; i++) {
			Slots curr = this.equipment[i];
			if (curr != null) {
				this.equipment[i].input(mouse, key);
			}
		}
	}

	public boolean isEquipmentEmpty() {

		for (int i = 0; i < this.equipment.length; i++) {
			Slots curr = this.equipment[i];
			if (curr == null) {
				continue;
			}
			if (curr.getItem() != null)
				return false;
		}

		return true;
	}

	public void render(Graphics2D g) {
		int panelWidth = (GamePanel.width / 5);

		int startX = GamePanel.width - panelWidth;

		g.setColor(Color.GRAY);
		// System.out.println(59);

		g.fillRect(startX, 0, panelWidth, GamePanel.height);

		this.hp.render(g);
		this.mp.render(g);
		for (int i = 0; i < this.equipment.length; i++) {
			Slots curr = this.equipment[i];
			if (curr != null) {
				this.equipment[i].render(g, new Vector2f(startX + (i * 64), 256));
			}
		}

		for (int i = 0; i < this.groundLoot.length; i++) {
			Slots curr = this.groundLoot[i];
			if (curr != null) {
				this.groundLoot[i].render(g, new Vector2f(startX + (i * 64), 412));
			}
		}
	}

}