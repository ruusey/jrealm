package com.jrealm.net.test;

import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.core.IOService;
import com.jrealm.util.TimedWorkerThread;
import com.jrealm.util.WorkerThread;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ClientConnectionThread extends Thread {
	private static final int BUFFER_CAPACITY = 65536 * 10;

	private Socket clientSocket;
	private ServerConnectionManager mgr;
	private boolean shutdownProcessing = false;
	private byte[] remoteBuffer = new byte[ClientConnectionThread.BUFFER_CAPACITY];
	private int remoteBufferIndex = 0;
	private long lastUpdate = 0;
	private long previousTime = 0;
	private long lastDataTime = System.currentTimeMillis();
	private final Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();
	private boolean handshakeComplete = false;
	
	public ClientConnectionThread(Socket clientSocket, ServerConnectionManager mgr) {
		this.clientSocket = clientSocket;
		this.mgr = mgr;
	}
	
	@Override
	public void run() {
		final Runnable readRemote = () ->{
			try {
				this.enqueueClientPackets();
			}catch(Exception e) {
				log.error("Failed to read remot. Reason: {}", e.getMessage());
			}
		};
		TimedWorkerThread thread = new TimedWorkerThread(readRemote, 64);
		WorkerThread.submitAndForkRun(thread);
	}

	private void enqueueClientPackets() throws Exception {
		try {
			InputStream stream = this.clientSocket.getInputStream();
			int bytesRead = stream.read(this.remoteBuffer, this.remoteBufferIndex,
					this.remoteBuffer.length - this.remoteBufferIndex);
			this.lastDataTime = System.currentTimeMillis();
			if (bytesRead == -1)
				throw new SocketException("end of stream");
			if (bytesRead > 0) {
				this.remoteBufferIndex += bytesRead;
				while (this.remoteBufferIndex >= 5) {
					int packetLength = (ByteBuffer.allocate(4).put(this.remoteBuffer[1]).put(this.remoteBuffer[2])
							.put(this.remoteBuffer[3]).put(this.remoteBuffer[4]).rewind()).getInt();
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
					final Class<?> packetClass = PacketType.valueOf(packetId);
					final Packet nPacket = IOService.readStream(packetClass, packetBytes);
					nPacket.setSrcIp(this.clientSocket.getInetAddress().getHostAddress());
					this.packetQueue.add(nPacket);
				}
			}
		} catch (Exception e) {
			this.shutdownProcessing = true;
			ClientConnectionThread.log.error("Failed to parse client input {}", e.getMessage());
		}
	}
	
}
