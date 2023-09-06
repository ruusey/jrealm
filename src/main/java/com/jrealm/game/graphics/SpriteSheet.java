package com.jrealm.game.graphics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.jrealm.game.math.Vector2f;

public class SpriteSheet {

	private Sprite SPRITESHEET = null;
	private Sprite[][] spriteArray;
	private final int TILE_SIZE = 32;
	public int w;
	public int h;
	private int wSprite;
	private int hSprite;
	private String file;
	private int rowOffset;

	public static Font currentFont;

	public SpriteSheet(String file, int rowOffset) {
		this.file = file;
		this.w = this.TILE_SIZE;
		this.h = this.TILE_SIZE;

		System.out.println("Loading: " + file + "...");
		this.SPRITESHEET = new Sprite(this.loadSprite(file));

		this.wSprite = this.SPRITESHEET.image.getWidth() / this.w;
		this.hSprite = this.SPRITESHEET.image.getHeight() / this.h;
		this.loadSpriteArray();
		this.rowOffset = rowOffset;
	}

	public SpriteSheet(Sprite sprite, String name, int w, int h, int rowOffset) {
		this.w = w;
		this.h = h;

		System.out.println("Loading: " + name + "...");
		this.SPRITESHEET = sprite;

		this.wSprite = this.SPRITESHEET.image.getWidth() / w;
		this.hSprite = this.SPRITESHEET.image.getHeight() / h;
		this.loadSpriteArray();
		this.rowOffset = rowOffset;

	}

	public SpriteSheet(String file, int w, int h, int rowOffset) {
		this.w = w;
		this.h = h;
		this.file = file;

		System.out.println("Loading: " + file + "...");
		this.SPRITESHEET = new Sprite(this.loadSprite(file));

		this.wSprite = this.SPRITESHEET.image.getWidth() / w;
		this.hSprite = this.SPRITESHEET.image.getHeight() / h;
		this.loadSpriteArray();
		this.rowOffset = rowOffset;

	}

	public void setSize(int width, int height) {
		this.setWidth(width);
		this.setHeight(height);
	}

	public void setWidth(int i) {
		this.w = i;
		this.wSprite = this.SPRITESHEET.image.getWidth() / this.w;
	}

	public void setHeight(int i) {
		this.h = i;
		this.hSprite = this.SPRITESHEET.image.getHeight() / this.h;
	}

	public int getWidth() { return this.w; }
	public int getHeight() { return this.h; }
	public int getRows() { return this.hSprite; }
	public int getCols() { return this.wSprite; }
	public int getTotalTiles() { return this.wSprite * this.hSprite; }
	public String getFilename() { return this.file; }

	public int getRowOffset() {
		return this.rowOffset;
	}
	private BufferedImage loadSprite(String file) {
		BufferedImage sprite = null;
		try {
			sprite = ImageIO.read(this.getClass().getClassLoader().getResourceAsStream(file));
		} catch (Exception e) {
			System.out.println("ERROR: could not load file: " + file);
		}
		return sprite;
	}

	public void loadSpriteArray() {
		this.spriteArray = new Sprite[this.hSprite][this.wSprite];

		for (int y = 0; y < this.hSprite; y++) {
			for (int x = 0; x < this.wSprite; x++) {
				this.spriteArray[y][x] = this.getSprite(x, y);
			}
		}
	}

	public void setEffect(Sprite.effect e) {
		this.SPRITESHEET.setEffect(e);
	}

	public Sprite getSpriteSheet() {
		return this.SPRITESHEET;
	}

	public Sprite getSprite(int x, int y) {
		return this.SPRITESHEET.getSubimage(x * this.w, y * this.h, this.w, this.h);
	}

	public Sprite getNewSprite(int x, int y) {
		return this.SPRITESHEET.getNewSubimage(x * this.w, y * this.h, this.w, this.h);
	}

	public Sprite getSprite(int x, int y, int w, int h) {
		return this.SPRITESHEET.getSubimage(x * w, y * h, w, h);
	}

	public BufferedImage getSubimage(int x, int y, int w, int h) {
		return this.SPRITESHEET.image.getSubimage(x, y, w, h);
	}

	public Sprite[] getSpriteArray(int i) {
		if ((this.spriteArray != null) && (this.spriteArray.length == 1))
			return this.spriteArray[0];
		return this.spriteArray[i];
	}

	public Sprite[] getSpriteArray(int i, int offset) {
		if ((this.spriteArray != null) && (this.spriteArray.length == 1))
			return this.spriteArray[0];
		return Arrays.copyOfRange(this.spriteArray[i], 0 + offset, this.spriteArray[i].length);
	}

	public Sprite[][] getSpriteArray2() {
		return this.spriteArray;
	}

	public static void drawArray(Graphics2D g, ArrayList<Sprite> img, Vector2f pos, int width, int height, int xOffset, int yOffset) {
		float x = pos.x;
		float y = pos.y;

		for (int i = 0; i < img.size(); i++) {
			if (img.get(i) != null) {
				g.drawImage(img.get(i).image, (int) x, (int) y, width, height, null);
			}

			x += xOffset;
			y += yOffset;
		}
	}

	public static void drawArray(Graphics2D g, String word, Vector2f pos, int size) {
		SpriteSheet.drawArray(g, SpriteSheet.currentFont, word, pos, size, size, size, 0);
	}

	public static void drawArray(Graphics2D g, String word, Vector2f pos, int size, int xOffset) {
		SpriteSheet.drawArray(g, SpriteSheet.currentFont, word, pos, size, size, xOffset, 0);
	}

	public static void drawArray(Graphics2D g, String word, Vector2f pos, int width, int height, int xOffset) {
		SpriteSheet.drawArray(g, SpriteSheet.currentFont, word, pos, width, height, xOffset, 0);
	}

	public static void drawArray(Graphics2D g, Font f, String word, Vector2f pos, int size, int xOffset) {
		SpriteSheet.drawArray(g, f, word, pos, size, size, xOffset, 0);
	}

	public static void drawArray(Graphics2D g, Font f, String word, Vector2f pos, int width, int height, int xOffset, int yOffset) {
		float x = pos.x;
		float y = pos.y;

		SpriteSheet.currentFont = f;

		for (int i = 0; i < word.length(); i++) {
			if (word.charAt(i) != 32) {
				g.drawImage(f.getLetter(word.charAt(i)), (int) x, (int) y, width, height, null);
			}

			x += xOffset;
			y += yOffset;
		}

	}

}
