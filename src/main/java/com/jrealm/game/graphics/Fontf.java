package com.jrealm.game.graphics;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j

public class Fontf {

    private HashMap<String, Font> fonts;

    public Fontf() {
	this.fonts = new HashMap<String, Font>();
    }

    public void loadFont(String path, String name) {
	try {
	    Fontf.log.info("Loading Font File {}", path);
	    Font customFont = Font.createFont(Font.TRUETYPE_FONT,
		    this.getClass().getClassLoader().getResourceAsStream(path));
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    ge.registerFont(customFont);

	    Font font = new Font(name, Font.PLAIN, 32);

	    this.fonts.put(name, font);
	} catch (Exception e) {
	    Fontf.log.error("ERROR: ttfFont - can't load font " + path + "...");
	    e.printStackTrace();
	}
    }

    public Font getFont(String name) {
	return this.fonts.get(name);
    }
}