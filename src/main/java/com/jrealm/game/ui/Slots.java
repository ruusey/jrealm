package com.jrealm.game.ui;

import java.awt.Graphics2D;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Slots {
	private GameItem item;
	private Button button;

	private Vector2f dragPos;

	public Slots(Button button, GameItem item) {
		this.item = item;
		this.button = button;
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

		if (this.button.isClicked()) {
			this.dragPos = new Vector2f(mouse.getX(), mouse.getY());
		} else {
			this.dragPos = null;
		}
	}

	public void render(Graphics2D g, Vector2f pos) {
		if (this.getItem() == null)
			return;
		Sprite itemSprite = GameDataManager.getSubSprite(this.item, 8);
		if(itemSprite==null) {
			return;
		}
		if(this.button != null) {
			this.button.render(g);
		} else {
			g.drawImage(itemSprite.image, (int) pos.x, (int) pos.y, 64, 64, null);
		}
		g.drawImage(itemSprite.image, (int) pos.x, (int) pos.y, 64, 64, null);
	}
}