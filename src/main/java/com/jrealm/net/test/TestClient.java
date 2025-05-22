package com.jrealm.net.test;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.core.IOService;
import com.jrealm.util.TimedWorkerThread;
import com.jrealm.util.WorkerThread;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestClient implements Runnable {
	private static final int BUFFER_CAPACITY = 65536 * 10;
	private Socket clientSocket;
	private boolean shutdown = false;
	private byte[] remoteBuffer = new byte[TestClient.BUFFER_CAPACITY];
	private int remoteBufferIndex = 0;
	private long lastUpdate = 0;
	private long previousTime = 0;
	private long lastDataTime = System.currentTimeMillis();
	private long currentBytesRecieved = 0;
	private volatile Queue<Packet> inboundPacketQueue = new ConcurrentLinkedQueue<>();
	private volatile Queue<Packet> outboundPacketQueue = new ConcurrentLinkedQueue<>();
	private TimedWorkerThread sendPacketThread = null;
	private TimedWorkerThread readPacketThread = null;

	public TestClient(String targetHost, int port) {
		try {
			this.clientSocket = new Socket(targetHost, port);
			this.clientSocket.setTcpNoDelay(true);
		} catch (Exception e) {
			TestClient.log.error("Failed to create ClientSocket, Reason: {}", e.getMessage());
		}
	}

	@Override
	public void run() {
		this.startBandwidthMonitor();
		// this.monitorLastReceived();

		final Runnable readPackets = () -> {
			this.readPackets();
		};
		final Runnable sendPackets = () -> {
			this.sendPackets();
		};
		
		final Runnable testSend = () -> {
			try {
				this.sendRemote(TestPacket.generate());
			}catch(Exception e) {
				log.error("Failed to send test packet. Reason: {}", e.getMessage());
			}
		};
		TimedWorkerThread testSendThread = new TimedWorkerThread(testSend, 1104);

		this.sendPacketThread = new TimedWorkerThread(sendPackets, 64);
		this.readPacketThread = new TimedWorkerThread(readPackets, 64);
		WorkerThread.submitAndForkRun(this.readPacketThread, this.sendPacketThread);
		WorkerThread.submitAndForkRun(testSendThread);
	}

	private void readPackets() {
		try {
			final InputStream stream = this.clientSocket.getInputStream();

			final int bytesRead = stream.read(this.remoteBuffer, this.remoteBufferIndex,
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
					final byte packetId = this.remoteBuffer[0];
					final byte[] packetBytes = new byte[packetLength];
					System.arraycopy(this.remoteBuffer, 5, packetBytes, 0, packetLength);
					if (this.remoteBufferIndex > packetLength) {
						System.arraycopy(this.remoteBuffer, packetLength, this.remoteBuffer, 0,
								this.remoteBufferIndex - packetLength);
					}
					this.currentBytesRecieved += packetLength;
					this.remoteBufferIndex -= packetLength;
					final Class<? extends Packet> packetClass = PacketType.valueOf(packetId);
					final Packet newPacket = IOService.readStream(packetClass, packetBytes);
					newPacket.setSrcIp(this.clientSocket.getInetAddress().getHostAddress());
					this.inboundPacketQueue.add(newPacket);
				}
			}
		} catch (Exception e) {
			this.shutdown = true;
			this.sendPacketThread.setShutdown(true);
			this.readPacketThread.setShutdown(true);
			TestClient.log.error("Failed to parse client input. Reason {}", e);
		}
	}

	private void sendPackets() {
		OutputStream stream = null;
		try {
			stream = this.clientSocket.getOutputStream();
		} catch (Exception e) {
			return;
		}
		final DataOutputStream dos = new DataOutputStream(stream);
		while (!this.outboundPacketQueue.isEmpty() && !this.shutdown) {
			final Packet toSend = this.outboundPacketQueue.remove();
			try {
				toSend.serializeWrite(dos);
			} catch (Exception e) {
				final String remoteAddr = this.clientSocket.getInetAddress().getHostAddress();
				TestClient.log.error("Failed to send Packet to remote addr {}, Reason: {}", remoteAddr, e);
			}
		}
	}

	/**
	 * Enqueues a Packet instance to be sent to the remote during the next
	 * processing cycle
	 * 
	 * @param packet Packet to send to remote server
	 * @throws Exception
	 */
	public void sendRemote(Packet packet) throws Exception {
		if (this.clientSocket == null)
			throw new Exception("Client socket is null/not yet established");

		this.outboundPacketQueue.add(packet);
	}

	@SuppressWarnings("unused")
	private void monitorLastReceived() {
		final Runnable monitorLastRecieved = () -> {
			while (!this.shutdown) {
				try {
					if ((System.currentTimeMillis() - this.lastDataTime) > 5000) {
						this.shutdown = true;
					}
				} catch (Exception e) {
					log.error("Failed to wait during last recieved monitor. Reason: {}", e);
				}
			}
		};
		WorkerThread.submitAndForkRun(monitorLastRecieved);
	}

	public void startBandwidthMonitor() {
		final Runnable run = () -> {
			while (!this.shutdown) {
				try {
					final long bytesRead = this.currentBytesRecieved;
					TestClient.log.info("[CLIENT] current read rate = {} kbit/s", (float) (bytesRead / 1024.0f) * 8.0f);
					this.currentBytesRecieved = 0;
					Thread.sleep(1000);
				} catch (Exception e) {
					log.error("Failed to monitor bandwidth usage. Reason: {}", e);
				}
			}
		};
		WorkerThread.submitAndForkRun(run);
	}

	public static String getLocalAddr() throws Exception {
		final String[] split = InetAddress.getLocalHost().toString().split("/");
		String addr = null;
		if (split.length > 1) {
			addr = split[1];
		} else {
			addr = split[0];
		}
		return addr;
	}
}
