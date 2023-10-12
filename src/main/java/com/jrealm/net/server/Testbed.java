package com.jrealm.net.server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Testbed {
	private static final Camera cam = new Camera(new AABB(new Vector2f(-64, -64), GamePanel.width + 128, GamePanel.height + 128));
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
		long playerId=-1l;
		try {
			playerId = realm.addPlayer(player);
			OutputStream stream = socketClient.getClientSocket().getOutputStream();
			DataOutputStream dos = new DataOutputStream(stream);
			log.info("Added player {} to realm. Sending update packets", playerId);
			//while(true) {
				List<UpdatePacket> uPackets = realm.getPlayersAsPackets(cam.getBounds());
				List<ObjectMove> mPackets = realm.getGameObjectsAsPackets(cam.getBounds());
				for(UpdatePacket packet: uPackets) {
					packet.serializeWrite(dos);
				}
				
				for(ObjectMove packet : mPackets) {
					packet.serializeWrite(dos);
				}
				
				while(socketServer.packetQueue.isEmpty()) {
					Thread.sleep(5000);
				}
				
				Packet nextPacket = socketServer.packetQueue.remove();
				while(nextPacket!=null) {
					switch(nextPacket.getId()) {
					case 2:
						UpdatePacket updatePacket = new UpdatePacket();
						updatePacket.readData(nextPacket);
						log.info("Recieved Player Update packet for Player ID {}", updatePacket.getPlayerId());
						break;
					case 3:
						ObjectMove objectMovePacket = new ObjectMove();
						objectMovePacket.readData(nextPacket);
						log.info("Recieved Object Move packet for Game Object {} {}", EntityType.valueOf(objectMovePacket.getEntityType()),objectMovePacket.getEntityId());
						break;
					}
					try {
						nextPacket = socketServer.packetQueue.remove();
					}catch(Exception e) {
						break;
					}
					
				}
				
				
			//}
		}catch(Exception e) {
			Testbed.log.error("Failed ", e);
		}

	}

}
