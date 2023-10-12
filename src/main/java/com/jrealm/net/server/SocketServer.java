package com.jrealm.net.server;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.net.Packet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocketServer extends Thread {
	private static final int BUFFER_CAPACITY = 65536 * 10;

	private ServerSocket serverSocket;
	private boolean shutdown = false;
	public byte[] localBuffer = new byte[SocketServer.BUFFER_CAPACITY];
	public int localBufferIndex = 0;
	public byte[] remoteBuffer = new byte[SocketServer.BUFFER_CAPACITY];
	public int remoteBufferIndex = 0;
	public long lastUpdate = 0;
	public long previousTime = 0;
	public long localNoDataTime = System.currentTimeMillis();
	public long remoteNoDataTime = System.currentTimeMillis();

	public Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();

	private Socket clientSocket;

	public SocketServer(int port) {
		SocketServer.log.info("Creating local HTTP server at port {}", port);
		try {
			this.serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			SocketServer.log.error("Failed to create server socket. Reason: {}", e.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			this.clientSocket = this.serverSocket.accept();
			
		}catch(Exception e) {
			log.error("Failed to accept incoming socket connection, exiting...",e);
			return;
		}

		while (!this.shutdown) {
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
						this.packetQueue.add(new Packet(packetId, packetBytes));

					}
				}
				//Thread.sleep(1000);

			}catch(Exception e) {
				SocketServer.log.error("Failed to parse client input {}", e.getMessage());
			}

		}

	}
}
