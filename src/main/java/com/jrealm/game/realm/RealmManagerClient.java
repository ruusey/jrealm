package com.jrealm.game.realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.jrealm.game.math.Vector2f;
import com.jrealm.game.states.PlayState;
import com.jrealm.game.util.TimedWorkerThread;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.EntityType;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class RealmManagerClient implements Runnable {
	private SocketClient client;
	private PlayState state;
	private Realm realm;
	private boolean shutdown = false;

	private final Map<Byte, BiConsumer<RealmManagerClient, Packet>> packetCallbacksClient = new HashMap<>();
	private List<Vector2f> shotDestQueue;
	private long lastUpdateTime;
	private long now;
	private long lastRenderTime;
	private long lastSecondTime;
	
	private int oldFrameCount;
	private int oldTickCount;
	private int tickCount;
	
	private long currentPlayerId;

	public RealmManagerClient(PlayState state, Realm realm) {
		this.registerPacketCallbacks();
		this.realm = realm;
		this.client = new SocketClient(2222);
		this.state = state;
		this.shotDestQueue = new ArrayList<>();
		WorkerThread.submitAndForkRun(this.client);
		this.startHeartbeatThread();
	}

	@Override
	public void run() {
		//TODO: remove and replace with an actual value: 20
		log.info("Starting JRealm Client");

		Runnable tick = ()->{
			this.tick();
			this.update(0);
		};
		
		TimedWorkerThread workerThread = new TimedWorkerThread(tick, 64);
		WorkerThread.submitAndForkRun(workerThread);
		
		log.info("RealmManager exiting run().");
	}
	
	private void tick() {
		try {
			
			Runnable processServerPackets = () -> {
				this.processClientPackets();
			};
			
			
			WorkerThread.submitAndRun(processServerPackets);
		} catch (Exception e) {
			log.error("Failed to sleep");
		}
	}
	

	public void processClientPackets() {
		while (!this.getClient().getPacketQueue().isEmpty()) {
			Packet toProcess = this.getClient().getPacketQueue().remove();
			try {
				Packet created = Packet.newInstance(toProcess.getId(), toProcess.getData());
				this.packetCallbacksClient.get(created.getId()).accept(this, created);
			} catch (Exception e) {
				log.error("Failed to process server packets {}", e);
			}
		}
	}

	private void registerPacketCallbacks() {
		this.registerPacketCallback(PacketType.UPDATE.getPacketId(), RealmManagerClient::handleUpdateClient);
		this.registerPacketCallback(PacketType.OBJECT_MOVE.getPacketId(), RealmManagerClient::handleObjectMoveClient);
		this.registerPacketCallback(PacketType.TEXT.getPacketId(), RealmManagerClient::handleTextClient);
	}

	private void registerPacketCallback(byte packetId, BiConsumer<RealmManagerClient, Packet> callback) {
		this.packetCallbacksClient.put(packetId, callback);
	}
	
	public void update(double time) {
		this.state.update(time);	
	}

	public void startHeartbeatThread() {
		Runnable sendHeartbeat = () -> {
			while (!this.shutdown) {
				try {
					if(this.currentPlayerId <= 0) {
						Thread.sleep(1000);
						continue;
					}

					long currentTime = System.currentTimeMillis();
					long playerId = this.currentPlayerId;

					HeartbeatPacket pack = HeartbeatPacket.from(playerId, currentTime);
					this.client.sendRemote(pack);
					Thread.sleep(500);
				} catch (Exception e) {
					log.error("Failed to send Heartbeat packet. Reason: {}", e);
				}
			}
		};
		WorkerThread.submitAndForkRun(sendHeartbeat);
	}
	
	public static void handleTextClient(RealmManagerClient cli, Packet packet) {
		TextPacket textPacket = (TextPacket) packet;
		log.info("[CLIENT] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
				textPacket.getFrom(), textPacket.getMessage());
	}

	public static void handleObjectMoveClient(RealmManagerClient cli, Packet packet) {
		ObjectMovePacket objectMovePacket = (ObjectMovePacket) packet;
		log.info("[CLIENT] Recieved ObjectMove Packet for Game Object {} ID {}",
				EntityType.valueOf(objectMovePacket.getEntityType()), objectMovePacket.getEntityId());
	}

	public static void handleUpdateClient(RealmManagerClient cli, Packet packet) {
		UpdatePacket updatePacket = (UpdatePacket) packet;
		log.info("[CLIENT] Recieved PlayerUpdate Packet for Player ID {}", updatePacket.getPlayerId());
	}
}
