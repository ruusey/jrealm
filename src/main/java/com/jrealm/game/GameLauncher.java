package com.jrealm.game;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.server.SocketServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameLauncher {
	public static final String GAME_VERSION = "0.2.5";
	public static final boolean LOCAL_SERVER = false;
	public static final boolean LOCAL_CLIENT = true;
	public GameLauncher() {
		new Window();
	}

	public static void main(String[] args) {

		GameLauncher.log.info("Starting JRealm...");
		GameDataManager.loadGameData();
		if(GameLauncher.LOCAL_SERVER) {
			Camera c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
			Realm realm = new Realm(c, true);
			RealmManagerServer server = new RealmManagerServer(realm);
			WorkerThread.submitAndForkRun(server);
		}
		if (GameLauncher.LOCAL_CLIENT) {
			if(args.length<2) {
				GameLauncher.log.error(
						"Please set the player username server and classId. [java -jar ./jrealm-client.jar {SERVER_ADDR} {PLAYER_NAME} {CLASS_ID}]");
				return;
			}
			SocketClient.PLAYER_USERNAME = args[1];
			if(SocketClient.PLAYER_USERNAME==null) {
				GameLauncher.log.error(
						"Please set the player username server and classId. [java -jar ./jrealm-client.jar {SERVER_ADDR} {PLAYER_NAME} {CLASS_ID}]");
				return;
			}
			SocketClient.SERVER_ADDR = args[0];
			if(SocketClient.SERVER_ADDR==null) {
				SocketClient.SERVER_ADDR=SocketServer.LOCALHOST;
			}
			try {
				SocketClient.CLASS_ID = Integer.parseInt(args[2]);
			} catch (Exception e) {
				GameLauncher.log.error(
						"Please set the player username server and classId. [java -jar ./jrealm-client.jar {SERVER_ADDR} {PLAYER_NAME} {CLASS_ID}]");
			}
			new GameLauncher();
		}
	}
}
