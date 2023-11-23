package com.jrealm.net.server;

import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import com.jrealm.net.BlankPacket;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ProcessingThread extends Thread{
	private static final int BUFFER_CAPACITY = 65536 * 10;

	private Socket clientSocket;
	private SocketServer server;
	private boolean shutdownProcessing = false;
	private byte[] remoteBuffer = new byte[ProcessingThread.BUFFER_CAPACITY];
	private int remoteBufferIndex = 0;
	private long lastUpdate = 0;
	private long previousTime = 0;
	private long localNoDataTime = System.currentTimeMillis();
	private long remoteNoDataTime = System.currentTimeMillis();

	public ProcessingThread(SocketServer server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		while (!this.shutdownProcessing) {
			try {
				this.enqueueClientPackets();
			} catch (Exception e) {
				ProcessingThread.log.error("Failed to parse client input {}", e.getMessage());
			}
		}
	}

	private void enqueueClientPackets() {
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
					this.server.getPacketQueue().add(newPacket);
				}
			}
		} catch (Exception e) {
			this.server.getClients().remove(this.clientSocket.getInetAddress().getHostAddress());
			this.shutdownProcessing = true;
			ProcessingThread.log.error("Failed to parse client input {}", e.getMessage());
		}

	}
}
