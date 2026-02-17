package com.jrealm.game.graphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.graphics.Sprite.EffectEnum;
import com.jrealm.game.model.SpriteModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpriteSheet {
    private Texture spriteSheetTexture;
    private int animationFrame = 0;
    private int elapsedFrames = 0;
    private TextureRegion[][] spriteSheetRegions;
    private List<Sprite> sprites;
    private List<Integer> animationFrames;
    private int spriteImageHeight;
    private int spriteImageWidth;

    // Current effect applied to all sprites in this sheet (used as a tag for shader selection)
    private EffectEnum currentEffect = EffectEnum.NORMAL;

    // Named animation sets for walk/idle/attack cycles
    private Map<String, List<Sprite>> animSets = new HashMap<>();
    private Map<String, List<Integer>> animSetDurations = new HashMap<>();
    private String currentAnimSetName;

    public SpriteSheet(Texture texture, int spriteWidth, int spriteHeight, int col, int row) {
        this.spriteImageWidth = spriteWidth;
        this.spriteImageHeight = spriteHeight;
        this.spriteSheetTexture = texture;
        this.sprites = new ArrayList<>();
        final int cols = texture.getWidth() / spriteImageWidth;
        final int rows = texture.getHeight() / spriteImageHeight;
        this.spriteSheetRegions = new TextureRegion[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                this.spriteSheetRegions[i][j] = new TextureRegion(texture,
                    j * spriteImageWidth, i * spriteImageHeight, spriteImageWidth, spriteImageHeight);
                this.spriteSheetRegions[i][j].flip(false, true);
            }
        }
        this.sprites.add(new Sprite(this.spriteSheetRegions[row][col]));
    }

    public SpriteSheet(String fileName, int spriteWidth, int spriteHeight, int col, int row) {
        this(GameSpriteManager.TEXTURE_CACHE.get(fileName), spriteWidth, spriteHeight, col, row);
    }

    public SpriteSheet(String fileName, int spriteWidth, int spriteHeight) {
        Texture texture = GameSpriteManager.TEXTURE_CACHE.get(fileName);
        this.spriteImageWidth = spriteWidth;
        this.spriteImageHeight = spriteHeight;
        this.spriteSheetTexture = texture;
        this.sprites = new ArrayList<>();
        final int cols = texture.getWidth() / spriteImageWidth;
        final int rows = texture.getHeight() / spriteImageHeight;
        this.spriteSheetRegions = new TextureRegion[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                this.spriteSheetRegions[i][j] = new TextureRegion(texture,
                    j * spriteImageWidth, i * spriteImageHeight, spriteImageWidth, spriteImageHeight);
                this.spriteSheetRegions[i][j].flip(false, true);
            }
        }
    }

    public SpriteSheet(Texture texture) {
        this(texture, GlobalConstants.BASE_SPRITE_SIZE, GlobalConstants.BASE_SPRITE_SIZE);
    }

    public SpriteSheet(Texture texture, int spriteWidth, int spriteHeight) {
        this.spriteImageWidth = spriteWidth;
        this.spriteImageHeight = spriteHeight;
        this.spriteSheetTexture = texture;
        this.sprites = new ArrayList<>();
        final int cols = texture.getWidth() / spriteImageWidth;
        final int rows = texture.getHeight() / spriteImageHeight;
        this.spriteSheetRegions = new TextureRegion[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                this.spriteSheetRegions[i][j] = new TextureRegion(texture,
                    j * spriteImageWidth, i * spriteImageHeight, spriteImageWidth, spriteImageHeight);
                this.spriteSheetRegions[i][j].flip(false, true);
            }
        }
    }

    public SpriteSheet(Texture texture, int x, int y, boolean isPosition) {
        this(texture, GlobalConstants.BASE_SPRITE_SIZE, GlobalConstants.BASE_SPRITE_SIZE, x, y);
    }

    public SpriteSheet(Texture texture, SpriteModel model) {
        this(texture, model.getSpriteSize() == 0 ? GlobalConstants.BASE_SPRITE_SIZE : model.getSpriteSize(),
                model.getSpriteSize() == 0 ? GlobalConstants.BASE_SPRITE_SIZE : model.getSpriteSize(), model.getCol(),
                model.getRow());
    }

    public Sprite getSubSprite(int x, int y) {
        return new Sprite(this.spriteSheetRegions[y][x]);
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
                this.elapsedFrames = 0;
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
        return this.currentEffect == effect;
    }

    public void setEffect(final EffectEnum effect) {
        this.currentEffect = effect;
        for (Sprite sprite : this.sprites) {
            sprite.setEffect(effect);
        }
    }

    public void resetEffects() {
        this.currentEffect = EffectEnum.NORMAL;
        for (Sprite sprite : this.sprites) {
            sprite.setEffect(EffectEnum.NORMAL);
        }
    }

    public void addAnimSet(String name, List<Sprite> frames, List<Integer> durations) {
        this.animSets.put(name, frames);
        this.animSetDurations.put(name, durations);
    }

    public void setAnimSet(String name) {
        if (name == null || name.equals(this.currentAnimSetName)) return;
        List<Sprite> frames = this.animSets.get(name);
        List<Integer> durations = this.animSetDurations.get(name);
        if (frames == null || durations == null) return;
        this.currentAnimSetName = name;
        this.sprites = frames;
        this.animationFrames = durations;
        this.animationFrame = 0;
        this.elapsedFrames = 0;
        for (Sprite sprite : this.sprites) {
            sprite.setEffect(this.currentEffect);
        }
    }

    public boolean hasAnimSets() {
        return !this.animSets.isEmpty();
    }

    public TextureRegion getCurrentFrame() {
        if (this.sprites == null || this.sprites.isEmpty()) return null;
        Sprite sprite = this.sprites.get(this.animationFrame);
        if (sprite != null)
            return sprite.getRegion();
        return null;
    }

    public void loadImageArray(final int x, final int y) {
        Sprite newSprite = new Sprite(this.spriteSheetRegions[x][y]);
        this.sprites.add(newSprite);
    }

    public void loadImageArray() {
        final int cols = this.spriteSheetTexture.getWidth() / this.spriteImageWidth;
        final int rows = this.spriteSheetTexture.getHeight() / this.spriteImageHeight;

        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < cols; y++) {
                final Sprite newSprite = new Sprite(this.spriteSheetRegions[x][y]);
                this.sprites.add(newSprite);
            }
        }
    }

    public TextureRegion cropImage(int x, int y, int width, int height) {
        TextureRegion region = new TextureRegion(this.spriteSheetTexture, x, y, width, height);
        region.flip(false, true);
        return region;
    }

    public TextureRegion getSubimage(int row, int col) {
        return this.spriteSheetRegions[row][col];
    }

    public static SpriteSheet fromSpriteModel(SpriteModel model) {
        Texture texture = GameSpriteManager.TEXTURE_CACHE.get(model.getSpriteKey());
        return new SpriteSheet(texture, model);
    }

    public static SpriteSheet x8SpriteSheet(final String fileName) {
        return new SpriteSheet(fileName, GlobalConstants.BASE_SPRITE_SIZE, GlobalConstants.BASE_SPRITE_SIZE);
    }

    public static SpriteSheet x16SpriteSheet(final String fileName) {
        return new SpriteSheet(fileName, GlobalConstants.MEDIUM_ART_SIZE, GlobalConstants.MEDIUM_ART_SIZE);
    }
}
