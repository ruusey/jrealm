package com.jrealm.game.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import com.jrealm.game.math.Matrix;

import lombok.Data;

@Data
public class Sprite {

	public BufferedImage image;

	private int[] pixels;
	private int[] ogpixels;

	private int w;
	private int h;

	private float angleOffset;

	public static enum EffectEnum {NORMAL, SEPIA, REDISH, GRAYSCALE, NEGATIVE, DECAY}

	private float[][] id = {{1.0f, 0.0f, 0.0f},
			{0.0f, 1.0f, 0.0f},
			{0.0f, 0.0f, 1.0f},
			{0.0f, 0.0f, 0.0f}};

	private float[][] negative = {{1.0f, 0.0f, 0.0f},
			{0.0f, 1.0f, 0.0f},
			{0.0f, 0.0f, 1.0f},
			{0.0f, 0.0f, 0.0f}};

	private float[][] decay = {{0.000f, 0.333f, 0.333f},
			{0.333f, 0.000f, 0.333f},
			{0.333f, 0.333f, 0.000f},
			{0.000f, 0.000f, 0.000f}};

	private float[][] sepia = {{0.393f, 0.349f, 0.272f},
			{0.769f, 0.686f, 0.534f},
			{0.189f, 0.168f, 0.131f},
			{0.000f, 0.000f, 0.000f}};

	private float[][] redish = {{1.0f, 0.0f, 0.0f},
			{0.0f, 0.3f, 0.0f},
			{0.0f, 0.0f, 0.3f},
			{0.0f, 0.0f, 0.0f}};

	private float[][] grayscale = {{0.333f, 0.333f, 0.333f},
			{0.333f, 0.333f, 0.333f},
			{0.333f, 0.333f, 0.333f},
			{0.000f, 0.000f, 0.000f}};

	private float[][] currentEffect = this.id;

	private EffectEnum currentEffectEnum = EffectEnum.NORMAL;


	public Sprite(BufferedImage image) {
		this.image = image;
		this.w = image.getWidth();
		this.h = image.getHeight();
		this.ogpixels = image.getRGB(0, 0, this.w, this.h, this.ogpixels, 0, this.w);
		this.pixels = this.ogpixels;
	}

	public int getWidth() { return this.w; }
	public int getHeight() { return this.h; }

	public void saveColors() {
		this.pixels = this.image.getRGB(0, 0, this.w, this.h, this.pixels, 0, this.w);
		this.currentEffect = this.id;
	}

	public void restoreColors() {
		this.image.setRGB(0, 0, this.w, this.h, this.pixels, 0, this.w);
	}

	public void restoreDefault() {
		this.image.setRGB(0, 0, this.w, this.h, this.ogpixels, 0, this.w);
	}

	// in #FFFFFF format
	public Color hexToColor(String color) {
		return new Color(
				Integer.valueOf(color.substring(1, 3), 16),
				Integer.valueOf(color.substring(3, 5), 16),
				Integer.valueOf(color.substring(5, 7), 16));
	}

	public void setContrast(float value) {
		float[][] effect = this.id;
		float contrast = (259 * (value + 255)) / (255 * (259 - value));
		for(int i = 0; i < 3; i++) {
			if(i < 3) {
				effect[i][i] = contrast;
			}
			effect[3][i] = 128 * (1 - contrast);
		}

		this.addEffect(effect);
	}

	public void setBrightness(float value) {
		float[][] effect = this.id;
		for(int i = 0; i < 3; i++) {
			effect[3][i] = value;
		}

		this.addEffect(effect);
	}

	public BufferedImage flipHorizontal() {
		AffineTransform at = new AffineTransform();
		BufferedImage newImage = new BufferedImage(
				this.image.getWidth(), this.image.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = newImage.createGraphics();
		g.transform(at);
		g.drawImage(this.image, 0, 0, null);
		g.dispose();
		return newImage;
	}

	public void setEffect(EffectEnum e) {
		float[][] effect;
		switch (e) {
		case SEPIA:
			effect = this.sepia;
			this.currentEffectEnum = EffectEnum.SEPIA;
			break;
		case REDISH:
			effect = this.redish;
			this.currentEffectEnum = EffectEnum.REDISH;

			break;
		case GRAYSCALE:
			effect = this.grayscale;
			this.currentEffectEnum = EffectEnum.GRAYSCALE;

			break;
		case NEGATIVE:
			effect = this.negative;
			this.currentEffectEnum = EffectEnum.NEGATIVE;

			break;
		case DECAY:
			effect = this.decay;
			this.currentEffectEnum = EffectEnum.DECAY;

			break;
		default:
			effect = this.id;
			this.currentEffectEnum = EffectEnum.NORMAL;

		}

		if(effect != this.currentEffect) {
			this.addEffect(effect);
		}
	}

	public Sprite.EffectEnum getEffect() {
		return this.currentEffectEnum;
	}

	public boolean hasEffect(Sprite.EffectEnum effect) {
		return (effect != null) && (this.currentEffectEnum != null) && this.currentEffectEnum.equals(effect);
	}

	private void addEffect(float[][] effect) {
		float[][] rgb = new float[1][4];
		float[][] xrgb;
		for(int x = 0; x < this.w; x++) {
			for(int y = 0; y < this.h; y++) {
				int p = this.pixels[x + (y * this.w)];

				int a = (p >> 24) & 0xff;

				rgb[0][0] = (p >> 16) & 0xff;
				rgb[0][1] = (p >> 8) & 0xff;
				rgb[0][2] = (p) & 0xff;
				rgb[0][3] = 1f;

				xrgb = Matrix.multiply(rgb, effect);

				for(int i = 0; i < 3; i++) {
					if(xrgb[0][i] > 255) {
						rgb[0][i] = 255;
					} else if(xrgb[0][i] < 0) {
						rgb[0][i] = 0;
					} else {
						rgb[0][i] = xrgb[0][i];
					}
				}

				p = (a<<24) | ((int) rgb[0][0]<<16) | ((int) rgb[0][1]<<8) | (int) rgb[0][2];
				this.image.setRGB(x, y, p);
			}
		}
		this.currentEffect = effect;
	}

	public Sprite getSubimage(int x, int y, int w, int h) {
		return new Sprite(this.image.getSubimage(x, y, w, h));
	}

	public Sprite getNewSubimage(int x, int y, int w, int h) {
		BufferedImage temp = this.image.getSubimage(x, y, w, h);
		BufferedImage newImage = new BufferedImage(this.image.getColorModel(), this.image.getRaster().createCompatibleWritableRaster(w,h), this.image.isAlphaPremultiplied(), null);
		temp.copyData(newImage.getRaster());
		return new Sprite(newImage);
	}

	public Sprite getNewSubimage() {
		return this.getNewSubimage(0, 0, this.w, this.h);
	}

	@Override
	public Sprite clone() {
		return new Sprite(this.getImage());
	}
}