package com.jrealm.game;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameLauncher {
	public GameLauncher() {
		new Window();
	}

	public static void main(String[] args) {
		GameLauncher.log.info("Starting JRealm...");
		new GameLauncher();
	}
}
