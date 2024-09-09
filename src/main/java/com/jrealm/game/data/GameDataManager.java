package com.jrealm.game.data;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.model.CharacterClassModel;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.ExperienceModel;
import com.jrealm.game.model.LootGroupModel;
import com.jrealm.game.model.LootTableModel;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.model.TerrainGenerationParameters;
import com.jrealm.game.model.TileModel;
import com.jrealm.net.client.ClientGameLogic;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameDataManager {
    public static final transient ObjectMapper JSON_MAPPER = new ObjectMapper();

    public static Map<Integer, ProjectileGroup> PROJECTILE_GROUPS = null;
    public static Map<Integer, GameItem> GAME_ITEMS = null;
    public static Map<Integer, EnemyModel> ENEMIES = null;
    public static Map<Integer, TileModel> TILES = null;
    public static Map<Integer, MapModel> MAPS = null;
    public static Map<Integer, TerrainGenerationParameters> TERRAINS = null;
    public static Map<Integer, PortalModel> PORTALS = null;
    public static Map<Integer, CharacterClassModel> CHARACTER_CLASSES = null;
    public static Map<Integer, LootTableModel> LOOT_TABLES = null;
    public static Map<Integer, LootGroupModel> LOOT_GROUPS = null;
    public static ExperienceModel EXPERIENCE_LVLS = null;

    private static void loadLootGroups(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading Loot Groups...");
        GameDataManager.LOOT_GROUPS = new HashMap<>();
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/loot-groups.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader()
                    .getResourceAsStream("data/loot-groups.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        LootGroupModel[] lootGroups = GameDataManager.JSON_MAPPER.readValue(text, LootGroupModel[].class);
        for (LootGroupModel lootGroup : lootGroups) {
            GameDataManager.LOOT_GROUPS.put(lootGroup.getLootGroupId(), lootGroup);
        }
        GameDataManager.log.info("Loading Loot Groups... DONE");
    }

    private static void loadLootTables(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading Loot Tables...");
        GameDataManager.LOOT_TABLES = new HashMap<>();
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/loot-tables.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader()
                    .getResourceAsStream("data/loot-tables.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        LootTableModel[] lootTables = GameDataManager.JSON_MAPPER.readValue(text, LootTableModel[].class);
        for (LootTableModel lootTable : lootTables) {
            GameDataManager.LOOT_TABLES.put(lootTable.getEnemyId(), lootTable);
        }
        GameDataManager.log.info("Loading Loot Tables... DONE");
    }

    private static void loadCharacterClasses(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading Character Classes...");
        GameDataManager.CHARACTER_CLASSES = new HashMap<>();
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/character-classes.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader()
                    .getResourceAsStream("data/character-classes.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        CharacterClassModel[] characterClasses = GameDataManager.JSON_MAPPER.readValue(text, CharacterClassModel[].class);
        for (CharacterClassModel characterClass : characterClasses) {
            GameDataManager.CHARACTER_CLASSES.put(characterClass.getClassId(), characterClass);
        }
        GameDataManager.log.info("Loading Character Classes... DONE");
    }

    private static void loadExperienceModel(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading ExperienceModel...");
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/exp-levels.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader()
                    .getResourceAsStream("data/exp-levels.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        ExperienceModel expModel = GameDataManager.JSON_MAPPER.readValue(text, ExperienceModel.class);
        expModel.parseMap();
        GameDataManager.EXPERIENCE_LVLS = expModel;
        GameDataManager.log.info("Loading ExperienceModel... DONE");
    }

    private static void loadPortals(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading Portals...");
        GameDataManager.PORTALS = new HashMap<>();
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/portals.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/portals.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        PortalModel[] maps = GameDataManager.JSON_MAPPER.readValue(text, PortalModel[].class);
        for (PortalModel map : maps) {
            GameDataManager.PORTALS.put(map.getPortalId(), map);
        }
        GameDataManager.log.info("Loading Portals... DONE");
    }

    private static void loadTerrains(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading Terrains...");
        GameDataManager.TERRAINS = new HashMap<>();
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/terrains.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/terrains.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        TerrainGenerationParameters[] maps = GameDataManager.JSON_MAPPER.readValue(text,
                TerrainGenerationParameters[].class);
        for (TerrainGenerationParameters map : maps) {
            GameDataManager.TERRAINS.put(map.getTerrainId(), map);
        }
        GameDataManager.log.info("Loading Terrains... DONE");
    }

    private static void loadMaps(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading Maps... ");
        GameDataManager.MAPS = new HashMap<>();
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/maps.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/maps.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        MapModel[] maps = GameDataManager.JSON_MAPPER.readValue(text, MapModel[].class);
        for (MapModel map : maps) {
            GameDataManager.MAPS.put(map.getMapId(), map);
        }
        GameDataManager.log.info("Loading Maps... DONE");
    }

    private static void loadTiles(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading Tiles...");
        GameDataManager.TILES = new HashMap<>();
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/tiles.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/tiles.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        TileModel[] tiles = GameDataManager.JSON_MAPPER.readValue(text, TileModel[].class);
        for (TileModel tile : tiles) {
            GameDataManager.TILES.put(tile.getTileId(), tile);
        }
        GameDataManager.log.info("Loading Tiles... DONE");
    }

    private static void loadEnemies(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading Enemies...");
        GameDataManager.ENEMIES = new HashMap<>();
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/enemies.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/enemies.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        EnemyModel[] enemies = GameDataManager.JSON_MAPPER.readValue(text, EnemyModel[].class);
        for (EnemyModel enemy : enemies) {
            GameDataManager.ENEMIES.put(enemy.getEnemyId(), enemy);
        }
        GameDataManager.log.info("Loading Enemies... DONE");
    }

    private static void loadProjectileGroups(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading Projectile Groups...");

        GameDataManager.PROJECTILE_GROUPS = new HashMap<>();
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/projectile-groups.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader()
                    .getResourceAsStream("data/projectile-groups.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        ProjectileGroup[] projectileGroups = GameDataManager.JSON_MAPPER.readValue(text, ProjectileGroup[].class);

        for (ProjectileGroup group : projectileGroups) {
            if ((group.getAngleOffset() != null) && group.getAngleOffset().contains("{{")) {
                group.setAngleOffset(GameDataManager.replaceInjectVariables(group.getAngleOffset()));
            } else {
                group.setAngleOffset("0");
            }
            for (Projectile p : group.getProjectiles()) {
                if (p.getAngle().contains("{{")) {
                    p.setAngle(GameDataManager.replaceInjectVariables(p.getAngle()));
                }
            }
            GameDataManager.PROJECTILE_GROUPS.put(group.getProjectileGroupId(), group);
        }
        GameDataManager.log.info("Loading Projectile Groups... DONE");

    }

    private static void loadGameItems(final boolean remote) throws Exception {
        GameDataManager.log.info("Loading Game Items...");

        GameDataManager.GAME_ITEMS = new HashMap<>();
        String text = null;
        if (remote) {
            text = ClientGameLogic.DATA_SERVICE.executeGet("game-data/game-items.json", null);
        } else {
            InputStream inputStream = GameDataManager.class.getClassLoader()
                    .getResourceAsStream("data/game-items.json");
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        GameItem[] gameItems = GameDataManager.JSON_MAPPER.readValue(text, GameItem[].class);

        for (GameItem item : gameItems) {
            GameDataManager.GAME_ITEMS.put(item.getItemId(), item);
        }
        GameDataManager.log.info("Loading Game Items... DONE");
    }

    // TODO: Add loot tier in LootContainer
    public static Sprite getLootSprite(int tier) {
        return GameSpriteManager.loadSprite(tier, 9, "rotmg-misc.png", GlobalConstants.BASE_SPRITE_SIZE);
    }

    public static Sprite getGraveSprite() {
        return GameSpriteManager.loadSprite(5, 5, "rotmg-bosses.png", GlobalConstants.MEDIUM_ART_SIZE);
    }

    public static Sprite getChestSprite() {
        return GameSpriteManager.loadSprite(2, 0, "rotmg-projectiles.png", GlobalConstants.BASE_SPRITE_SIZE);
    }

    public static Map<Integer, GameItem> getStartingEquipment(final CharacterClass characterClass) {
        final CharacterClassModel model = GameDataManager.CHARACTER_CLASSES.get(characterClass.classId);
        return model.getStartingEquipmentMap();
    }

    private static String replaceInjectVariables(String input) {
        String randomizeRegex = "\\{\\{(.*?)}}";
        Pattern pattern = Pattern.compile(randomizeRegex);
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String match = matcher.group(1);
            if (match.contains("PI/")) {
                String[] split = match.split("/");

                float multF = 1.0f;
                if (split[0].length() > 2) {
                    int endIndex = split[0].indexOf("P");
                    multF = Float.parseFloat(split[0].substring(0, endIndex));
                }
                float angle = (float) ((multF * Math.PI) / Float.parseFloat(split[1]));
                input = GameDataManager.replaceGen(input, match, angle + "");

            } else if (match.contains("PI")) {
                float angle = (float) Math.PI;
                input = GameDataManager.replaceGen(input, match, angle + "");
            }
        }
        return input;

    }

    public static void loadSpriteModel(GameItem item) {
        if (item.getItemId() > -1) {
            item.applySpriteModel(GameDataManager.GAME_ITEMS.get(item.getItemId()));
        }
    }

    public static String replaceGen(String source, String variable, String value) {
        final String text = source.replace("{{" + variable + "}}", value);
        return text;
    }

    public static void loadGameData(final boolean loadRemote) {
        GameDataManager.log.info("Loading Game Data from remote={}", loadRemote);
        try {
            GameDataManager.loadProjectileGroups(loadRemote);
            GameDataManager.loadGameItems(loadRemote);
            GameDataManager.loadEnemies(loadRemote);
            GameDataManager.loadTiles(loadRemote);
            GameDataManager.loadMaps(loadRemote);
            GameDataManager.loadTerrains(loadRemote);
            GameDataManager.loadPortals(loadRemote);
            GameDataManager.loadExperienceModel(loadRemote);
            GameDataManager.loadCharacterClasses(loadRemote);
            GameDataManager.loadLootTables(loadRemote);
            GameDataManager.loadLootGroups(loadRemote);
            GameSpriteManager.loadSpriteImages(loadRemote);
            GameSpriteManager.loadTileSprites();
            GameSpriteManager.loadItemSprites();
        } catch (Exception e) {
            GameDataManager.log.error("Failed to load game data. Reason: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String data = "";
        for (int i = 0; i < 32; i++) {
            data += "0,";
        }
        System.out.println(data);
    }
}
