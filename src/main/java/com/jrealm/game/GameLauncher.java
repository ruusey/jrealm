package com.jrealm.game;

import java.awt.Dimension;
import java.net.http.HttpClient;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
	public static final String GAME_VERSION = "0.3.7";
	public static final Boolean DEBUG_MODE = true;
	// private static final String HELP_MSG = "Please set the player email,
	// password, server and character UUID. [java -jar ./jrealm-{version}.jar
	// {-client | -server| -embedded} {SERVER_ADDR} {PLAYER_EMAIL} {PLAYER_PASSWORD}
	// {CHARACTER_UUID}]";

	public static void main(String[] args) {
		GameLauncher.log.info("Starting JRealm...");
		if(args.length==0) {
			log.info("NO ARGS PROVIDED. Running as local embedded server. Assuming local data service is running at 127.0.0.1");
			args = new String[] {"-embedded", "127.0.0.1"};
		}

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
			PingResponseDto  dataServerOnline = ClientGameLogic.DATA_SERVICE.executeGet("ping", null,
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

	private static void startClient(String[] args) {
		boolean skipLogin=false;
		if(args.length>2) {
			SocketClient.SERVER_ADDR = args[1];
			SocketClient.PLAYER_EMAIL = args[2];
			SocketClient.PLAYER_PASSWORD = args[3];
			SocketClient.CHARACTER_UUID = args[4];
			skipLogin=true;
				
		}
		final LoginScreenPanel loginPanel = new LoginScreenPanel(1020, 320);
		try {
			final JFrame frame = loginPanel.getLoginFrame();
			frame.setVisible(true);
			frame.setLocationRelativeTo(null);
			frame.setResizable(true);
			while(true) {
				while (!loginPanel.isSubmitted() && !skipLogin) {
					try {
						Thread.sleep(50);
					} catch (Exception e) {
					}
				}
				final ObjectNode loginRequest = new ObjectNode(JsonNodeFactory.instance);
				loginRequest.put("email",  SocketClient.PLAYER_EMAIL);
				loginRequest.put("password",  SocketClient.PLAYER_PASSWORD);

				final ObjectNode response = ClientGameLogic.DATA_SERVICE.executePost("admin/account/login",
						loginRequest, ObjectNode.class);
				ClientGameLogic.DATA_SERVICE.setSessionToken(response.get("token").asText());
				final PlayerAccountDto account = ClientGameLogic.DATA_SERVICE.executeGet(
						"/data/account/" + response.get("accountGuid").asText(), null, PlayerAccountDto.class);
				loginPanel.setCharacters(account);
				final Dimension currSize = frame.getSize();
				currSize.setSize(currSize.getWidth() + 70, currSize.getHeight());
				loginPanel.setSize(currSize);
				frame.setSize(currSize);
				while (loginPanel.getChars().getSelectedItem().toString().equals("-- Select Character --") && !skipLogin) {
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
				break;
			}
			
		} catch (IndexOutOfBoundsException e) {
			log.error("[CLIENT] Automatically logged in as user {}", SocketClient.PLAYER_EMAIL);
			JOptionPane.showMessageDialog(loginPanel.getFrame(), "Automatic login as "+SocketClient.PLAYER_EMAIL+" successful");
			//System.exit(-1);
		}catch (Exception e) {
			log.error("[CLIENT] Failed to perform login and account fetch. Reason: {}", e.getMessage());
			JOptionPane.showMessageDialog(loginPanel.getFrame(), e.getMessage());
			//System.exit(-1);
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
