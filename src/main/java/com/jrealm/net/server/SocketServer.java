package com.jrealm.net.server;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.BlankPacket;
import com.jrealm.net.Packet;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.server.packet.TextPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class SocketServer extends Thread {
	public static final String LOCALHOST = "127.0.0.1";

	private static final int BUFFER_CAPACITY = 65536 * 10;

	private ServerSocket serverSocket;
	private boolean shutdownSocketAccept = false;
	private boolean shutdownProcessing = false;
	private byte[] localBuffer = new byte[SocketServer.BUFFER_CAPACITY];
	private int localBufferIndex = 0;
	private byte[] remoteBuffer = new byte[SocketServer.BUFFER_CAPACITY];
	private int remoteBufferIndex = 0;
	private long lastUpdate = 0;
	private long previousTime = 0;
	private long localNoDataTime = System.currentTimeMillis();
	private long remoteNoDataTime = System.currentTimeMillis();

	private final Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();

	private Map<String, Socket> clients = new ConcurrentHashMap<>();

	public SocketServer(int port) {
		SocketServer.log.info("Creating local server at port {}", port);
		try {
			this.serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			SocketServer.log.error("Failed to create server socket. Reason: {}", e.getMessage());
		}
	}

	@Override
	public void run() {

		Runnable socketAccept = () -> {
			log.info("Server now accepting inbound connections...");
			while (!this.shutdownSocketAccept) {
				try {
					Socket socket = this.serverSocket.accept();
					String remoteAddr = socket.getInetAddress().getHostAddress();
					log.info("Accepted new socket connection from {}", remoteAddr);
					this.clients.put(remoteAddr, socket);
					TextPacket welcomeMessage = TextPacket.create("SYSTEM", "Ruusey", "Welcome to JRealm!");
					welcomeMessage.serializeWrite(new DataOutputStream(socket.getOutputStream()));
					log.info("Server accepted new connection from Remote Address {}", remoteAddr);
				} catch (Exception e) {
					log.error("Failed to accept incoming socket connection, exiting...", e);
					// return;
				}
			}
		};

		Runnable processClients = () -> {
			log.info("Begin processing client packets...");
			while (!this.shutdownProcessing) {
				try {
					this.enqueueClientPackets();
					Thread.sleep(100);
				} catch (Exception e) {
					SocketServer.log.error("Failed to parse client input {}", e.getMessage());
				}
			}
		};

		WorkerThread.submitAndRun(socketAccept, processClients);

	}

	private void enqueueClientPackets() {
		for (Map.Entry<String, Socket> client : this.clients.entrySet()) {
			try {
				InputStream stream = client.getValue().getInputStream();
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
				SocketServer.log.error("Failed to parse client input {}", e.getMessage());
			}
		}
	}
}
