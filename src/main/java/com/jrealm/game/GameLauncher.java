package com.jrealm.game;

import java.net.http.HttpClient;

import javax.swing.JOptionPane;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jrealm.account.dto.PingResponseDto;
import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.account.service.JrealmClientDataService;
import com.jrealm.account.service.JrealmServerDataService;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.net.client.ClientGameLogic;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.ServerGameLogic;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameLauncher {
    public static final String GAME_VERSION = "0.3.8";
    public static final Boolean DEBUG_MODE = true;

    public static void main(String[] args) {
        GameLauncher.log.info("Starting JRealm...");
        if (args.length == 0) {
            log.info("NO ARGS PROVIDED. Running as local embedded server. Assuming local data service is running at 127.0.0.1");
            args = new String[] { "-embedded", "127.0.0.1" };
        }

        if (args.length < 2) {
            GameLauncher.log.error("Run option {-client | -server| -embedded} and {SERVER_ADDR} are required arguments");
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
        try {
            final String sysToken = ServerGameLogic.DATA_SERVICE.executeGet("token", null);
            ServerGameLogic.DATA_SERVICE.setBearerToken(sysToken);
        } catch (Exception e) {
            log.error("Failed to get Server SYS_TOKEN. Reason: {}", e);
        }
        final RealmManagerServer server = new RealmManagerServer();
        server.doRunServer();
    }

    private static void startClient(String[] args) {
        boolean skipLogin = false;
        if (args.length > 2) {
            SocketClient.SERVER_ADDR = args[1];
            SocketClient.PLAYER_EMAIL = args[2];
            SocketClient.PLAYER_PASSWORD = args[3];
            SocketClient.CHARACTER_UUID = args[4];
            skipLogin = true;
        }

        // Simple Swing login dialog before LibGDX takes over the main thread
        if (!skipLogin) {
            try {
                String email = JOptionPane.showInputDialog(null, "Email:", "JRealm Login", JOptionPane.PLAIN_MESSAGE);
                if (email == null || email.isBlank()) {
                    log.error("[CLIENT] Login cancelled");
                    System.exit(0);
                }
                String password = JOptionPane.showInputDialog(null, "Password:", "JRealm Login", JOptionPane.PLAIN_MESSAGE);
                if (password == null || password.isBlank()) {
                    log.error("[CLIENT] Login cancelled");
                    System.exit(0);
                }
                SocketClient.PLAYER_EMAIL = email;
                SocketClient.PLAYER_PASSWORD = password;

                final ObjectNode loginRequest = new ObjectNode(JsonNodeFactory.instance);
                loginRequest.put("email", email);
                loginRequest.put("password", password);

                final ObjectNode response = ClientGameLogic.DATA_SERVICE.executePost("admin/account/login",
                        loginRequest, ObjectNode.class);
                ClientGameLogic.DATA_SERVICE.setSessionToken(response.get("token").asText());
                final PlayerAccountDto account = ClientGameLogic.DATA_SERVICE.executeGet(
                        "/data/account/" + response.get("accountGuid").asText(), null, PlayerAccountDto.class);

                // Build character selection list
                String[] charOptions = account.getCharacters().stream()
                        .map(c -> c.getCharacterClass() + " [" + c.getCharacterUuid() + "]")
                        .toArray(String[]::new);

                if (charOptions.length == 0) {
                    JOptionPane.showMessageDialog(null, "No characters found on this account.");
                    System.exit(0);
                }

                String selected = (String) JOptionPane.showInputDialog(null, "Select Character:", "JRealm",
                        JOptionPane.PLAIN_MESSAGE, null, charOptions, charOptions[0]);
                if (selected == null) {
                    System.exit(0);
                }
                int idx = selected.indexOf("[");
                SocketClient.CHARACTER_UUID = selected.substring(idx + 1, selected.lastIndexOf("]"));
                log.info("[CLIENT] Chose characterUuid={}", SocketClient.CHARACTER_UUID);
            } catch (Exception e) {
                log.error("[CLIENT] Failed to perform login. Reason: {}", e.getMessage());
                JOptionPane.showMessageDialog(null, e.getMessage());
                System.exit(-1);
            }
        }

        log.info("[CLIENT] Starting LibGDX game client...");

        // Launch LibGDX application
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("JRealm " + GAME_VERSION);
        config.setWindowedMode(1920, 1080);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(144);

        new Lwjgl3Application(new JRealmGame(), config);
    }

    private static boolean argsContains(final String[] args, String arg) {
        for (String s : args) {
            if (s.equals(arg))
                return true;
        }
        return false;
    }
}
