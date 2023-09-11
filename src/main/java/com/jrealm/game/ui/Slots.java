package com.jrealm.game.ui;

import java.awt.Graphics2D;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

import lombok.Data;

@Data
public class Slots {

	private GameItem item;
	private Button button;

	// Once we have items we will create an items and inventory class
	public Slots(Button button, GameItem item) {
		this.item = item;
		this.button = button;
		if (button != null) {
			this.button.setSlot(this);
		}
	}


	public void update(double time) {
		if (this.button != null) {
			this.button.update(time);
		}
	}

	public void input(MouseHandler mouse, KeyHandler key) {
		if (this.button != null) {
			this.button.input(mouse, key);
		}
	}

	public void render(Graphics2D g, Vector2f pos) {

		Sprite itemSprite = GameDataManager.getSubSprite(this.item, 8);

		if(this.button != null) {
			this.button.render(g);
		} else {
			g.drawImage(itemSprite.image, (int) pos.x, (int) pos.y, 64, 64, null);
		}
		g.drawImage(itemSprite.image, (int) pos.x, (int) pos.y, 64, 64, null);

	}

}