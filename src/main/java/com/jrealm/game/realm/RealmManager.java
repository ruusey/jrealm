package com.jrealm.game.realm;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.jrealm.game.entity.Player;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.client.packet.ObjectMove;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.SocketServer;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class RealmManager extends Thread{
	private SocketServer server;
	private Realm realm;
	private boolean shutdown = false;
	
	public RealmManager(Realm realm) {
		this.realm = realm;
		this.server = new SocketServer(2222);
		this.server.start();
	}
	
	@Override
	public void run() {
		while(!shutdown) {
			try {
				for(Map.Entry<Long, Player> player : this.realm.getPlayers().entrySet()) {
					List<UpdatePacket> uPackets = this.realm.getPlayersAsPackets(player.getValue().getCam().getBounds());
					List<ObjectMove> mPackets = this.realm.getGameObjectsAsPackets(player.getValue().getCam().getBounds());
					try {
						OutputStream toClientStream = server.getClients().get(SocketServer.LOCALHOST).getOutputStream();
						DataOutputStream dosToClient = new DataOutputStream(toClientStream);
						
						for (UpdatePacket packet : uPackets) {
							packet.serializeWrite(dosToClient);
						}

						for (ObjectMove packet : mPackets) {
							packet.serializeWrite(dosToClient);
						}
					}catch(Exception e) {
						log.error("Failed to get OutputStream to Client");
					}
				}
				Thread.sleep(100);
			}catch(Exception e) {
				log.error("Failed to sleep");
			}
		
		}
		log.info("Realm manager exiting run().");
	}
	
	
	
}
