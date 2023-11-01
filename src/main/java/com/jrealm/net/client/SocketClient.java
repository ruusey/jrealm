package com.jrealm.net.client;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.BlankPacket;
import com.jrealm.net.EntityType;
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

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class SocketClient implements Runnable {

	private static final int BUFFER_CAPACITY = 65536 * 10;

	private long currentPlayerId;

	private Socket clientSocket;
	private boolean shutdown = false;
	private byte[] localBuffer = new byte[SocketClient.BUFFER_CAPACITY];
	private int localBufferIndex = 0;
	private byte[] remoteBuffer = new byte[SocketClient.BUFFER_CAPACITY];
	private int remoteBufferIndex = 0;
	private long lastUpdate = 0;
	private long previousTime = 0;
	private long localNoDataTime = System.currentTimeMillis();
	private long remoteNoDataTime = System.currentTimeMillis();

	private final Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();

	private final Map<Byte, BiConsumer<SocketClient, Packet>> packetCallbacksClient = new HashMap<>();

	public SocketClient(int port) {
		try {
			this.registerPacketCallbacks();
			this.clientSocket = new Socket(SocketServer.LOCALHOST, port);
			this.sendRemote(TextPacket.create("Ruusey", "SYSTEM", "LoginRequest"));
			this.startHeartbeatThread();
		} catch (Exception e) {
			SocketClient.log.error("Failed to create ClientSocket, Reason: {}", e.getMessage());
		}
	}

	@Override
	public void run() {
		while (!this.shutdown) {
			Runnable recievePackets = () -> {
				this.readPackets();
			};

			Runnable processPackets = () -> {
				this.processPackets();
			};

			WorkerThread.submitAndRun(recievePackets, processPackets);
		}
	}

	private void readPackets() {
		try {
			InputStream stream = this.clientSocket.getInputStream();
			int bytesRead = stream.read(this.remoteBuffer, this.remoteBufferIndex,
					this.remoteBuffer.length - this.remoteBufferIndex);
			if (bytesRead == -1)
				throw new SocketException("end of stream");
			if (bytesRead > 0) {
				this.remoteBufferIndex += bytesRead;

				while (this.remoteBufferIndex >= 5) {
					int packetLength = ((ByteBuffer) ByteBuffer.allocate(4).put(this.remoteBuffer[1])
							.put(this.remoteBuffer[2]).put(this.remoteBuffer[3]).put(this.remoteBuffer[4]).rewind())
							.getInt();
					if (this.remoteBufferIndex < (packetLength)) {
						break;
					}
					byte packetId = this.remoteBuffer[0];
					byte[] packetBytes = new byte[packetLength];
					System.arraycopy(this.remoteBuffer, 5, packetBytes, 0, packetLength);
					if (this.remoteBufferIndex > packetLength) {
						System.arraycopy(this.remoteBuffer, packetLength, this.remoteBuffer, 0,
								this.remoteBufferIndex - packetLength);
					}
					this.remoteBufferIndex -= packetLength;
					this.packetQueue.add(new BlankPacket(packetId, packetBytes));
				}
			}
		} catch (Exception e) {
			SocketClient.log.error("Failed to parse client input {}", e.getMessage());
		}
	}

	public void sendRemote(Packet packet) throws Exception {
		if (this.clientSocket == null)
			throw new Exception("Client socket is null/not yet established");
		try {
			OutputStream stream = this.clientSocket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(stream);
			packet.serializeWrite(dos);
		} catch (Exception e) {
			String remoteAddr = this.clientSocket.getInetAddress().getHostAddress();
			log.error("Failed to send Packet to remote addr {}", remoteAddr);
		}
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

					HeartbeatPacket pack = new HeartbeatPacket().from(playerId, currentTime);
					this.sendRemote(pack);
					Thread.sleep(500);
				} catch (Exception e) {
					log.error("Failed to send Heartbeat packet. Reason: {}", e);
				}
			}
		};
		WorkerThread.submitAndForkRun(sendHeartbeat);
	}

	private void registerPacketCallbacks() {
		this.registerPacketCallback(PacketType.UPDATE.getPacketId(), SocketClient::handleUpdateClient);
		this.registerPacketCallback(PacketType.OBJECT_MOVE.getPacketId(), SocketClient::handleObjectMoveClient);
		this.registerPacketCallback(PacketType.TEXT.getPacketId(), SocketClient::handleTextClient);
	}

	private void registerPacketCallback(byte packetId, BiConsumer<SocketClient, Packet> callback) {
		this.packetCallbacksClient.put(packetId, callback);
	}

	public void processPackets() {
		while (!this.getPacketQueue().isEmpty()) {
			Packet toProcess = this.getPacketQueue().remove();
			try {
				Packet created = Packet.newPacketInstance(toProcess.getId(), toProcess.getData());
				this.packetCallbacksClient.get(created.getId()).accept(this, created);
			} catch (Exception e) {
				log.error("Failed to process Client Packet. Reason: {}", e);
			}
		}
	}

	public static void handleTextClient(SocketClient cli, Packet packet) {
		TextPacket textPacket = (TextPacket) packet;
		log.info("[CLIENT] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
				textPacket.getFrom(), textPacket.getMessage());
	}

	public static void handleObjectMoveClient(SocketClient cli, Packet packet) {
		ObjectMovePacket objectMovePacket = (ObjectMovePacket) packet;
		log.info("[CLIENT] Recieved ObjectMove Packet for Game Object {} ID {}",
				EntityType.valueOf(objectMovePacket.getEntityType()), objectMovePacket.getEntityId());
	}

	public static void handleUpdateClient(SocketClient cli, Packet packet) {
		UpdatePacket updatePacket = (UpdatePacket) packet;
		log.info("[CLIENT] Recieved PlayerUpdate Packet for Player ID {}", updatePacket.getPlayerId());
	}
}
