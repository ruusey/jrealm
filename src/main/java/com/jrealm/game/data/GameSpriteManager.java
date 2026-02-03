package com.jrealm.game.data;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.model.SpriteModel;
import com.jrealm.game.model.TileModel;
import com.jrealm.net.client.ClientGameLogic;
import com.jrealm.net.server.ServerGameLogic;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameSpriteManager {

    private static final String[] SPRITE_NAMES = { "rotmg-classes.png", "rotmg-projectiles.png", "rotmg-bosses-1.png",
            "rotmg-bosses.png", "rotmg-items.png", "rotmg-tiles.png", "rotmg-tiles-1.png", "rotmg-tiles-2.png",
            "rotmg-tiles-all.png", "rotmg-items-1.png", "rotmg-abilities.png", "rotmg-misc.png", "buttons.png",
            "fillbars.png", "icons.png", "slots.png", "ui.png", "rotmg-tiles-1_0.png", "rotmg-tiles-1_.png",
            "rotmg-bosses-1_.png", "rotmg-classes-0.png", "rotmg-classes-1.png", "rotmg-classes-2.png",
            "rotmg-classes-3.png" };

    public static Map<String, Texture> TEXTURE_CACHE;
    public static Map<Integer, TextureRegion> TILE_SPRITES;
    public static Map<Integer, TextureRegion> ITEM_SPRITES;

    public static void loadItemSprites() {
        GameSpriteManager.ITEM_SPRITES = new HashMap<>();
        for (Integer gameItemId : GameDataManager.GAME_ITEMS.keySet()) {
            final GameItem model = GameDataManager.GAME_ITEMS.get(gameItemId);
            if (model.getSpriteSize() == 0) {
                model.setSpriteSize(GlobalConstants.BASE_SPRITE_SIZE);
            }
            final Texture spriteTexture = GameSpriteManager.TEXTURE_CACHE.get(model.getSpriteKey());
            if (spriteTexture == null) continue;
            TextureRegion subRegion = new TextureRegion(spriteTexture,
                    model.getCol() * model.getSpriteSize(),
                    model.getRow() * model.getSpriteSize(),
                    model.getSpriteSize(), model.getSpriteSize());
            subRegion.flip(false, true);
            GameSpriteManager.ITEM_SPRITES.put(gameItemId, subRegion);
        }
    }

    public static void loadTileSprites() {
        GameSpriteManager.TILE_SPRITES = new HashMap<>();
        for (Integer tileId : GameDataManager.TILES.keySet()) {
            final TileModel model = GameDataManager.TILES.get(tileId);
            if (model.getSpriteSize() == 0) {
                model.setSpriteSize(GlobalConstants.BASE_SPRITE_SIZE);
            }

            final Texture spriteTexture = GameSpriteManager.TEXTURE_CACHE.get(model.getSpriteKey());
            if (spriteTexture == null) continue;
            TextureRegion subRegion = new TextureRegion(spriteTexture,
                    model.getCol() * model.getSpriteSize(),
                    model.getRow() * model.getSpriteSize(),
                    model.getSpriteSize(), model.getSpriteSize());
            subRegion.flip(false, true);
            GameSpriteManager.TILE_SPRITES.put(tileId, subRegion);
        }
    }

    public static SpriteSheet getSpriteSheet(SpriteModel spriteModel) {
        SpriteSheet result = null;
        try {
            final Texture spriteTexture = GameSpriteManager.TEXTURE_CACHE.get(spriteModel.getSpriteKey());
            final SpriteSheet sheet = new SpriteSheet(spriteTexture, spriteModel);
            result = sheet;
        } catch (Exception e) {
            GameSpriteManager.log.error("Failed to build sprite sheet for sprite model {}. Reason: {}", spriteModel, e);
        }
        return result;
    }

    public static Sprite loadSprite(int x, int y, String file, int spriteSize) {
        final Texture texture = GameSpriteManager.TEXTURE_CACHE.get(file);
        final TextureRegion subRegion = new TextureRegion(texture, x * spriteSize, y * spriteSize, spriteSize, spriteSize);
        subRegion.flip(false, true);
        return new Sprite(subRegion);
    }

    public static Sprite loadSprite(SpriteModel model) {
        if (model.getSpriteSize() == 0) {
            model.setSpriteSize(GlobalConstants.BASE_SPRITE_SIZE);
        }
        final Texture texture = GameSpriteManager.TEXTURE_CACHE.get(model.getSpriteKey());
        final TextureRegion subRegion = new TextureRegion(texture,
                model.getCol() * model.getSpriteSize(),
                model.getRow() * model.getSpriteSize(),
                model.getSpriteSize(), model.getSpriteSize());
        subRegion.flip(false, true);
        return new Sprite(subRegion);
    }

    public static void loadSpriteImages(boolean loadRemote) {
        GameSpriteManager.TEXTURE_CACHE = new HashMap<>();
        try {
            for (final String spriteKey : GameSpriteManager.SPRITE_NAMES) {
                Texture texture = null;
                if (loadRemote) {
                    texture = GameSpriteManager.loadTextureRemote(spriteKey);
                } else {
                    texture = GameSpriteManager.loadTexture("entity/" + spriteKey);
                }
                if (texture == null) continue;
                GameSpriteManager.TEXTURE_CACHE.put(spriteKey, texture);
            }
        } catch (Exception e) {
            GameSpriteManager.log.error("Failed to load game sprites. Exiting. Reason: {}", e);
            System.exit(-1);
        }
    }

    private static Texture loadTexture(String file) {
        Texture texture = null;
        try {
            InputStream is = GameSpriteManager.class.getClassLoader().getResourceAsStream(file);
            if (is == null) {
                GameSpriteManager.log.error("ERROR: could not find file: {}", file);
                return null;
            }
            byte[] bytes = readAllBytes(is);
            Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
            texture = new Texture(pixmap);
            pixmap.dispose();
        } catch (Exception e) {
            GameSpriteManager.log.error("ERROR: could not load file: {}", file);
        }
        return texture;
    }

    private static Texture loadTextureRemote(String file) {
        Texture texture = null;
        try {
            String baseUrl = ClientGameLogic.DATA_SERVICE.getBaseUrl() == null
                    ? ServerGameLogic.DATA_SERVICE.getBaseUrl()
                    : ClientGameLogic.DATA_SERVICE.getBaseUrl();
            final URL imageUrl = new URL(baseUrl + "game-data/" + file);
            InputStream is = imageUrl.openStream();
            byte[] bytes = readAllBytes(is);
            is.close();
            Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
            texture = new Texture(pixmap);
            pixmap.dispose();
        } catch (Exception e) {
            GameSpriteManager.log.error("ERROR: could not load remote file: {}. Reason: {}", file, e.getMessage());
        }
        return texture;
    }

    private static byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    public static SpriteSheet loadClassSprites(CharacterClass cls) {
        Texture classTexture = GameSpriteManager.TEXTURE_CACHE.get("rotmg-classes.png");
        final SpriteSheet classSprites = new SpriteSheet(classTexture, GlobalConstants.BASE_SPRITE_SIZE,
                GlobalConstants.BASE_SPRITE_SIZE, 0, 4 * cls.classId);
        return classSprites;
    }

    public static void disposeAll() {
        if (TEXTURE_CACHE != null) {
            for (Texture t : TEXTURE_CACHE.values()) {
                t.dispose();
            }
            TEXTURE_CACHE.clear();
        }
    }
}
