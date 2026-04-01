package com.jrealm.game.data;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
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
import com.jrealm.game.model.CharacterClassModel;
import com.jrealm.game.model.SpriteModel;
import com.jrealm.game.model.TileModel;
import com.jrealm.net.client.ClientGameLogic;
import com.jrealm.net.server.ServerGameLogic;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameSpriteManager {

    private static final String[] SPRITE_NAMES = {
            "rotmg-projectiles.png", "rotmg-projectiles-1.png",
            "rotmg-bosses.png", "rotmg-bosses-1.png", "rotmg-bosses-1_.png",
            "rotmg-items.png", "rotmg-items-1.png",
            "rotmg-tiles.png", "rotmg-tiles-1.png", "rotmg-tiles-2.png", "rotmg-tiles-all.png",
            "rotmg-abilities.png", "rotmg-misc.png",
            "rotmg-classes-0.png", "rotmg-classes-1.png", "rotmg-classes-2.png", "rotmg-classes-3.png",
            "lofi_char.png", "lofi_environment.png", "lofi_obj.png",
            "chars8x8rBeach.png", "chars8x8rHero2.png", "cursedLibraryChars16x16.png",
            "d3Chars8x8r.png", "cursedLibraryChars8x8.png",
            "archbishopObjects16x16.png", "autumnNexusObjects16x16.png",
            "chars16x16dEncounters2.png", "crystalCaveChars16x16.png",
            "epicHiveChars8x8.png", "lairOfDraconisChars8x8.png",
            "oryxHordeChars16x16.png", "oryxHordeChars8x8.png",
            "secludedThicketChars16x16.png" };

    public static Map<String, Texture> TEXTURE_CACHE;
    public static Map<Integer, TextureRegion> TILE_SPRITES;
    public static Map<Integer, TextureRegion> ITEM_SPRITES;

    public static void loadItemSprites() {
        if (GameSpriteManager.TEXTURE_CACHE == null) return;
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
        if (GameSpriteManager.TEXTURE_CACHE == null) return;
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
        if (GameSpriteManager.TEXTURE_CACHE == null) {
            return null;
        }
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
        if (GameSpriteManager.TEXTURE_CACHE == null) {
            return null;
        }
        final Texture texture = GameSpriteManager.TEXTURE_CACHE.get(file);
        if (texture == null) {
            return null;
        }
        final TextureRegion subRegion = new TextureRegion(texture, x * spriteSize, y * spriteSize, spriteSize, spriteSize);
        subRegion.flip(false, true);
        return new Sprite(subRegion);
    }

    public static Sprite loadSprite(SpriteModel model) {
        if (GameSpriteManager.TEXTURE_CACHE == null) {
            return null;
        }
        if (model.getSpriteSize() == 0) {
            model.setSpriteSize(GlobalConstants.BASE_SPRITE_SIZE);
        }
        final Texture texture = GameSpriteManager.TEXTURE_CACHE.get(model.getSpriteKey());
        if (texture == null) {
            return null;
        }
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
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
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
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
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

    private static final String[] CLASS_SHEET_NAMES = {
        "rotmg-classes-0.png", "rotmg-classes-1.png", "rotmg-classes-2.png", "rotmg-classes-3.png"
    };

    private static String getClassSheetName(int classId) {
        int sheetIndex = classId / 3;
        if (sheetIndex >= 0 && sheetIndex < CLASS_SHEET_NAMES.length) {
            return CLASS_SHEET_NAMES[sheetIndex];
        }
        return CLASS_SHEET_NAMES[0];
    }

    public static SpriteSheet loadClassSprites(CharacterClass cls) {
        if (GameSpriteManager.TEXTURE_CACHE == null) return null;

        CharacterClassModel classModel = GameDataManager.CHARACTER_CLASSES.get(cls.classId);
        if (classModel == null) return null;
        String sheetName = getClassSheetName(cls.classId);
        Texture classTexture = GameSpriteManager.TEXTURE_CACHE.get(sheetName);
        if (classTexture == null) return null;

        // Each sheet has 3 classes. spriteOffset is the global row; compute local row within the sheet.
        // Sheet 0: global rows 0-11, Sheet 1: 12-23, Sheet 2: 24-33, Sheet 3: 34-42
        int sheetIndex = cls.classId / 3;
        int[] sheetBaseOffsets = {0, 12, 24, 34};
        int localRow = classModel.getSpriteOffset() - sheetBaseOffsets[sheetIndex];

        final SpriteSheet classSprites = new SpriteSheet(classTexture, GlobalConstants.BASE_SPRITE_SIZE,
                GlobalConstants.BASE_SPRITE_SIZE, 0, localRow);

        // Sprite layout: 2 rows per direction, cols 0-2 contain main animation frames.
        // Row 0: Side/primary walk (cols 0-1 = walk frame 1, walk frame 2)
        // Row 1: Side/primary attack (cols 0-2 = 3 attack frames)
        // Row 2: Back direction (no face visible)
        // Row 3: Front direction (4-row classes only; classId <= 6)
        // For 3-row classes (classId >= 7): front reuses side (rows 0-1).
        int walkDur = 8;
        int atkDur = 5;
        boolean hasFrontRow = (cls.classId <= 6);
        int frontRow = hasFrontRow ? localRow + 3 : localRow;

        // Side idle + walk: Row 0, cols 0-1
        classSprites.addAnimSet("idle_side",
            Arrays.asList(classSprites.getSubSprite(0, localRow)),
            Arrays.asList(999));
        classSprites.addAnimSet("walk_side",
            Arrays.asList(
                classSprites.getSubSprite(0, localRow),
                classSprites.getSubSprite(1, localRow)),
            Arrays.asList(walkDur, walkDur));

        // Side attack (shoot left/right): Row 0, cols 4-5
        classSprites.addAnimSet("attack_side",
            Arrays.asList(
                classSprites.getSubSprite(0, localRow),
                classSprites.getSubSprite(4, localRow),
                classSprites.getSubSprite(5, localRow)),
            Arrays.asList(atkDur, atkDur, atkDur));

        // Down attack (shoot downward, mouse below player): Row 1, cols 4-5
        classSprites.addAnimSet("attack_down",
            Arrays.asList(
                classSprites.getSubSprite(0, localRow + 1),
                classSprites.getSubSprite(4, localRow + 1),
                classSprites.getSubSprite(5, localRow + 1)),
            Arrays.asList(atkDur, atkDur, atkDur));

        // Up attack (shoot upward, mouse above player): Row 2, cols 4-5
        classSprites.addAnimSet("attack_up",
            Arrays.asList(
                classSprites.getSubSprite(0, localRow + 2),
                classSprites.getSubSprite(4, localRow + 2),
                classSprites.getSubSprite(5, localRow + 2)),
            Arrays.asList(atkDur, atkDur, atkDur));

        // Front idle + walk: Row 3 for 4-row classes, or reuse Row 0 for 3-row classes
        classSprites.addAnimSet("idle_front",
            Arrays.asList(classSprites.getSubSprite(0, frontRow)),
            Arrays.asList(999));
        classSprites.addAnimSet("walk_front",
            Arrays.asList(
                classSprites.getSubSprite(0, frontRow),
                classSprites.getSubSprite(1, frontRow)),
            Arrays.asList(walkDur, walkDur));

        // Keep attack_front as alias for attack_down (backwards compat)
        classSprites.addAnimSet("attack_front",
            Arrays.asList(
                classSprites.getSubSprite(0, localRow + 1),
                classSprites.getSubSprite(4, localRow + 1),
                classSprites.getSubSprite(5, localRow + 1)),
            Arrays.asList(atkDur, atkDur, atkDur));

        classSprites.setAnimSet("idle_side");
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
