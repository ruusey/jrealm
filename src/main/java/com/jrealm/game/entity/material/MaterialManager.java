package com.jrealm.game.entity.material;

import java.util.ArrayList;

import com.jrealm.game.GamePanel;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.util.GameObjectKey;

public class MaterialManager {

	public ArrayList<GameObjectKey> list;
	public static enum TYPE {
		TREE(0), STONE(1), IRON(2), WHEAT(3);

		private final int value;

		TYPE(final int newValue) {
			this.value = newValue;
		}

		public int getValue() { return this.value; }
	}

	private TemplateMaterial[] tempMaterial;

	private int tileSize;
	private int chuckSize;
	private Vector2f playerStart = new Vector2f((0 + (GamePanel.width / 2)) - 32, (0 + (GamePanel.height / 2)) - 32);

	public MaterialManager(int tileSize, int chuckSize) {
		this.tileSize = tileSize;
		this.chuckSize = chuckSize;
		this.list = new ArrayList<GameObjectKey>();
		this.tempMaterial = new TemplateMaterial[4];

	}

	public void setMaterial(TYPE material, Sprite sprite, int size) {
		if(this.tempMaterial[material.getValue()] == null) {
			this.tempMaterial[material.getValue()] = new TemplateMaterial(size);
		}

		this.tempMaterial[material.getValue()].images.add(sprite);
	}

	public int add(TYPE material, int position) {

		int size = Math.max(this.tileSize, this.tempMaterial[material.getValue()].size);
		int length = (this.tempMaterial[material.getValue()].images.size() / 10) + 1; // 10 images in trees SpriteSheet, thus change later
		int index = (int) (Math.random() * (10 * length)) % (this.tempMaterial[material.getValue()].images.size());

		Vector2f pos = new Vector2f((position % this.chuckSize) * size, (position / this.chuckSize) * size);

		Material mat = new Material(0, this.tempMaterial[material.getValue()].images.get(index).getNewSubimage(), pos,
				size, material.getValue());
		float distance = mat.getBounds().distance(this.playerStart);
		this.list.add(new GameObjectKey(distance, mat));

		return material.getValue();
	}
}

class TemplateMaterial {

	public int size;
	public ArrayList<Sprite> images;

	public TemplateMaterial(int size) {
		this.size = size;
		this.images = new ArrayList<Sprite>();
	}
}