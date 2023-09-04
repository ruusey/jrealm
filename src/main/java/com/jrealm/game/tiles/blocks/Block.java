package com.jrealm.game.tiles.blocks;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;

public abstract class Block {
    protected int w;
    protected int h;

    public Sprite img;
    public Vector2f pos;

    public Block(Sprite img, Vector2f pos, int w, int h) {
        this.img = img;
        this.pos = pos;
        this.w = w;
        this.h = h;
    }

    public int getWidth() { return w; }
    public int getHeight() { return h; }

    public abstract boolean update(AABB p);
    public abstract boolean isInside(AABB p);

    public abstract Sprite getImage();
    public Vector2f getPos() { return pos; }

    public void render(Graphics2D g) {
        g.drawImage(img.image, (int) pos.getWorldVar().x, (int) pos.getWorldVar().y, w, h, null);

    }
}
