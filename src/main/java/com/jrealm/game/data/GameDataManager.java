package com.jrealm.game.data;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.model.ProjectileGroup;

public class GameDataManager {
	private static final transient ObjectMapper mapper = new ObjectMapper();

	public static SpriteSheet PLAYER_SPRITESHEET = null;

	public static Map<Integer, ProjectileGroup> PROJECTILE_GROUPS = null;

	private static void loadProjectileGroups() throws Exception {
		System.out.println("Loading Projectile Groups...");

		GameDataManager.PROJECTILE_GROUPS = new HashMap<>();
		InputStream inputStream = GameDataManager.class.getClassLoader()
				.getResourceAsStream("data/projectile-groups.json");
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

		ProjectileGroup[] projectileGroups = GameDataManager.mapper.readValue(text, ProjectileGroup[].class);

		for (ProjectileGroup group : projectileGroups) {
			GameDataManager.PROJECTILE_GROUPS.put(group.getProjectileGroupId(), group);
		}
		System.out.println("Projectile Groups... DONE");

	}

	public static void loadGameData() {
		System.out.println("Loading Game Data...");
		try {
			GameDataManager.loadProjectileGroups();
		}catch(Exception e) {
			System.err.println("Failed to load game data. Reason: " + e.getMessage());
		}
	}

}
