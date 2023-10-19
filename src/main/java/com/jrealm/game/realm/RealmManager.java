package com.jrealm.game.realm;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.jrealm.game.entity.Player;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.SocketServer;
import com.jrealm.net.server.Testbed;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper=false)
public class RealmManager extends Thread{
	private SocketServer server;
	private Realm realm;
	private boolean shutdown = false;
	
	private static final Map<Integer, Consumer<Packet>> packetCallbacksServer = new HashMap<>();
	
	public RealmManager(Realm realm) {
		this.realm = realm;
		this.server = new SocketServer(2222);
		this.server.start();
		
		packetCallbacksServer.put(4, Testbed::handleTextServer);
		packetCallbacksServer.put(5, Testbed::handleHeartbeatServer);
	}
	
	@Override
	public void run() {
		while(!shutdown) {
			try {
				for(Map.Entry<Long, Player> player : this.realm.getPlayers().entrySet()) {
					List<UpdatePacket> uPackets = this.realm.getPlayersAsPackets(player.getValue().getCam().getBounds());
					List<ObjectMovePacket> mPackets = this.realm.getGameObjectsAsPackets(player.getValue().getCam().getBounds());
					try {
						OutputStream toClientStream = server.getClients().get(SocketServer.LOCALHOST).getOutputStream();
						DataOutputStream dosToClient = new DataOutputStream(toClientStream);
						
						for (UpdatePacket packet : uPackets) {
							packet.serializeWrite(dosToClient);
						}

						for (ObjectMovePacket packet : mPackets) {
							packet.serializeWrite(dosToClient);
						}
					}catch(Exception e) {
						log.error("Failed to get OutputStream to Client");
					}
				}
				
				this.processServerPackets();
				Thread.sleep(1000);
			}catch(Exception e) {
				log.error("Failed to sleep");
			}
		
		}
		log.info("Realm manager exiting run().");
	}
	
	public void processServerPackets() {
		while(!this.getServer().getPacketQueue().isEmpty()) {
			Packet toProcess = this.getServer().getPacketQueue().remove();
			try {
				switch (toProcess.getId()) {
				case 2:
					UpdatePacket updatePacket = new UpdatePacket();
					updatePacket.readData(toProcess.getData());
					break;
				case 3:
					ObjectMovePacket objectMovePacket = new ObjectMovePacket();
					objectMovePacket.readData(toProcess.getData());
					break;
				case 4:
					TextPacket textPacket = new TextPacket();
					textPacket.readData(toProcess.getData());
					packetCallbacksServer.get(4).accept(textPacket);
					break;
				case 5:
					HeartbeatPacket heartbeatPacket = new HeartbeatPacket();
					heartbeatPacket.readData(toProcess.getData());
					packetCallbacksServer.get(5).accept(heartbeatPacket);
					break;
				}
			}catch(Exception e) {
				log.error("Failed to process server packets {}", e);
			}
		}
	}
}
