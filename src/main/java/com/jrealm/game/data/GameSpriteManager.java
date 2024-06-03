package com.jrealm.game.data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.graphics.ImageUtils;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.model.SpriteModel;
import com.jrealm.game.model.TileModel;
import com.jrealm.net.server.ServerGameLogic;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameSpriteManager {

    private static final String[] SPRITE_NAMES = { "rotmg-classes.png", "rotmg-projectiles.png", "rotmg-bosses-1.png",
	    "rotmg-bosses.png", "rotmg-items.png", "rotmg-tiles.png", "rotmg-tiles-1.png", "rotmg-tiles-2.png",
	    "rotmg-tiles-all.png", "rotmg-items-1.png", "rotmg-abilities.png", "rotmg-misc.png", "buttons.png",
	    "fillbars.png", "icons.png", "slots.png", "ui.png" };

    public static Map<String, BufferedImage> IMAGE_CACHE;
    public static Map<Integer, BufferedImage> TILE_SPRITES;
    public static Map<Integer, BufferedImage> ITEM_SPRITES;

    public static void loadItemSprites() {
	GameSpriteManager.ITEM_SPRITES = new HashMap<>();
	for (Integer gameItemId : GameDataManager.GAME_ITEMS.keySet()) {
	    final GameItem model = GameDataManager.GAME_ITEMS.get(gameItemId);
	    if (model.getSpriteSize() == 0) {
		model.setSpriteSize(GlobalConstants.BASE_SPRITE_SIZE);
	    }
	    final BufferedImage spriteImage = GameSpriteManager.IMAGE_CACHE.get(model.getSpriteKey());
	    BufferedImage subImage = spriteImage.getSubimage(model.getCol() * model.getSpriteSize(),
		    model.getRow() * model.getSpriteSize(), model.getSpriteSize(), model.getSpriteSize());
	    //subImage = ImageUtils.generateBorder(subImage, 1, Color.BLACK, 0.2f);
	    subImage = ImageUtils.applyShadow(subImage, 1, Color.BLACK, 0.60f);

	    
	    GameSpriteManager.ITEM_SPRITES.put(gameItemId, subImage);
	}
    }

    public static void loadTileSprites() {
	GameSpriteManager.TILE_SPRITES = new HashMap<>();
	for (Integer tileId : GameDataManager.TILES.keySet()) {
	    final TileModel model = GameDataManager.TILES.get(tileId);
	    if (model.getSpriteSize() == 0) {
		model.setSpriteSize(GlobalConstants.BASE_SPRITE_SIZE);
	    }

	    final BufferedImage spriteImage = GameSpriteManager.IMAGE_CACHE.get(model.getSpriteKey());
	    BufferedImage subImage = spriteImage.getSubimage(model.getCol() * model.getSpriteSize(),
		    model.getRow() * model.getSpriteSize(), model.getSpriteSize(), model.getSpriteSize());
	    if (model.getData().hasCollision()) {
		subImage = ImageUtils.applyShadow(subImage, 1, Color.BLACK, 0.65f);
	    }
	
	    GameSpriteManager.TILE_SPRITES.put(tileId, subImage);
	}
    }

    public static SpriteSheet getSpriteSheet(SpriteModel spriteModel) {
	SpriteSheet result = null;
	try {
	    final BufferedImage spriteSheetImage = GameSpriteManager.IMAGE_CACHE.get(spriteModel.getSpriteKey());
	    final SpriteSheet sheet = new SpriteSheet(spriteSheetImage, spriteModel);
	    result = sheet;
	} catch (Exception e) {
	    GameSpriteManager.log.error("Failed to build sprite sheet for sprite model {}. Reason: {}", spriteModel, e);
	}

	return result;
    }

    public static Sprite loadSprite(int x, int y, String file, int spriteSize) {
	final BufferedImage classSpritesImage = GameSpriteManager.IMAGE_CACHE.get(file);
	final BufferedImage subImage = classSpritesImage.getSubimage(x * spriteSize, y * spriteSize, spriteSize,
		spriteSize);
	return new Sprite(subImage);
    }

    public static Sprite loadSprite(SpriteModel model) {
	if (model.getSpriteSize() == 0) {
	    model.setSpriteSize(GlobalConstants.BASE_SPRITE_SIZE);
	}
	final BufferedImage classSpritesImage = GameSpriteManager.IMAGE_CACHE.get(model.getSpriteKey());
	final BufferedImage subImage = classSpritesImage.getSubimage(model.getCol() * model.getSpriteSize(),
		model.getRow() * model.getSpriteSize(), model.getSpriteSize(), model.getSpriteSize());
	return new Sprite(subImage);
    }

    public static void loadSpriteImages(boolean loadRemote) {
	GameSpriteManager.IMAGE_CACHE = new HashMap<>();
	try {
	    for (final String spriteKey : GameSpriteManager.SPRITE_NAMES) {
		BufferedImage spriteImage = null;

		if (loadRemote) {
		    spriteImage = GameSpriteManager.loadSpriteRemote(spriteKey);
		} else {
		    spriteImage = GameSpriteManager.loadSprite(spriteKey);
		}
		BufferedImage mask = ImageUtils.generateMask(spriteImage, Color.GREEN,0.15f);
		spriteImage = ImageUtils.applyMask(spriteImage, mask, 10);

		GameSpriteManager.IMAGE_CACHE.put(spriteKey, spriteImage);
	    }
	} catch (Exception e) {
	    GameSpriteManager.log.error("Failed to load game sprites. Exiting. Reason: {}", e);
	    System.exit(-1);
	}
    }

    private static BufferedImage loadSprite(String file) {
	BufferedImage sprite = null;
	try {
	    sprite = ImageIO.read(GameSpriteManager.class.getClassLoader().getResourceAsStream(file));
	} catch (Exception e) {
	    GameSpriteManager.log.error("ERROR: could not load file: {}", file);
	}
	return sprite;
    }

    private static BufferedImage loadSpriteRemote(String file) {
	BufferedImage sprite = null;
	try {
	    final java.net.URL imageUrl = new java.net.URL(
		    ServerGameLogic.DATA_SERVICE.getBaseUrl() + "game-data/" + file);
	    sprite = ImageIO.read(imageUrl);
	} catch (Exception e) {
	    e.printStackTrace();
	    GameSpriteManager.log.error("ERROR: could not load file: {}", file);
	}
	return sprite;
    }

    public static SpriteSheet loadClassSprites(CharacterClass cls) {
	BufferedImage classSpritesImage = GameSpriteManager.IMAGE_CACHE.get("rotmg-classes.png");
	final SpriteSheet classSprites = new SpriteSheet(classSpritesImage, 0, 4 * cls.classId);
	return classSprites;
    }
}
