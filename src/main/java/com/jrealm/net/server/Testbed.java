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
		Player player = new Player(CharacterClass.ARCHER.classId, cam, GameDataManager.loadClassSprites(clazz),
				new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
						(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
				GlobalConstants.PLAYER_SIZE);
		player.equipSlots(PlayState.getStartingEquipment(clazz));

		SocketServer socketServer = new SocketServer(2222);
		socketServer.start();
		SocketClient socketClient = new SocketClient(2222);

		Realm realm = new Realm(cam);
		realm.spawnRandomEnemies();
		long playerId = -1l;
		try {
			playerId = realm.addPlayer(player);
			OutputStream stream = socketClient.getClientSocket().getOutputStream();
			DataOutputStream dos = new DataOutputStream(stream);
			log.info("Added player {} to realm. Sending update packets", playerId);
			List<UpdatePacket> uPackets = realm.getPlayersAsPackets(cam.getBounds());
			List<ObjectMove> mPackets = realm.getGameObjectsAsPackets(cam.getBounds());

			TextPacket welcomeMessage = TextPacket.create("SYSTEM", "Ruusey", "Welcome to JRealm!");

			welcomeMessage.serializeWrite(dos);

			for (UpdatePacket packet : uPackets) {
				packet.serializeWrite(dos);
			}

			for (ObjectMove packet : mPackets) {
				packet.serializeWrite(dos);
			}

			while (socketServer.packetQueue.isEmpty()) {
				Thread.sleep(1000);
			}

			Packet nextPacket = socketServer.packetQueue.remove();
			while (nextPacket != null) {
				switch (nextPacket.getId()) {
				case 2:
					UpdatePacket updatePacket = new UpdatePacket();
					updatePacket.readData(nextPacket.getData());
					log.info("Recieved PlayerUpdate Packet for Player ID {}", updatePacket.getPlayerId());
					break;
				case 3:
					ObjectMove objectMovePacket = new ObjectMove();
					objectMovePacket.readData(nextPacket.getData());
					log.info("Recieved ObjectMove Packet for Game Object {} {}",
							EntityType.valueOf(objectMovePacket.getEntityType()), objectMovePacket.getEntityId());
					break;
				case 4:
					TextPacket textPacket = new TextPacket();
					textPacket.readData(nextPacket.getData());
					log.info("Recieved Text Packet \n\tTO: {}\n\tFROM: {}\n\tMESSAGE: {}", textPacket.getTo(),
							textPacket.getFrom(), textPacket.getMessage());
					break;
				default:
					log.error("Unknown packet with ID {} recieved, Discarding", nextPacket.getId());
				}
				try {
					nextPacket = socketServer.packetQueue.remove();
				} catch (Exception e) {
					log.warn("No more packets to recieve. Exiting...");
					break;
				}
			}
		} catch (Exception e) {
			Testbed.log.error("Failed ", e);
		}
	}
}
