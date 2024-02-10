package com.jrealm.game.data;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.model.CharacterClassModel;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.ExperienceModel;
import com.jrealm.game.model.LootGroupModel;
import com.jrealm.game.model.LootTableModel;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.model.SpriteModel;
import com.jrealm.game.model.TerrainGenerationParameters;
import com.jrealm.game.model.TileModel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameDataManager {
	private static final transient ObjectMapper mapper = new ObjectMapper();

	public static Map<Integer, ProjectileGroup> PROJECTILE_GROUPS = null;
	public static Map<Integer, GameItem> GAME_ITEMS = null;
	public static Map<Integer, EnemyModel> ENEMIES = null;
	public static Map<String, SpriteSheet> SPRITE_SHEETS = null;
	public static Map<Integer, TileModel> TILES = null;
	public static Map<Integer, MapModel> MAPS = null;
	public static Map<Integer, TerrainGenerationParameters> TERRAINS = null;
	public static Map<Integer, PortalModel> PORTALS = null;
	public static Map<Integer, CharacterClassModel> CHARACTER_CLASSES = null;
	public static Map<Integer, LootTableModel> LOOT_TABLES = null;
	public static Map<Integer, LootGroupModel> LOOT_GROUPS = null;
	public static ExperienceModel EXPERIENCE_LVLS = null;

	private static final String[] SPRITE_SHEET_LOCATIONS = { "entity/rotmg-classes.png", "entity/rotmg-projectiles.png",
			"entity/rotmg-bosses-1.png", "entity/rotmg-bosses.png", "entity/rotmg-items.png", "entity/rotmg-tiles.png",
			"entity/rotmg-tiles-1.png", "entity/rotmg-tiles-2.png", "entity/rotmg-tiles-all.png",
			"entity/rotmg-items-1.png", "entity/rotmg-abilities.png", "entity/rotmg-misc.png" };

	private static void loadLootGroups() throws Exception {
		GameDataManager.log.info("Loading Loot Groups..");
		GameDataManager.LOOT_GROUPS = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/loot-groups.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		LootGroupModel[] lootGroups = GameDataManager.mapper.readValue(text, LootGroupModel[].class);
		for (LootGroupModel lootGroup : lootGroups) {
			GameDataManager.LOOT_GROUPS.put(lootGroup.getLootGroupId(), lootGroup);
		}
		GameDataManager.log.info("Loading Loot Groups... DONE");
	}

	private static void loadLootTables() throws Exception {
		GameDataManager.log.info("Loading Loot Tables..");
		GameDataManager.LOOT_TABLES = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/loot-tables.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		LootTableModel[] lootTables = GameDataManager.mapper.readValue(text, LootTableModel[].class);
		for (LootTableModel lootTable : lootTables) {
			GameDataManager.LOOT_TABLES.put(lootTable.getEnemyId(), lootTable);
		}
		GameDataManager.log.info("Loading Loot Tables... DONE");
	}

	private static void loadCharacterClasses() throws Exception {
		GameDataManager.log.info("Loading Character Classes..");
		GameDataManager.CHARACTER_CLASSES = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader()
				.getResourceAsStream("data/character-classes.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		CharacterClassModel[] characterClasses = GameDataManager.mapper.readValue(text, CharacterClassModel[].class);
		for (CharacterClassModel characterClass : characterClasses) {
			GameDataManager.CHARACTER_CLASSES.put(characterClass.getClassId(), characterClass);
		}
		GameDataManager.log.info("Loading Character Classes... DONE");
	}

	private static void loadExperienceModel() throws Exception {
		GameDataManager.log.info("Loading ExperienceModel..");
		InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/exp-levels.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		ExperienceModel expModel = GameDataManager.mapper.readValue(text, ExperienceModel.class);
		expModel.parseMap();
		GameDataManager.EXPERIENCE_LVLS = expModel;
		GameDataManager.log.info("Loading ExperienceModel... DONE");
	}

	private static void loadPortals() throws Exception {
		GameDataManager.log.info("Loading Portals..");
		GameDataManager.PORTALS = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/portals.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		PortalModel[] maps = GameDataManager.mapper.readValue(text, PortalModel[].class);
		for (PortalModel map : maps) {
			GameDataManager.PORTALS.put(map.getPortalId(), map);
		}
		GameDataManager.log.info("Loading Portals... DONE");
	}

	private static void loadTerrains() throws Exception {
		GameDataManager.log.info("Loading Terrains..");
		GameDataManager.TERRAINS = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/terrains.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		TerrainGenerationParameters[] maps = GameDataManager.mapper.readValue(text,
				TerrainGenerationParameters[].class);
		for (TerrainGenerationParameters map : maps) {
			GameDataManager.TERRAINS.put(map.getTerrainId(), map);
		}
		GameDataManager.log.info("Loading Terrains... DONE");
	}

	private static void loadMaps() throws Exception {
		GameDataManager.log.info("Loading Maps..");
		GameDataManager.MAPS = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader()
				.getResourceAsStream("data/maps.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		MapModel[] maps = GameDataManager.mapper.readValue(text, MapModel[].class);
		for(MapModel map : maps) {
			GameDataManager.MAPS.put(map.getMapId(), map);
		}
		GameDataManager.log.info("Loading Maps... DONE");
	}

	private static void loadTiles() throws Exception {
		GameDataManager.log.info("Loading Tiles..");
		GameDataManager.TILES = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader()
				.getResourceAsStream("data/tiles.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		TileModel[] tiles = GameDataManager.mapper.readValue(text, TileModel[].class);
		for(TileModel tile : tiles) {
			GameDataManager.TILES.put(tile.getTileId(), tile);
		}
		GameDataManager.log.info("Loading Tiles... DONE");
	}

	private static void loadEnemies() throws Exception {
		GameDataManager.log.info("Loading Enemies..");
		GameDataManager.ENEMIES = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader()
				.getResourceAsStream("data/enemies.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		EnemyModel[] enemies = GameDataManager.mapper.readValue(text, EnemyModel[].class);
		for(EnemyModel enemy : enemies) {
			GameDataManager.ENEMIES.put(enemy.getEnemyId(), enemy);
		}
		GameDataManager.log.info("Loading Enemies... DONE");
	}

	private static void loadProjectileGroups() throws Exception {
		GameDataManager.log.info("Loading Projectile Groups...");

		GameDataManager.PROJECTILE_GROUPS = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader()
				.getResourceAsStream("data/projectile-groups.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		ProjectileGroup[] projectileGroups = GameDataManager.mapper.readValue(text, ProjectileGroup[].class);

		for (ProjectileGroup group : projectileGroups) {
			if ((group.getAngleOffset() != null) && group.getAngleOffset().contains("{{")) {
				group.setAngleOffset(GameDataManager.replaceInjectVariables(group.getAngleOffset()));
			}
			for(Projectile p : group.getProjectiles()) {
				if(p.getAngle().contains("{{")) {
					p.setAngle(GameDataManager.replaceInjectVariables(p.getAngle()));
				}
			}
			GameDataManager.PROJECTILE_GROUPS.put(group.getProjectileGroupId(), group);
		}
		GameDataManager.log.info("Loading Projectile Groups... DONE");

	}

	private static void loadGameItems() throws Exception {
		GameDataManager.log.info("Loading Game Items...");

		GameDataManager.GAME_ITEMS = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/game-items.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		GameItem[] gameItems = GameDataManager.mapper.readValue(text, GameItem[].class);

		for (GameItem item : gameItems) {
			GameDataManager.GAME_ITEMS.put(item.getItemId(), item);
		}
		GameDataManager.log.info("Loading Game Items... DONE");
	}

	// TODO: Add loot tier in LootContainer
	public static Sprite getLootSprite(int tier) {
		return GameDataManager.SPRITE_SHEETS.get("entity/rotmg-misc.png").getSprite(tier, 9, 8, 8);
	}

	private static void loadSpriteSheets() throws Exception {
		GameDataManager.log.info("Loading Sprite Sheets...");

		GameDataManager.SPRITE_SHEETS = new HashMap<>();
		for (String loc : GameDataManager.SPRITE_SHEET_LOCATIONS) {
			switch(loc) {
			case "entity/rotmg-classes.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-classes.png",
						GameDataManager.loadClassSprites(CharacterClass.ARCHER));
				break;
			case "entity/rotmg-projectiles.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-projectiles.png",
						new SpriteSheet("entity/rotmg-projectiles.png", 8, 8, 0));
				break;
			case "entity/rotmg-items.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-items.png",
						new SpriteSheet("entity/rotmg-items.png", 8, 8, 0));
				break;
			case "entity/rotmg-misc.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-misc.png",
						new SpriteSheet("entity/rotmg-misc.png", 8, 8, 0));
				break;
			case "entity/rotmg-abilities.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-abilities.png",
						new SpriteSheet("entity/rotmg-abilities.png", 8, 8, 0));
				break;
			case "entity/rotmg-items-1.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-items-1.png",
						new SpriteSheet("entity/rotmg-items-1.png", 8, 8, 0));
				break;
			case "entity/rotmg-bosses.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-bosses.png",
						new SpriteSheet("entity/rotmg-bosses.png", 16, 16, 0));
				break;
			case "entity/rotmg-bosses-1.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-bosses-1.png",
						new SpriteSheet("entity/rotmg-bosses-1.png", 8, 8, 0));
				break;
			case "entity/rotmg-tiles.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-tiles.png",
						new SpriteSheet("entity/rotmg-tiles.png", 8, 8, 0));
				break;
			case "entity/rotmg-tiles-1.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-tiles-1.png",
						new SpriteSheet("entity/rotmg-tiles-1.png", 8, 8, 0));
				break;
			case "entity/rotmg-tiles-2.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-tiles-2.png",
						new SpriteSheet("entity/rotmg-tiles-2.png", 8, 8, 0));
				break;
			case "entity/rotmg-tiles-all.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-tiles-all.png",
						new SpriteSheet("entity/rotmg-tiles-all.png", 8, 8, 0));
				break;
			}
		}
		GameDataManager.log.info("Loading Sprite Sheets... DONE");
	}

	public static Map<Integer, GameItem> getStartingEquipment(final CharacterClass characterClass) {
		Map<Integer, GameItem> result = new HashMap<>();

		switch (characterClass) {
		case ROGUE:
			result.put(0, GameDataManager.GAME_ITEMS.get(49));
			result.put(1, GameDataManager.GAME_ITEMS.get(152));
			result.put(2, GameDataManager.GAME_ITEMS.get(32));
			result.put(3, GameDataManager.GAME_ITEMS.get(48));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		case ARCHER:
			result.put(0, GameDataManager.GAME_ITEMS.get(17));
			result.put(1, GameDataManager.GAME_ITEMS.get(154));
			result.put(2, GameDataManager.GAME_ITEMS.get(32));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(0));
			break;
		case WIZARD:
			result.put(0, GameDataManager.GAME_ITEMS.get(121));
			result.put(1, GameDataManager.GAME_ITEMS.get(136));
			result.put(2, GameDataManager.GAME_ITEMS.get(106));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		case PRIEST:
			result.put(0, GameDataManager.GAME_ITEMS.get(137));
			result.put(1, GameDataManager.GAME_ITEMS.get(157));
			result.put(2, GameDataManager.GAME_ITEMS.get(106));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		case WARRIOR:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(1, GameDataManager.GAME_ITEMS.get(156));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		case KNIGHT:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(1, GameDataManager.GAME_ITEMS.get(155));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		case PALLADIN:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(1, GameDataManager.GAME_ITEMS.get(153));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		default:
			break;
		}
		return result;
	}

	public static SpriteSheet loadClassSprites(CharacterClass cls) {
		SpriteSheet sheet = new SpriteSheet("entity/rotmg-classes.png", 8, 8, 4 * cls.classId);
		return sheet;
	}

	private static String replaceInjectVariables(String input) {
		String randomizeRegex = "\\{\\{(.*?)}}";
		Pattern pattern = Pattern.compile(randomizeRegex);
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) {
			String match = matcher.group(1);
			if(match.contains("PI/")) {
				String[] split = match.split("/");

				float multF = 1.0f;
				if (split[0].length() > 2) {
					int endIndex = split[0].indexOf("P");
					multF = Float.parseFloat(split[0].substring(0, endIndex));
				}
				float angle = (float) ((multF * Math.PI) / Float.parseFloat(split[1]));
				input = GameDataManager.replaceGen(input, match, angle+"");

			} else if (match.contains("PI")) {
				float angle = (float) Math.PI;
				input = GameDataManager.replaceGen(input, match, angle + "");
			}
		}
		return input;

	}

	public static Sprite getSubSprite(String spriteKey, int col, int row, int size) {
		SpriteSheet sheet = GameDataManager.SPRITE_SHEETS.get(spriteKey);
		if(sheet==null)
			return null;
		return sheet.getSprite(col, row, size, size);
	}

	public static void loadSpriteModel(GameItem item){
		if(item.getItemId()>-1) {
			item.applySpriteModel(GameDataManager.GAME_ITEMS.get(item.getItemId()));
		}
	}

	public static Sprite getSubSprite(SpriteModel model, int size) {
		return GameDataManager.getSubSprite(model.getSpriteKey(), model.getCol(), model.getRow(), size);
	}

	public static String replaceGen(String source, String variable, String value) {
		String text = source.replace("{{" + variable + "}}", value);
		return text;
	}

	public static void loadGameData() {
		GameDataManager.log.info("Loading Game Data...");
		try {
			GameDataManager.loadProjectileGroups();
			GameDataManager.loadGameItems();
			GameDataManager.loadSpriteSheets();
			GameDataManager.loadEnemies();
			GameDataManager.loadTiles();
			GameDataManager.loadMaps();
			GameDataManager.loadTerrains();
			GameDataManager.loadPortals();
			GameDataManager.loadExperienceModel();
			GameDataManager.loadCharacterClasses();
			GameDataManager.loadLootTables();
			GameDataManager.loadLootGroups();
		}catch(Exception e) {
			GameDataManager.log.error("Failed to load game data. Reason: " + e.getMessage());
		}
	}

	public static void main(String[] args) {
		String data = "";
		for(int i = 0; i < 32 ; i++) {
			data+="0,";
		}
		System.out.println(data);
	}
}
