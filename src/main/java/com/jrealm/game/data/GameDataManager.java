package com.jrealm.game.data;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;

public class GameDataManager {
	private static final transient ObjectMapper mapper = new ObjectMapper();

	public static SpriteSheet PLAYER_SPRITESHEET = null;

	public static Map<Integer, ProjectileGroup> PROJECTILE_GROUPS = null;
	public static Map<String, SpriteSheet> SPRITE_SHEETS = null;

	private static final String[] SPRITE_SHEET_LOCATIONS = { "material/trees.png", "tile/overworldOP.png",
			"entity/rotmg-classes.png", "entity/rotmg-projectiles.png",
	"entity/rotmg-bosses.png" };
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
				}
			}
			GameDataManager.PROJECTILE_GROUPS.put(group.getProjectileGroupId(), group);
		}
		System.out.println("Projectile Groups... DONE");

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

	public static String replaceGen(String source, String variable, String value) {
		String text = source.replace("{{" + variable + "}}", value);
		return text;
	}

	public static void loadGameData() {
		System.out.println("Loading Game Data...");
		try {
			GameDataManager.loadProjectileGroups();
			GameDataManager.loadSpriteSheets();
		}catch(Exception e) {
			System.err.println("Failed to load game data. Reason: " + e.getMessage());
		}
	}
}
