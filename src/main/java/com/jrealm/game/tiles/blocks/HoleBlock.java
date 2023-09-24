package com.jrealm.game.tiles.blocks;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;

public class HoleBlock extends Tile {

    public HoleBlock(Sprite img, Vector2f pos, int w, int h) {
        super(img, pos, w, h);
    }

    public boolean update(AABB p) {
        return false;
    }

    public Sprite getImage() {
        return img;
    }

    public boolean isInside(AABB p) {

        if(p.getPos().x + p.getXOffset() < pos.x) return false;
        if(p.getPos().y + p.getYOffset() < pos.y) return false;
        if(w + pos.x < p.getWidth() + (p.getPos().x + p.getXOffset())) return false;
        if(h + pos.y < p.getHeight() + (p.getPos().y + p.getYOffset())) return false;
        
        return true;
    }

    public void render(Graphics2D g){
        super.render(g);
        
    }

}
