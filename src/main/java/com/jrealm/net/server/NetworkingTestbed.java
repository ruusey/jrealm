package com.jrealm.net.server;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManager;
import com.jrealm.game.states.PlayState;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.SocketClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetworkingTestbed {
	private static final Camera cam = new Camera(
			new AABB(new Vector2f(-64, -64), GamePanel.width + 128, GamePanel.height + 128));

	public static void main(String[] args) {
		GameDataManager.loadGameData();
		CharacterClass clazz = CharacterClass.ARCHER;
		Player player = new Player(Realm.RANDOM.nextLong(), cam, GameDataManager.loadClassSprites(clazz),
				new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
						(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
				GlobalConstants.PLAYER_SIZE, clazz);
		player.equipSlots(PlayState.getStartingEquipment(clazz));
		Realm realm = new Realm(cam);
		realm.spawnRandomEnemies();

		RealmManager realmManager = new RealmManager(realm);
		SocketClient socketClient = new SocketClient(2222);
		WorkerThread.submitAndForkRun(realmManager, socketClient);
		
		while(realmManager.getServer().getClients().size()==0) {
			try {
				Thread.sleep(100);
				log.info("Waiting for client connections in Server Manager");
			}catch(Exception e) {
				
			}
		}
		long playerId = -1l;
		try {
			playerId = realm.addPlayer(player);
			socketClient.setCurrentPlayerId(playerId);
			while (realmManager.getServer().getPacketQueue().isEmpty()) {
				Thread.sleep(100);
			}
			
		} catch (Exception e) {
			NetworkingTestbed.log.error("Networking failure ", e);
		}
	}
}
