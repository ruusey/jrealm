package com.jrealm.net.server;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.List;

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
import com.jrealm.net.client.packet.ObjectMove;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Testbed {
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
			OutputStream toServerStream = socketClient.getClientSocket().getOutputStream();
			DataOutputStream dosToServer = new DataOutputStream(toServerStream);
			TextPacket welcomeMessage = TextPacket.create("CLIENT", "Ruusey", "Login");
			welcomeMessage.serializeWrite(dosToServer);
//			OutputStream toServerStream = socketClient.getClientSocket().getOutputStream();
//			DataOutputStream dosToServer = new DataOutputStream(toServerStream);
//			
//			OutputStream toClientStream = realmManager.getServer().getClients().get(SocketServer.LOCALHOST).getOutputStream();
//			DataOutputStream dosToClient = new DataOutputStream(toClientStream);
//			
//			log.info("Added player {} to realm. Sending update packets", playerId);
//			List<UpdatePacket> uPackets = realm.getPlayersAsPackets(cam.getBounds());
//			List<ObjectMove> mPackets = realm.getGameObjectsAsPackets(cam.getBounds());
//
//			TextPacket welcomeMessage = TextPacket.create("SYSTEM", "Ruusey", "Welcome to JRealm!");
//			welcomeMessage.serializeWrite(dosToServer);
//
//			for (UpdatePacket packet : uPackets) {
//				packet.serializeWrite(dosToServer);
//			}
//
//			for (ObjectMove packet : mPackets) {
//				packet.serializeWrite(dosToClient);
//			}

			while (realmManager.getServer().getPacketQueue().isEmpty()) {
				Thread.sleep(100);
			}

			Packet nextPacket = realmManager.getServer().getPacketQueue().remove();
			while (nextPacket != null) {
				switch (nextPacket.getId()) {
				case 2:
					UpdatePacket updatePacket = new UpdatePacket();
					updatePacket.readData(nextPacket.getData());
					log.info("[SERVER] Recieved PlayerUpdate Packet for Player ID {}", updatePacket.getPlayerId());
					break;
				case 3:
					ObjectMove objectMovePacket = new ObjectMove();
					objectMovePacket.readData(nextPacket.getData());
					log.info("[SERVER] Recieved ObjectMove Packet for Game Object {} ID {}",
							EntityType.valueOf(objectMovePacket.getEntityType()), objectMovePacket.getEntityId());
					break;
				case 4:
					TextPacket textPacket = new TextPacket();
					textPacket.readData(nextPacket.getData());
					log.info("[SERVER] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
							textPacket.getFrom(), textPacket.getMessage());
					break;
				default:
					log.error("[SERVER] Unknown packet with ID {} recieved, Discarding", nextPacket.getId());
				}
				try {
					nextPacket = realmManager.getServer().getPacketQueue().remove();
				} catch (Exception e) {
					log.warn("No more SERVER packets to process. Exiting...");
					break;
				}
			}
			Thread.sleep(1000);
			while (socketClient.getPacketQueue().isEmpty()) {
				Thread.sleep(1000);
			}
			
			nextPacket = socketClient.getPacketQueue().remove();
			while (nextPacket != null) {
				switch (nextPacket.getId()) {
				case 2:
					UpdatePacket updatePacket = new UpdatePacket();
					updatePacket.readData(nextPacket.getData());
					log.info("[CLIENT] Recieved PlayerUpdate Packet for Player ID {}", updatePacket.getPlayerId());
					break;
				case 3:
					ObjectMove objectMovePacket = new ObjectMove();
					objectMovePacket.readData(nextPacket.getData());
					log.info("[CLIENT] Recieved ObjectMove Packet for Game Object {} ID {}",
							EntityType.valueOf(objectMovePacket.getEntityType()), objectMovePacket.getEntityId());
					break;
				case 4:
					TextPacket textPacket = new TextPacket();
					textPacket.readData(nextPacket.getData());
					log.info("[CLIENT] Recieved Text Packet \n\tTO: {}\n\tFROM: {}\n\tMESSAGE: {}", textPacket.getTo(),
							textPacket.getFrom(), textPacket.getMessage());
					break;
				default:
					log.error("Unknown packet with ID {} recieved, Discarding", nextPacket.getId());
				}
				try {
					nextPacket = socketClient.getPacketQueue().remove();
				} catch (Exception e) {
					log.warn("No more CLIENT packets to process. Exiting...");
					break;
				}
			}
		} catch (Exception e) {
			Testbed.log.error("Networking failure ", e);
		}
	}
}
