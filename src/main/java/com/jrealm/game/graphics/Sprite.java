package com.jrealm.game.graphics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import lombok.Data;

@Data
public class Sprite {

    public TextureRegion region;

    private int w;
    private int h;
    private float angleOffset;

    public static enum EffectEnum {
        NORMAL, SEPIA, REDISH, GRAYSCALE, NEGATIVE, DECAY, SILHOUETTE
    }

    private EffectEnum currentEffectEnum = EffectEnum.NORMAL;

    public Sprite(TextureRegion region) {
        this.region = region;
        this.w = region.getRegionWidth();
        this.h = region.getRegionHeight();
    }

    public int getWidth() {
        return this.w;
    }

    public int getHeight() {
        return this.h;
    }

    public TextureRegion getImage() {
        return this.region;
    }

    public void setEffect(EffectEnum e) {
        this.currentEffectEnum = e;
    }

    public Sprite.EffectEnum getEffect() {
        return this.currentEffectEnum;
    }

    public boolean hasEffect(Sprite.EffectEnum effect) {
        return (effect != null) && (this.currentEffectEnum != null) && this.currentEffectEnum.equals(effect);
    }

    public Sprite getSubimage(int x, int y, int w, int h) {
        return new Sprite(new TextureRegion(this.region, x, y, w, h));
    }

    @Override
    public Sprite clone() {
        return new Sprite(new TextureRegion(this.region));
    }
}
