package com.jrealm.game.realm;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.jrealm.game.entity.Player;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.SocketServer;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class RealmManager implements Runnable {
	private SocketServer server;
	private Realm realm;
	private boolean shutdown = false;

	private final Map<Byte, BiConsumer<RealmManager, Packet>> packetCallbacksServer = new HashMap<>();

	private long lastUpdateTime;
	private long now;
	private long lastRenderTime;
	private long lastSecondTime;
	
	private int oldFrameCount;
	private int oldTickCount;
	private int tickCount;
	
	public RealmManager(Realm realm) {
		this.registerPacketCallbacks();
		this.realm = realm;
		this.server = new SocketServer(2222);
		WorkerThread.submitAndForkRun(this.server);
	}

	@Override
	public void run() {
		//TODO: remove and replace with an actual value: 20

		final double GAME_HERTZ = 2.0;
		final double TBU = 1000000000 / GAME_HERTZ; // Time Before Update

		final int MUBR = 3; // Must Update before render

		this.lastUpdateTime = System.nanoTime();

		//TODO: remove and replace with an actual value: 20
		final double TARGET_FPS = 2;
		final double TTBR = 1000000000 / TARGET_FPS; // Total time before render

		int frameCount = 0;
		this.lastSecondTime = (long) (this.lastUpdateTime / 1000000000);
		this.oldFrameCount = 0;

		this.tickCount = 0;
		this.oldTickCount = 0;

		while (!this.shutdown) {
			this.now = System.nanoTime();
			int updateCount = 0;
			while (((this.now - this.lastUpdateTime) > TBU) && (updateCount < MUBR)) {
				this.tick();
				this.lastUpdateTime += TBU;
				updateCount++;
				this.tickCount++;
			}

			if ((this.now - this.lastUpdateTime) > TBU) {
				this.lastUpdateTime = (long) (this.now - TBU);
			}
			
			int thisSecond = (int) (this.lastUpdateTime / 1000000000);
			if (thisSecond > this.lastSecondTime) {
				if (frameCount != this.oldFrameCount) {
					// System.out.println("NEW SECOND " + thisSecond + " " + frameCount);
					this.oldFrameCount = frameCount;
				}

				if (this.tickCount != this.oldTickCount) {
					this.oldTickCount = this.tickCount;
				}
				this.tickCount = 0;
				frameCount = 0;
				this.lastSecondTime = thisSecond;
			}

			while (((this.now - this.lastRenderTime) < TTBR) && ((this.now - this.lastUpdateTime) < TBU)) {
				try {
				} catch (Exception e) {
					System.out.println("ERROR: yielding thread");
				}
				this.now = System.nanoTime();
			}
			
		}
		log.info("RealmManager exiting run().");
	}
	
	private void tick() {
		try {
			Runnable broadcastGameData = () -> {
				this.broadcastGameData();
			};
			
			Runnable processServerPackets = () -> {
				this.processServerPackets();
			};
			
			
			WorkerThread.submitAndRun(broadcastGameData, processServerPackets);
		} catch (Exception e) {
			log.error("Failed to sleep");
		}
	}
	
	public void broadcastGameData() {
		for (Map.Entry<Long, Player> player : this.realm.getPlayers().entrySet()) {
			List<UpdatePacket> uPackets = this.realm
					.getPlayersAsPackets(player.getValue().getCam().getBounds());
			List<ObjectMovePacket> mPackets = this.realm
					.getGameObjectsAsPackets(player.getValue().getCam().getBounds());
			try {
				OutputStream toClientStream = server.getClients().get(SocketServer.LOCALHOST).getOutputStream();
				DataOutputStream dosToClient = new DataOutputStream(toClientStream);

				for (UpdatePacket packet : uPackets) {
					packet.serializeWrite(dosToClient);
				}

				for (ObjectMovePacket packet : mPackets) {
					packet.serializeWrite(dosToClient);
				}
			} catch (Exception e) {
				log.error("Failed to get OutputStream to Client");
			}
		}
	}

	public void processServerPackets() {
		while (!this.getServer().getPacketQueue().isEmpty()) {
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
					this.packetCallbacksServer.get(PacketType.TEXT.getPacketId()).accept(this, textPacket);
					break;
				case 5:
					HeartbeatPacket heartbeatPacket = new HeartbeatPacket();
					heartbeatPacket.readData(toProcess.getData());
					this.packetCallbacksServer.get(PacketType.HEARTBEAT.getPacketId()).accept(this, heartbeatPacket);
					break;
				}
			} catch (Exception e) {
				log.error("Failed to process server packets {}", e);
			}
		}
	}

	private void registerPacketCallbacks() {
		this.registerPacketCallback(PacketType.HEARTBEAT.getPacketId(), RealmManager::handleHeartbeatServer);
		this.registerPacketCallback(PacketType.TEXT.getPacketId(), RealmManager::handleTextServer);
	}

	private void registerPacketCallback(byte packetId, BiConsumer<RealmManager, Packet> callback) {
		this.packetCallbacksServer.put(packetId, callback);
	}
	
	public static void handleHeartbeatServer(RealmManager mgr, Packet packet) {
		HeartbeatPacket heartbeatPacket = (HeartbeatPacket) packet;
		log.info("[SERVER] Recieved Heartbeat Packet For Player {}@{}", heartbeatPacket.getPlayerId(), heartbeatPacket.getTimestamp());
	}
	
	public static void handleTextServer(RealmManager mgr, Packet packet) {
		TextPacket textPacket = (TextPacket) packet;
		log.info("[SERVER] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
				textPacket.getFrom(), textPacket.getMessage());
		try {
			OutputStream toClientStream = mgr.getServer().getClients().get(SocketServer.LOCALHOST).getOutputStream();
			DataOutputStream dosToClient = new DataOutputStream(toClientStream);
			TextPacket welcomeMessage = TextPacket.create("SYSTEM", textPacket.getFrom(), "Welcome to JRealm "+textPacket.getFrom()+"!");
			welcomeMessage.serializeWrite(dosToClient);
		}catch(Exception e) {
			log.error("Failed to send welcome message. Reason: {}", e);
		}
	
	}
}
