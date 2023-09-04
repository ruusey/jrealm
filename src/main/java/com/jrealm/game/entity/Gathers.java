package com.jrealm.game.entity;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Vector2f;

public class Gathers extends Entity {

    private int material;

    public Gathers(SpriteSheet sprite, Vector2f origin, int size, int material) {
        super(sprite, origin, size);
        this.material = material;
    }

    public int getMaterial() { return material; }
    
    public void update() {

    }

    public void render(Graphics2D g) {

    }


}