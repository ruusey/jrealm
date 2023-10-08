package com.jrealm.game.entity.material;

import java.awt.Graphics2D;

import com.jrealm.game.entity.GameObject;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Material extends GameObject {
	private long materialId;
	protected int maxHealth = 100;
	protected int health = 100;
	protected int damage = 10;
	protected int material;

	public Material(int id, Sprite image, Vector2f origin, int size, int material) {
		super(id, image, origin, size);
		this.material = material;

		this.bounds.setXOffset(16);
		this.bounds.setYOffset(48);
		this.bounds.setWidth(32);
		this.bounds.setHeight(16);

		image.setEffect(Sprite.EffectEnum.NORMAL);
	}

	@Override
	public void update() {

	}

	@Override
	public void render(Graphics2D g) {
		g.drawImage(this.image.image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y), this.size, this.size, null);
	}
}