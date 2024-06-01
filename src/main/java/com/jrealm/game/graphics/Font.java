package com.jrealm.game.graphics;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Font {

    private BufferedImage FONTSHEET = null;
    private BufferedImage[][] spriteArray;
    private final int TILE_SIZE = 32;
    public int w;
    public int h;
    private int wLetter;
    private int hLetter;

    public Font(String file) {
	this.w = this.TILE_SIZE;
	this.h = this.TILE_SIZE;

	Font.log.info("Loading Font File {}", file);
	this.FONTSHEET = this.loadFont(file);

	this.wLetter = this.FONTSHEET.getWidth() / this.w;
	this.hLetter = this.FONTSHEET.getHeight() / this.h;
	this.loadFontArray();
    }

    public Font(String file, int w, int h) {
	this.w = w;
	this.h = h;

	Font.log.info("Loading Font File {}", file);
	this.FONTSHEET = this.loadFont(file);

	this.wLetter = this.FONTSHEET.getWidth() / w;
	this.hLetter = this.FONTSHEET.getHeight() / h;
	this.loadFontArray();
    }

    public void setSize(int width, int height) {
	this.setWidth(width);
	this.setHeight(height);
    }

    public void setWidth(int i) {
	this.w = i;
	this.wLetter = this.FONTSHEET.getWidth() / this.w;
    }

    public void setHeight(int i) {
	this.h = i;
	this.hLetter = this.FONTSHEET.getHeight() / this.h;
    }

    public int getWidth() {
	return this.w;
    }

    public int getHeight() {
	return this.h;
    }

    private BufferedImage loadFont(String file) {
	BufferedImage sprite = null;
	try {
	    sprite = ImageIO.read(this.getClass().getClassLoader().getResourceAsStream(file));
	} catch (Exception e) {
	    Font.log.error("ERROR: could not load file {} Reason: {}", file, e);
	}
	return sprite;
    }

    public void loadFontArray() {
	this.spriteArray = new BufferedImage[this.wLetter][this.hLetter];

	for (int x = 0; x < this.wLetter; x++) {
	    for (int y = 0; y < this.hLetter; y++) {
		this.spriteArray[x][y] = this.getLetter(x, y);
	    }
	}
    }

    public BufferedImage getFontSheet() {
	return this.FONTSHEET;
    }

    public BufferedImage getLetter(int x, int y) {
	BufferedImage img = this.FONTSHEET.getSubimage(x * this.w, y * this.h, this.w, this.h);
	return img;
    }

    public BufferedImage getLetter(char letter) {
	int value = letter;

	int x = value % this.wLetter;
	int y = value / this.wLetter;
	return this.getLetter(x, y);
    }
}
