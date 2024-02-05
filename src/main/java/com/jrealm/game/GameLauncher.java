package com.jrealm.game;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.server.SocketServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameLauncher {
	public static final String GAME_VERSION = "0.3.0";
	public static final boolean LOCAL_SERVER = false;
	public static final boolean LOCAL_CLIENT = true;
	public GameLauncher() {
		new Window();
	}

	public static void main(String[] args) {
		GameLauncher.log.info("Starting JRealm...");
		GameDataManager.loadGameData();
		if(GameLauncher.LOCAL_SERVER) {
			Realm realm = new Realm(true, 4);
			RealmManagerServer server = new RealmManagerServer();
			server.addRealm(realm);
			realm.spawnRandomEnemies(realm.getMapId());
			//server.spawnTestPlayers(realm.getRealmId(), 2);
			WorkerThread.submitAndForkRun(server);
		}
		if (GameLauncher.LOCAL_CLIENT) {
			if(args.length<2) {
				GameLauncher.log.error(
						"Please set the player username server and classId. [java -jar ./jrealm-client.jar {SERVER_ADDR} {PLAYER_EMAIL} {CLASS_UUID}]");
				return;
			}
			SocketClient.PLAYER_EMAIL = args[1];
			if(SocketClient.PLAYER_EMAIL==null) {
				GameLauncher.log.error(
						"Please set the player username server and classId. [java -jar ./jrealm-client.jar {SERVER_ADDR} {PLAYER_EMAIL} {CLASS_UUID}]");
				return;
			}
			SocketClient.SERVER_ADDR = args[0];
			if(SocketClient.SERVER_ADDR==null) {
				SocketClient.SERVER_ADDR=SocketServer.LOCALHOST;
			}
			try {
				SocketClient.CHARACTER_UUID = args[2];
			} catch (Exception e) {
				GameLauncher.log.error(
						"Please set the player username server and classId. [java -jar ./jrealm-client.jar {SERVER_ADDR} {PLAYER_EMAIL} {CLASS_UUID}]");
			}
			new GameLauncher();
		}
	}
}
