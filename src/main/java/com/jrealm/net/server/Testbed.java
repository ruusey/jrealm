package com.jrealm.net.server;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

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
import com.jrealm.net.EntityType;
import com.jrealm.net.Packet;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Testbed {
	private static final Camera cam = new Camera(
			new AABB(new Vector2f(-64, -64), GamePanel.width + 128, GamePanel.height + 128));

	
	private static final Map<Integer, Consumer<Packet>> packetCallbacksClient = new HashMap<>();
	private static final Map<Integer, Consumer<Packet>> packetCallbacksServer = new HashMap<>();

	
	public static void main(String[] args) {
		
		packetCallbacksClient.put(2, Testbed::handleUpdateClient);
		packetCallbacksClient.put(3, Testbed::handleObjectMoveClient);
		packetCallbacksClient.put(4, Testbed::handleTextClient);

		packetCallbacksServer.put(4, Testbed::handleTextServer);
		packetCallbacksServer.put(5, Testbed::handleHeartbeatServer);

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
		realmManager.start();
		SocketClient socketClient = new SocketClient(2222);
		socketClient.start();
		
	
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
			socketClient.startHeartbeatThread();
			while (realmManager.getServer().getPacketQueue().isEmpty()) {
				Thread.sleep(100);
			}
			
		} catch (Exception e) {
			Testbed.log.error("Networking failure ", e);
		}
	}
	
	public static void handleTextServer(Packet packet) {
		TextPacket textPacket = (TextPacket) packet;
		log.info("[SERVER] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
				textPacket.getFrom(), textPacket.getMessage());
	}
	
	public static void handleTextClient(Packet packet) {
		TextPacket textPacket = (TextPacket) packet;
		log.info("[CLIENT] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
				textPacket.getFrom(), textPacket.getMessage());
	}
	
	public static void handleHeartbeatServer(Packet packet) {
		HeartbeatPacket heartbeatPacket = (HeartbeatPacket) packet;
		log.info("[SERVER] Recieved Heartbeat Packet For Player {}@{}", heartbeatPacket.getPlayerId(), heartbeatPacket.getTimestamp());
	}
	
	public static void handleObjectMoveClient(Packet packet) {
		ObjectMovePacket objectMovePacket = (ObjectMovePacket) packet;
		log.info("[CLIENT] Recieved ObjectMove Packet for Game Object {} ID {}",
				EntityType.valueOf(objectMovePacket.getEntityType()), objectMovePacket.getEntityId());	
	}
	
	public static void handleUpdateClient(Packet packet) {
		UpdatePacket updatePacket = (UpdatePacket) packet;
		log.info("[CLIENT] Recieved PlayerUpdate Packet for Player ID {}", updatePacket.getPlayerId());
	}
}
