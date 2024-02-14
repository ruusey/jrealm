package com.jrealm.game;

import com.jrealm.account.dto.PingResponseDto;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.server.ServerGameLogic;
import com.jrealm.net.server.SocketServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameLauncher {
	public static final String GAME_VERSION = "0.3.1";
	private static final String HELP_MSG = "Please set the player email, password, server and character UUID. [java -jar ./jrealm-{version}.jar {-client | -server| -embedded} {SERVER_ADDR} {PLAYER_EMAIL} {PLAYER_PASSWORD} {CHARACTER_UUID}]";

	public GameLauncher() {
		new Window();
	}

	public static void main(String[] args) {
		GameLauncher.log.info("Starting JRealm...");
		try {
			PingResponseDto dataServerOnline = ServerGameLogic.DATA_SERVICE.executeGet("ping", null,
					PingResponseDto.class);
			GameLauncher.log.info("Data server online. Response: {}", dataServerOnline);
		} catch (Exception e) {
			GameLauncher.log.error("FATAL. Unable to reach data server at {}. Reason: {}", ServerGameLogic.DATA_HOST,
					e.getMessage());
			System.exit(-1);
		}
		if (GameLauncher.argsContains(args, "-server")) {
			GameDataManager.loadGameData(false);
			GameLauncher.startServer();
		}
		else if (GameLauncher.argsContains(args, "-client")) {
			GameDataManager.loadGameData(true);
			GameLauncher.startClient(args);
		} else if (GameLauncher.argsContains(args, "-embedded")) {
			GameDataManager.loadGameData(true);
			GameLauncher.startServer();
			GameLauncher.startClient(args);
		}
	}

	private static void startServer() {
		Realm realm = new Realm(true, 4);
		RealmManagerServer server = new RealmManagerServer();
		Runtime.getRuntime().addShutdownHook(server.shutdownHook());

		server.addRealm(realm);
		realm.spawnRandomEnemies(realm.getMapId());
		// server.spawnTestPlayers(realm.getRealmId(), 10);
		WorkerThread.submitAndForkRun(server);
	}

	private static void startClient(String[] args) {
		SocketClient.SERVER_ADDR = args[1];
		if (SocketClient.SERVER_ADDR == null) {
			SocketClient.SERVER_ADDR = SocketServer.LOCALHOST;
			GameLauncher.log.error("Server address not set");
			GameLauncher.log.error(GameLauncher.HELP_MSG);
			return;
		}
		SocketClient.PLAYER_EMAIL = args[2];
		if (SocketClient.PLAYER_EMAIL == null) {
			GameLauncher.log.error("Player email not set");
			GameLauncher.log.error(GameLauncher.HELP_MSG);
			return;
		}
		try {
			SocketClient.PLAYER_PASSWORD = args[3];
		} catch (Exception e) {
			GameLauncher.log.error("Player password not set");
			GameLauncher.log.error(GameLauncher.HELP_MSG);
			return;
		}
		try {
			SocketClient.CHARACTER_UUID = args[4];
		} catch (Exception e) {
			GameLauncher.log.error("Player character UUID not set");
			GameLauncher.log.error(GameLauncher.HELP_MSG);
			return;
		}
		new GameLauncher();
	}

	private static boolean argsContains(final String[] args, String arg) {
		for (String s : args) {
			if (s.equals(arg))
				return true;
		}
		return false;
	}
}
