package com.jrealm.game.data;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.model.SpriteModel;

public class GameDataManager {
	private static final transient ObjectMapper mapper = new ObjectMapper();

	public static SpriteSheet PLAYER_SPRITESHEET = null;

	public static Map<Integer, ProjectileGroup> PROJECTILE_GROUPS = null;
	public static Map<Integer, GameItem> GAME_ITEMS = null;
	public static Map<String, SpriteSheet> SPRITE_SHEETS = null;

	private static final String[] SPRITE_SHEET_LOCATIONS = { "material/trees.png", "tile/overworldOP.png",
			"entity/rotmg-classes.png", "entity/rotmg-projectiles.png",
			"entity/rotmg-bosses.png", "entity/rotmg-items.png", "entity/rotmg-items-1.png" };

	private static void loadProjectileGroups() throws Exception {
		System.out.println("Loading Projectile Groups...");

		GameDataManager.PROJECTILE_GROUPS = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader()
				.getResourceAsStream("data/projectile-groups.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		//replaceInjectVariables(text);
		ProjectileGroup[] projectileGroups = GameDataManager.mapper.readValue(text, ProjectileGroup[].class);

		for (ProjectileGroup group : projectileGroups) {
			for(Projectile p : group.getProjectiles()) {
				if(p.getAngle().contains("{{")) {
					p.setAngle(GameDataManager.replaceInjectVariables(p.getAngle()));
				} else if ((group.getAngleOffset() != null) && group.getAngleOffset().contains("{{")) {
					group.setAngleOffset(GameDataManager.replaceInjectVariables(group.getAngleOffset()));
				}
			}
			GameDataManager.PROJECTILE_GROUPS.put(group.getProjectileGroupId(), group);
		}
		System.out.println("Projectile Groups... DONE");

	}

	private static void loadGameItems() throws Exception {
		System.out.println("Loading Game Items...");

		GameDataManager.GAME_ITEMS = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader().getResourceAsStream("data/game-items.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		// replaceInjectVariables(text);
		GameItem[] gameItems = GameDataManager.mapper.readValue(text, GameItem[].class);

		for (GameItem item : gameItems) {
			GameDataManager.GAME_ITEMS.put(item.getItemId(), item);
		}
		System.out.println("Game Items... DONE");

	}

	private static void loadSpriteSheets() throws Exception {
		System.out.println("Loading Sprite Sheets...");

		GameDataManager.SPRITE_SHEETS = new HashMap<>();
		for (String loc : GameDataManager.SPRITE_SHEET_LOCATIONS) {
			switch(loc) {
			case "tile/overworldOP.png":
				GameDataManager.SPRITE_SHEETS.put("tile/overworldOP.png",
						new SpriteSheet("tile/overworldOP.png", 32, 32, 0));

				break;
			case "material/trees.png":
				GameDataManager.SPRITE_SHEETS.put("material/trees.png",
						new SpriteSheet("material/trees.png", 64, 96, 0));
				break;
			case "entity/rotmg-classes.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-classes.png",
						new SpriteSheet("entity/rotmg-classes.png", 8, 8, 4));
				break;
			case "entity/rotmg-projectiles.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-projectiles.png",
						new SpriteSheet("entity/rotmg-projectiles.png", 8, 8, 0));
				break;
			case "entity/rotmg-items.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-items.png",
						new SpriteSheet("entity/rotmg-items.png", 8, 8, 0));
				break;
			case "entity/rotmg-items-1.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-items-1.png",
						new SpriteSheet("entity/rotmg-items-1.png", 8, 8, 0));
				break;
			case "entity/rotmg-bosses.png":
				GameDataManager.SPRITE_SHEETS.put("entity/rotmg-bosses.png",
						new SpriteSheet("entity/rotmg-bosses.png", 16, 16, 0));
				break;
			}

		}

		System.out.println("Sprite Sheets... DONE");

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
		return GameDataManager.SPRITE_SHEETS.get(spriteKey).getSprite(col, row, size, size);
	}

	public static Sprite getSubSprite(SpriteModel model, int size) {
		return GameDataManager.getSubSprite(model.getSpriteKey(), model.getCol(), model.getRow(), size);
	}

	public static String replaceGen(String source, String variable, String value) {
		String text = source.replace("{{" + variable + "}}", value);
		return text;
	}

	public static void loadGameData() {
		System.out.println("Loading Game Data...");
		try {
			GameDataManager.loadProjectileGroups();
			GameDataManager.loadGameItems();
			GameDataManager.loadSpriteSheets();
		}catch(Exception e) {
			System.err.println("Failed to load game data. Reason: " + e.getMessage());
		}
	}
}
