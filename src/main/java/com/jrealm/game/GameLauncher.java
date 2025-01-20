package com.jrealm.game;

import java.awt.Dimension;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import com.jrealm.account.dto.PingResponseDto;
import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.account.service.JrealmClientDataService;
import com.jrealm.account.service.JrealmServerDataService;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.ClientGameLogic;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.ServerGameLogic;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameLauncher {
	public static final String GAME_VERSION = "0.3.5";
	// private static final String HELP_MSG = "Please set the player email,
	// password, server and character UUID. [java -jar ./jrealm-{version}.jar
	// {-client | -server| -embedded} {SERVER_ADDR} {PLAYER_EMAIL} {PLAYER_PASSWORD}
	// {CHARACTER_UUID}]";

	public static void main(String[] args) {
		GameLauncher.log.info("Starting JRealm...");
		if (args.length < 2) {
			GameLauncher.log
					.error("Run option {-client | -server| -embedded} and {SERVER_ADDR} are required arguments");
			System.exit(-1);
		}

		if (GameLauncher.argsContains(args, "-server")) {
			ServerGameLogic.DATA_SERVICE = new JrealmServerDataService(HttpClient.newHttpClient(),
					"http://127.0.0.1:8085/", null);
			ClientGameLogic.DATA_SERVICE = new JrealmClientDataService(HttpClient.newHttpClient(),
					"http://" + args[1] + ":8085/", null);
			GameLauncher.pingServer();
			GameDataManager.loadGameData(true);
			GameLauncher.startServer();
		} else if (GameLauncher.argsContains(args, "-client")) {
			ServerGameLogic.DATA_SERVICE = new JrealmServerDataService(HttpClient.newHttpClient(),
					"http://127.0.0.1:8085/", null);
			ClientGameLogic.DATA_SERVICE = new JrealmClientDataService(HttpClient.newHttpClient(),
					"http://" + args[1] + ":8085/", null);
			GameLauncher.pingClient();
			GameDataManager.loadGameData(true);
			GameLauncher.startClient(args);
		} else if (GameLauncher.argsContains(args, "-embedded")) {
			ServerGameLogic.DATA_SERVICE = new JrealmServerDataService(HttpClient.newHttpClient(),
					"http://127.0.0.1:8085/", null);
			ClientGameLogic.DATA_SERVICE = new JrealmClientDataService(HttpClient.newHttpClient(),
					"http://" + args[1] + ":8085/", null);
			GameDataManager.loadGameData(true);
			GameLauncher.pingClient();
			GameLauncher.startServer();
			GameLauncher.startClient(args);
		}
	}

	private static void pingServer() {
		try {
			PingResponseDto dataServerOnline = ServerGameLogic.DATA_SERVICE.executeGet("ping", null,
					PingResponseDto.class);
			GameLauncher.log.info("Data server online. Response: {}", dataServerOnline);
		} catch (Exception e) {
			GameLauncher.log.error("FATAL. Unable to reach data server at {}. Reason: {}",
					ServerGameLogic.DATA_SERVICE.getBaseUrl(), e.getMessage());
			System.exit(-1);
		}
	}

	private static void pingClient() {
		try {
			PingResponseDto dataServerOnline = ClientGameLogic.DATA_SERVICE.executeGet("ping", null,
					PingResponseDto.class);
			GameLauncher.log.info("Data server online. Response: {}", dataServerOnline);
		} catch (Exception e) {
			GameLauncher.log.error("FATAL. Unable to reach data server at {}. Reason: {}",
					ClientGameLogic.DATA_SERVICE.getBaseUrl(), e.getMessage());
			System.exit(-1);
		}
	}

	private static void startServer() {
		Realm realm = new Realm(true, 2);
		try {
			String sysToken = ServerGameLogic.DATA_SERVICE.executeGet("token", null);
			ServerGameLogic.DATA_SERVICE.setBearerToken(sysToken);
		} catch (Exception e) {
			log.error("Failed to get Server SYS_TOKEN. Reason: {}", e);
		}
		RealmManagerServer server = new RealmManagerServer();
		Runtime.getRuntime().addShutdownHook(server.shutdownHook());

		server.addRealm(realm);
		realm.spawnRandomEnemies(realm.getMapId());
		// server.spawnTestPlayers(realm.getRealmId(), 10);
		WorkerThread.submitAndForkRun(server);
	}

	@SuppressWarnings("unchecked")
	private static void startClient(String[] args) {
		final LoginScreenPanel loginPanel = new LoginScreenPanel(820, 320);
		final JFrame frame = loginPanel.getLoginFrame();
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);

		while (!loginPanel.isSubmitted()) {
			try {
				Thread.sleep(50);
			} catch (Exception e) {
			}
		}
		try {
			final Map<String, String> loginRequest = new HashMap<>();
			loginRequest.put("email", loginPanel.getUsernameText().getText());
			loginRequest.put("password", new String(loginPanel.getPasswordText().getPassword()));

			final Map<String, Object> response = ClientGameLogic.DATA_SERVICE.executePost("admin/account/login",
					loginRequest, Map.class);
			ClientGameLogic.DATA_SERVICE.setSessionToken(response.get("token").toString());
			final PlayerAccountDto account = ClientGameLogic.DATA_SERVICE.executeGet(
					"/data/account/" + response.get("accountGuid").toString(), null, PlayerAccountDto.class);
			loginPanel.setCharacters(account);
			final Dimension currSize = frame.getSize();
			currSize.setSize(currSize.getWidth() + 20, currSize.getHeight() + 20);
			frame.setSize(currSize);
			while (loginPanel.getChars().getSelectedItem().toString().equals("-- Select Character --")) {
				try {
					Thread.sleep(50);
				} catch (Exception e) {
				}
			}
			final String selected = loginPanel.getChars().getSelectedItem().toString();
			int idx = selected.indexOf("[");
			final String charUuid = selected.substring(idx + 1, selected.lastIndexOf("]"));
			SocketClient.CHARACTER_UUID = charUuid;
			frame.dispose();
			log.info("[CLIENT] Chose characterUuid={}, disposing login frame", charUuid);
		} catch (Exception e) {
			log.error("[CLIENT] Failed to perform login and account fetch. Reason: {}", e.getMessage());
		}
		log.info("[CLIENT] Starting game client...");
		new Window();
	}

	private static boolean argsContains(final String[] args, String arg) {
		for (String s : args) {
			if (s.equals(arg))
				return true;
		}
		return false;
	}
}
