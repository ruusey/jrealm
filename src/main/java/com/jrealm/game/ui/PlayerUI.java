package com.jrealm.game.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jrealm.game.GamePanel;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
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

	private Player player;
	public PlayerUI(Player p) {
		this.player = p;
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
		int panelWidth = (GamePanel.width / 5);

		int startX = GamePanel.width - panelWidth;


		this.equipment = new Slots[4];

		for (int i = 0; i < loot.length; i++) {
			GameItem item = loot[i];
			if (item != null) {
				int actualIdx = (int) item.getTargetSlot();
				Button b = new Button(new Vector2f(startX + (actualIdx * 64), 256), 64);
				b.addEvent(event -> {

					System.out.println("clicked");
				});
				this.equipment[actualIdx] = new Slots(b, item);
			}

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

	private void renderStats(Graphics2D g) {
		if (this.player != null) {
			int panelWidth = (GamePanel.width / 5);
			int startX = (GamePanel.width - panelWidth) + 8;

			Stats stats = this.player.getStats().clone();
			for (Slots item : this.equipment) {
				if ((item != null) && (item.getItem() != null)) {
					stats = stats.concat(item.getItem().getStats());
				}
			}
			g.setColor(Color.WHITE);

			int xOffset = 128;
			int yOffset = 42;
			int startY = 350;
			g.drawString("att :" + stats.getAtt(), startX, startY);
			g.drawString("spd :" + stats.getSpd(), startX, startY+(1*yOffset));
			g.drawString("vit :" + stats.getVit(), startX, startY + (2 * yOffset));

			g.drawString("def :" + stats.getDef(), startX + xOffset, startY);
			g.drawString("dex :" + stats.getDex(), startX + xOffset, startY + (1 * yOffset));
			g.drawString("wis :" + stats.getWis(), startX + xOffset, startY + (2 * yOffset));

		}
	}

	public void render(Graphics2D g) {
		int panelWidth = (GamePanel.width / 5);

		int startX = GamePanel.width - panelWidth;

		g.setColor(Color.GRAY);
		// System.out.println(59);

		g.fillRect(startX, 0, panelWidth, GamePanel.height);
		this.renderStats(g);

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
				this.groundLoot[i].render(g, new Vector2f(startX + (i * 64), 450));
			}
		}
	}

}