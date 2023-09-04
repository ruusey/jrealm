package com.jrealm.game.entity.material;

import java.awt.Graphics2D;

import com.jrealm.game.entity.GameObject;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;

public class Material extends GameObject {

    protected int maxHealth = 100;
    protected int health = 100;
    protected int damage = 10;
    protected int material;

    public Material(Sprite image, Vector2f origin, int size, int material) {
        super(image, origin, size);
        this.material = material;

        bounds.setXOffset(16);
        bounds.setYOffset(48);
        bounds.setWidth(32);
        bounds.setHeight(16);

        image.setEffect(Sprite.effect.DECAY);
    }

    public void update() {

    }

    public void render(Graphics2D g) {
        g.drawImage(image.image, (int) (pos.getWorldVar().x), (int) (pos.getWorldVar().y), size, size, null);
    }
}