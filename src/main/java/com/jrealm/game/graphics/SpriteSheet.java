package com.jrealm.game.graphics;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.graphics.Sprite.EffectEnum;
import com.jrealm.game.model.SpriteModel;
import com.jrealm.game.util.Tuple;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpriteSheet {
    private BufferedImage spriteSheetImage;
    private int animationFrame = 0;
    private int elapsedFrames = 0;
    private BufferedImage[][] spriteSheetImageSplit;
    private List<Sprite> sprites;
    private List<Integer> animationFrames;
    private int spriteImageHeight;
    private int spriteImageWidth;

    // Builds a sprite sheet from fileName at row, col with given subimage width and
    // height of
    // spriteWidth, spriteHeight
    public SpriteSheet(BufferedImage toUse, int spriteWidth, int spriteHeight, int col, int row) {
        this.spriteImageWidth = spriteWidth;
        this.spriteImageHeight = spriteHeight;
        this.spriteSheetImage = toUse;
        this.sprites = new ArrayList<>();
        final int cols = toUse.getWidth() / spriteImageWidth;
        final int rows = toUse.getHeight() / spriteImageHeight;
        this.spriteSheetImageSplit = new BufferedImage[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                spriteSheetImageSplit[i][j] = this.getSubimage(i, j);
            }
        }
        this.sprites.add(new Sprite(this.spriteSheetImageSplit[row][col]));
    }

    // Builds a sprite sheet from fileName at row, col with given subimage width and
    // height of
    // spriteWidth, spriteHeight
    public SpriteSheet(String fileName, int spriteWidth, int spriteHeight, int col, int row) {
        this(deepCopy(GameSpriteManager.IMAGE_CACHE.get(fileName)), spriteWidth, spriteHeight, col, row);
    }

    // Builds an empty sprite sheet from fileName with spriteWidth and spriteHeight
    public SpriteSheet(String fileName, int spriteWidth, int spriteHeight) {
        BufferedImage toUse = GameSpriteManager.IMAGE_CACHE.get(fileName);
        toUse = deepCopy(toUse);
        this.spriteImageWidth = spriteWidth;
        this.spriteImageHeight = spriteHeight;
        this.spriteSheetImage = toUse;
        this.sprites = new ArrayList<>();
        final int cols = toUse.getWidth() / spriteImageWidth;
        final int rows = toUse.getHeight() / spriteImageHeight;
        spriteSheetImageSplit = new BufferedImage[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                spriteSheetImageSplit[i][j] = this.getSubimage(i, j);
            }
        }
    }

    public SpriteSheet(BufferedImage baseSheet) {
        this(baseSheet, GlobalConstants.BASE_SPRITE_SIZE, GlobalConstants.BASE_SPRITE_SIZE);
    }

    public SpriteSheet(BufferedImage baseSheet, int x, int y) {
        this(baseSheet, GlobalConstants.BASE_SPRITE_SIZE, GlobalConstants.BASE_SPRITE_SIZE, x, y);
    }

    public SpriteSheet(BufferedImage baseSheet, SpriteModel model) {
        this(baseSheet, model.getSpriteSize() == 0 ? GlobalConstants.BASE_SPRITE_SIZE : model.getSpriteSize(),
                model.getSpriteSize() == 0 ? GlobalConstants.BASE_SPRITE_SIZE : model.getSpriteSize(), model.getCol(),
                model.getRow());
    }

    public SpriteSheet(BufferedImage baseSheet, int spriteSize, List<Tuple<Integer, Integer>> spriteFrames,
            final List<Integer> animationFrames) {
        this(baseSheet, spriteSize, spriteSize);
        // Copy over the animation frames
        for (int i = 0; i < spriteFrames.size(); i++) {
            Tuple<Integer, Integer> spriteLocation = spriteFrames.get(i);
            if (spriteLocation != null) {
                this.animationFrames.set(i, animationFrames.get(i));
            }
        }
    }

    public Sprite getSubSprite(int x, int y) {
        return new Sprite(this.spriteSheetImageSplit[y][x]);
    }

    public void resetAnimation() {
        if ((this.animationFrames != null) && (this.animationFrames.size() > 0)) {
            this.animationFrame = 0;
        }
    }

    public void animate() {
        if ((this.animationFrames != null) && (this.animationFrames.size() > 0)) {
            int currentAnimationFrames = this.animationFrames.get(this.animationFrame);
            if (this.elapsedFrames >= currentAnimationFrames) {
                if (this.animationFrame == (this.animationFrames.size() - 1)) {
                    this.animationFrame = 0;
                } else {
                    this.animationFrame = this.animationFrame + 1;
                }
            }
        }
        this.elapsedFrames++;
    }

    public boolean hasEffect(final EffectEnum effect) {
        for (Sprite sprite : this.sprites) {
            if (sprite.hasEffect(effect))
                return true;
        }
        return false;
    }

    public void setEffect(final EffectEnum effect) {
        for (Sprite sprite : this.sprites) {
            sprite.setEffect(effect);
        }
    }

    public void resetEffects() {
        for (Sprite sprite : this.sprites) {
            sprite.restoreDefault();
        }
    }

    public BufferedImage getCurrentFrame() {
        Sprite sprite = this.sprites.get(this.animationFrame);
        if (sprite != null)
            return sprite.getImage();
        return null;
    }

    public void loadImageArray(final int x, final int y) {
        Sprite newSprite = new Sprite(this.getSubimage(x, y));
        this.sprites.add(newSprite);
    }

    public void loadImageArray() {
        final int cols = this.spriteSheetImage.getWidth() / this.spriteImageWidth;
        final int rows = this.spriteSheetImage.getHeight() / this.spriteImageHeight;

        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < cols; y++) {
                final Sprite newSprite = new Sprite(this.getSubimage(x, y));
                this.sprites.add(newSprite);
            }
        }
    }

    public BufferedImage cropImage(int x, int y, int width, int height) {
        return this.spriteSheetImage.getSubimage(x, y, width, height);
    }

    public BufferedImage getSubimage(int x, int y) {
        return this.spriteSheetImage.getSubimage(y * this.spriteImageWidth, x * this.spriteImageHeight,
                this.spriteImageWidth, this.spriteImageHeight);
    }

    public static SpriteSheet fromSpriteModel(SpriteModel model) {
        BufferedImage toUse = deepCopy(GameSpriteManager.IMAGE_CACHE.get(model.getSpriteKey()));
        return new SpriteSheet(toUse, model);
    }

    public static SpriteSheet x8SpriteSheet(final String fileName) {
        return new SpriteSheet(fileName, GlobalConstants.BASE_SPRITE_SIZE, GlobalConstants.BASE_SPRITE_SIZE);
    }

    public static SpriteSheet x16SpriteSheet(final String fileName) {
        return new SpriteSheet(fileName, GlobalConstants.MEDIUM_ART_SIZE, GlobalConstants.MEDIUM_ART_SIZE);
    }
    
    public static BufferedImage deepCopy(BufferedImage bi) {
    	 final ColorModel cm = bi.getColorModel();
    	 final boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    	 final WritableRaster raster = bi.copyData(null);
    	 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
}
