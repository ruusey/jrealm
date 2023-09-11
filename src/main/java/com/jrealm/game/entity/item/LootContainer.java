package com.jrealm.game.entity.item;

import java.awt.Graphics2D;
import java.util.Random;
import java.util.UUID;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LootContainer {
	private Sprite sprite;
	private String uid;
	private GameItem[] items;
	private Vector2f pos;

	public LootContainer(Sprite sprite, Vector2f pos) {
		this.sprite = sprite;
		this.uid = UUID.randomUUID().toString();
		this.items = new GameItem[8];
		this.pos = pos;
		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < 4; i++) {
			this.items[i] = GameDataManager.GAME_ITEMS.get(r.nextInt(10));
		}
		// this.items[1] = GameDataManager.GAME_ITEMS.get(9);

	}

	public void render(Graphics2D g) {
		g.drawImage(this.sprite.image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y), 32, 32,
				null);

	}
}
