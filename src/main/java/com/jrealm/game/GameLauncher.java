package com.jrealm.game;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.SocketClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameLauncher {
	public static final boolean LOCAL_SERVER = true;
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
			SocketClient.PLAYER_USERNAME = args[0];
			if(SocketClient.PLAYER_USERNAME==null) {
				log.error("Please set the player username. [java -jar ./jrealm-client.jar {PLAYER_NAME}]");
			}
			new GameLauncher();
		}
	}
}
