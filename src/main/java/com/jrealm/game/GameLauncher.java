package com.jrealm.game;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.WorkerThread;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameLauncher {
	public GameLauncher() {
		new Window();
	}

	public static void main(String[] args) {
		
		GameLauncher.log.info("Starting JRealm...");
		GameDataManager.loadGameData();

		Camera c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
		Realm realm = new Realm(c, true);
		RealmManagerServer server = new RealmManagerServer(realm);
		WorkerThread.submitAndForkRun(server);
		new GameLauncher();
	}
}
