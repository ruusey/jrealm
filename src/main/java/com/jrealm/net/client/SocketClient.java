package com.jrealm.net.client;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.game.GameLauncher;
import com.jrealm.game.util.TimedWorkerThread;
import com.jrealm.net.BlankPacket;
import com.jrealm.net.Packet;
import com.jrealm.net.server.SocketServer;
import com.jrealm.net.server.packet.TextPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class SocketClient implements Runnable {
	public static String PLAYER_USERNAME = null;
	public static String SERVER_ADDR = null;
	public static int CLASS_ID = 0;

	private static final int BUFFER_CAPACITY = 65536 * 10;

	private Socket clientSocket;
	private boolean shutdown = false;
	private byte[] remoteBuffer = new byte[SocketClient.BUFFER_CAPACITY];
	private int remoteBufferIndex = 0;
	private long lastUpdate = 0;
	private long previousTime = 0;
	private long localNoDataTime = System.currentTimeMillis();
	private long remoteNoDataTime = System.currentTimeMillis();

	private final Queue<Packet> inboundPacketQueue = new ConcurrentLinkedQueue<>();
	private final Queue<Packet> outboundPacketQueue = new ConcurrentLinkedQueue<>();
	
	public SocketClient(String targetHost, int port) {
		try {
			this.clientSocket = new Socket(targetHost, port);
		} catch (Exception e) {
			SocketClient.log.error("Failed to create ClientSocket, Reason: {}", e.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			Thread.sleep(100);
			this.sendRemote(TextPacket.create(SocketClient.PLAYER_USERNAME, "SYSTEM", "LoginRequest"));
		} catch (Exception e) {
			log.error("Failed to send initial LoginRequest. Reason: {}", e);
		}
		
		Runnable readPackets = () -> {
			this.readPackets();
		};
		Runnable sendPackets = () -> {
			this.sendPackets();	
		};
		TimedWorkerThread readThread = new TimedWorkerThread(readPackets, 32);
		TimedWorkerThread sendThread = new TimedWorkerThread(sendPackets, 20);
		sendThread.start();
		readThread.start();
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
					BlankPacket newPacket = new BlankPacket(packetId, packetBytes);
					newPacket.setSrcIp(this.clientSocket.getInetAddress().getHostAddress());
					this.inboundPacketQueue.add(newPacket);
				}
			}
		} catch (Exception e) {
			SocketClient.log.error("Failed to parse client input. Reason {}", e);
		}
	}
	
	private void sendPackets() {
		while(!this.outboundPacketQueue.isEmpty()) {
			Packet toSend = this.outboundPacketQueue.remove();
			try {
				OutputStream stream = this.clientSocket.getOutputStream();
				DataOutputStream dos = new DataOutputStream(stream);
				toSend.serializeWrite(dos);
			}catch(Exception e) {
				String remoteAddr = this.clientSocket.getInetAddress().getHostAddress();
				SocketClient.log.error("Failed to send Packet to remote addr {}, Reason: {}", remoteAddr, e);
			}
		}
	}

	/**
	 * Enqueues a Packet instance to be sent to the remote during the next
	 * processing cycle
	 * @param packet Packet to send to remote server
	 * @throws Exception
	 */
	public void sendRemote(Packet packet) throws Exception {
		if (this.clientSocket == null)
			throw new Exception("Client socket is null/not yet established");
		
		this.outboundPacketQueue.add(packet);
	}

	public static String getLocalAddr() throws Exception{
		if(GameLauncher.LOCAL_SERVER) return SocketServer.LOCALHOST;
		String[] split = InetAddress.getLocalHost().toString().split("/");
		String addr = null;
		if(split.length>1) {
			addr = split[1];
		}else {
			addr = split[0];
		}
		return addr;
	}
}
