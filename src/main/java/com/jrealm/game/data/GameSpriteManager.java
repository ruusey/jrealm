package com.jrealm.game.data;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheetNew;
import com.jrealm.game.model.SpriteModel;
import com.jrealm.game.model.TileModel;
import com.jrealm.net.server.ServerGameLogic;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameSpriteManager {

	private static final String[] SPRITE_NAMES = { "rotmg-classes.png", "rotmg-projectiles.png", "rotmg-bosses-1.png",
			"rotmg-bosses.png", "rotmg-items.png", "rotmg-tiles.png", "rotmg-tiles-1.png", "rotmg-tiles-2.png",
			"rotmg-tiles-all.png", "rotmg-items-1.png", "rotmg-abilities.png",
	"rotmg-misc.png", "buttons.png", "fillbars.png", "icons.png", "slots.png", "ui.png" };

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
			final BufferedImage subImage = spriteImage.getSubimage(model.getCol() * model.getSpriteSize(),
					model.getRow() * model.getSpriteSize(),
					model.getSpriteSize(), model.getSpriteSize());
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
			final BufferedImage subImage = spriteImage.getSubimage(model.getCol() * model.getSpriteSize(),
					model.getRow() * model.getSpriteSize(),
					model.getSpriteSize(), model.getSpriteSize());
			GameSpriteManager.TILE_SPRITES.put(tileId, subImage);
		}
	}

	public static SpriteSheetNew getSpriteSheet(SpriteModel spriteModel) {
		SpriteSheetNew result = null;
		try {
			final BufferedImage spriteSheetImage = GameSpriteManager.IMAGE_CACHE.get(spriteModel.getSpriteKey());
			final SpriteSheetNew sheet = new SpriteSheetNew(spriteSheetImage, spriteModel);
			result = sheet;
		} catch (Exception e) {
			GameSpriteManager.log.error("Failed to build sprite sheet for sprite model {}. Reason: {}", spriteModel, e);
		}

		return result;
	}

	public static Sprite loadSprite(int x, int y, String file, int spriteSize) {
		final BufferedImage classSpritesImage = GameSpriteManager.IMAGE_CACHE.get(file);
		final BufferedImage subImage = classSpritesImage.getSubimage(x * spriteSize, y * spriteSize, spriteSize, spriteSize);
		return new Sprite(subImage);
	}

	public static Sprite loadSprite(SpriteModel model) {
		if (model.getSpriteSize() == 0) {
			model.setSpriteSize(GlobalConstants.BASE_SPRITE_SIZE);
		}
		final BufferedImage classSpritesImage = GameSpriteManager.IMAGE_CACHE.get(model.getSpriteKey());
		final BufferedImage subImage = classSpritesImage.getSubimage(model.getCol() * model.getSpriteSize(),
				model.getRow() * model.getSpriteSize(), model.getSpriteSize(),
				model.getSpriteSize());
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
				GameSpriteManager.IMAGE_CACHE.put(spriteKey, spriteImage);
			}
		}catch(Exception e) {
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
			final java.net.URL imageUrl = new java.net.URL(ServerGameLogic.DATA_SERVICE.getBaseUrl() + file);
			sprite = ImageIO.read(imageUrl);
		} catch (Exception e) {
			GameSpriteManager.log.error("ERROR: could not load file: {}", file);
		}
		return sprite;
	}

	public static SpriteSheetNew loadClassSprites(CharacterClass cls) {
		final BufferedImage classSpritesImage = GameSpriteManager.IMAGE_CACHE.get("rotmg-classes.png");
		final SpriteSheetNew classSprites = new SpriteSheetNew(classSpritesImage, 0, 4 * cls.classId);
		return classSprites;
	}
}
