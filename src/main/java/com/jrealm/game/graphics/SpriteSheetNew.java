package com.jrealm.game.graphics;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.jrealm.game.graphics.Sprite.EffectEnum;
import com.jrealm.game.model.SpriteModel;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class SpriteSheetNew {
	private BufferedImage spriteSheetImage;
	private int animationFrame = 0;
	private int elapsedFrames = 0;
	private List<Sprite> sprites;
	private List<Integer> animationFrames;
	private int spriteImageSize;

	public SpriteSheetNew(SpriteModel key, int spriteImageSize) {
		this.spriteImageSize = spriteImageSize;
		this.spriteSheetImage = this.loadSprite(key.getSpriteKey());
		this.animationFrames = new ArrayList<>();
		this.sprites = new ArrayList<>();
		this.loadImageArray();
	}

	public SpriteSheetNew(SpriteModel key, int spriteImageSize, final List<Integer> animationFrames) {
		this.spriteImageSize = spriteImageSize;
		this.spriteSheetImage = this.loadSprite(key.getSpriteKey());
		this.animationFrames = animationFrames;
		this.sprites = new ArrayList<>();
		this.loadImageArray();
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

	public void setEffect(final EffectEnum effect) {
		for (Sprite sprite : this.sprites) {
			sprite.setEffect(effect);
		}
	}

	public void resetEffect() {
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
		return this.spriteSheetImage.getSubimage(x, y, this.spriteImageSize, this.spriteImageSize);
	}

	private BufferedImage loadSprite(String file) {
		BufferedImage sprite = null;
		try {
			sprite = ImageIO.read(this.getClass().getClassLoader().getResourceAsStream(file));
		} catch (Exception e) {
			SpriteSheetNew.log.error("ERROR: could not load file: {}", file);
		}
		return sprite;
	}
}
