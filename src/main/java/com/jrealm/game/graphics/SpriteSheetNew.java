package com.jrealm.game.graphics;

import java.awt.image.BufferedImage;
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
public class SpriteSheetNew {
	private BufferedImage spriteSheetImage;
	private int animationFrame = 0;
	private int elapsedFrames = 0;
	private List<Sprite> sprites;
	private List<Integer> animationFrames;
	private int spriteImageSize;

	public SpriteSheetNew(BufferedImage baseSheet) {
		this.spriteImageSize = GlobalConstants.BASE_SPRITE_SIZE;
		this.spriteSheetImage = baseSheet;
		this.animationFrames = new ArrayList<>();
		this.sprites = new ArrayList<>();
		this.loadImageArray();
	}

	public SpriteSheetNew(BufferedImage baseSheet, int x, int y) {
		this.spriteImageSize = GlobalConstants.BASE_SPRITE_SIZE;
		this.spriteSheetImage = baseSheet;
		this.animationFrames = new ArrayList<>();
		this.sprites = new ArrayList<>();
		this.loadImageArray(y, x);
	}

	public SpriteSheetNew(BufferedImage baseSheet, SpriteModel model) {
		this.spriteImageSize = model.getSpriteSize() == 0 ? GlobalConstants.BASE_SPRITE_SIZE : model.getSpriteSize();
		this.spriteSheetImage = baseSheet;
		this.animationFrames = new ArrayList<>();
		this.sprites = new ArrayList<>();
		this.loadImageArray(model.getRow(), model.getCol());
	}

	public SpriteSheetNew(BufferedImage baseSheet, List<Tuple<Integer, Integer>> spriteFrames,
			final List<Integer> animationFrames) {
		this.spriteImageSize = GlobalConstants.BASE_SPRITE_SIZE;
		this.spriteSheetImage = baseSheet;
		this.animationFrames = animationFrames;
		this.sprites = new ArrayList<>();
		for (Tuple<Integer, Integer> sprite : spriteFrames) {
			this.loadImageArray(sprite.getX(), sprite.getY());
		}
	}

	public void resetAnimation() {
		if ((this.animationFrames != null) && (this.animationFrames.size() > 0)) {
			this.animationFrame = 0;
		}
	}

	public void animate() {
		if ((this.animationFrames != null) && (this.animationFrames.size() > 0)) {
			int currentAnimationFrames = this.animationFrames.get(this.animationFrame);
			if(this.elapsedFrames>=currentAnimationFrames) {
				if(this.animationFrame ==  (this.animationFrames.size()-1)) {
					this.animationFrame = 0;
				}else {
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

	private void loadImageArray(final int x, final int y) {
		Sprite newSprite = new Sprite(this.getSubimage(x, y));
		this.sprites.add(newSprite);
	}

	private void loadImageArray() {
		int subimageWidth = this.spriteSheetImage.getWidth() / this.spriteImageSize;
		int subImageHeight = this.spriteSheetImage.getHeight() / this.spriteImageSize;

		for (int x = 0; x < (this.spriteSheetImage.getWidth() / subimageWidth); x++) {
			for (int y = 0; y < (this.spriteSheetImage.getHeight() / subImageHeight); y++) {
				Sprite newSprite = new Sprite(this.getSubimage(x, y));
				this.sprites.add(newSprite);
			}
		}
	}

	private BufferedImage getSubimage(int x, int y) {
		return this.spriteSheetImage.getSubimage(y * this.spriteImageSize, x * this.spriteImageSize,
				this.spriteImageSize, this.spriteImageSize);
	}

	public static SpriteSheetNew fromSpriteModel(SpriteModel model) {
		BufferedImage toUse = GameSpriteManager.IMAGE_CACHE.get(model.getSpriteKey());
		return new SpriteSheetNew(toUse, model);
	}
}
