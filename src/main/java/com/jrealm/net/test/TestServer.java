package com.jrealm.net.test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.core.IOService;
import com.jrealm.net.server.SocketServer;
import com.jrealm.util.WorkerThread;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class TestServer extends Thread {
	private static final int BUFFER_CAPACITY = 65536 * 10;

	private Socket clientSocket;
	private TestServerMapper mapper;
	private ServerConnectionManager server;
	private boolean shutdownProcessing = false;
	private byte[] remoteBuffer = new byte[TestServer.BUFFER_CAPACITY];
	private int remoteBufferIndex = 0;
	private long lastUpdate = 0;
	private long previousTime = 0;
	private long lastDataTime = System.currentTimeMillis();
	private final Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();
	private boolean handshakeComplete = false;

	
	private volatile Queue<Packet> outboundPacketQueue = new ConcurrentLinkedQueue<>();
	private volatile Map<Long, ConcurrentLinkedQueue<Packet>> playerOutboundPacketQueue = new HashMap<Long, ConcurrentLinkedQueue<Packet>>();
	private long bytesWritten = 0;
	
	
	public TestServer(ServerConnectionManager server, Socket clientSocket) {
		this.mapper = new TestServerMapper();
		this.server = server;
		this.clientSocket = clientSocket;
	}
	
	public void shutDown() {
		this.shutdownProcessing=true;
		try {
			this.clientSocket.close();
		} catch (IOException e) {
			log.error("Failed to shutdown server thread {}", e);
		}
	}

	@Override
	public void run() {
		// this.monitorLastReceived();
		while (!this.shutdownProcessing) {
			try {
				this.enqueueClientPackets();
			} catch (Exception e) {
				TestServer.log.error("Failed to parse client input {}", e.getMessage());
			}
		}
		this.shutdownAndCleanup();
		log.info("Server shutdown complete.");
	}
	
	private void shutdownAndCleanup() {
		try {
			this.clientSocket.close();
			this.remoteBuffer = null;
			this.packetQueue.clear();
			log.info("Client socket succesfully closed");
		} catch (IOException e1) {
			log.error("Failed to close client socket");
		}
	}
	
	private void sendGameData() {
		long startNanos = System.nanoTime();
		final List<Packet> packetsToBroadcast = new ArrayList<>();
		// TODO: Possibly rework this queue as we dont usually send stuff globally
		while (!this.outboundPacketQueue.isEmpty()) {
			packetsToBroadcast.add(this.outboundPacketQueue.remove());
		}
		final List<Map.Entry<String, TestServer>> staleProcessingThreads = new ArrayList<>();
		for (final Entry<String, TestServer> client : this.server.getClients().entrySet()) {
			if (client.getValue().getClientSocket().isClosed() || !client.getValue().getClientSocket().isConnected()) {
				staleProcessingThreads.add(client);
			}
		}
		staleProcessingThreads.forEach(thread -> {
			try {
				thread.getValue().shutDown();
				this.server.getClients().remove(thread.getKey());
				this.server.getClients().remove(thread.getKey(), thread.getValue());
			} catch (Exception e) {
				log.error("[SERVER] Failed to remove stale processing threads. Reason:  {}", e);
			}
		});
		for (final Map.Entry<String, TestServer> client : this.server.getClients().entrySet()) {
			try {
				long targetClient = 12345l;
				// Dequeue and send any player specific packets
				final List<Packet> playerPackets = new ArrayList<>();
				final ConcurrentLinkedQueue<Packet> playerPacketsToSend = this.playerOutboundPacketQueue
						.get(targetClient);

				while ((playerPacketsToSend != null) && !playerPacketsToSend.isEmpty()) {
					playerPackets.add(playerPacketsToSend.remove());
				}

				final OutputStream toClientStream = client.getValue().getClientSocket().getOutputStream();
				final DataOutputStream dosToClient = new DataOutputStream(toClientStream);

				// Globally sent packets
				for (final Packet packet : packetsToBroadcast) {
					this.bytesWritten += packet.serializeWrite(dosToClient);
				}

				for (final Packet packet : playerPackets) {
					this.bytesWritten += packet.serializeWrite(dosToClient);
				}
			} catch (Exception e) {
				TestServer.log.error("[SERVER] Failed to get OutputStream to Client. Reason: {}", e);
			}
		}
	}
	
	public void processServerPackets(ServerConnectionManager accepter) {
		for (final Entry<String, TestServer> thread : accepter.getClients().entrySet()) {
			if (!thread.getValue().isShutdownProcessing()) {
				// Read all packets from the ProcessingThread (client's) queue
				while (!thread.getValue().getPacketQueue().isEmpty()) {
					final Packet packet = thread.getValue().getPacketQueue().remove();
					try {
						Packet created = packet;
						created.setSrcIp(packet.getSrcIp());
						// Invoke packet callback
						final List<Function<Packet, Packet>> packetHandles = this.mapper.getPacketCallbacksServer()
								.get(packet.getId());
						long start = System.nanoTime();
						if (packetHandles != null) {
							for (Function<Packet, Packet> handler : packetHandles) {
								try {
									Packet retVal = handler.apply(created);
								} catch (Throwable e) {
									log.error("Failed to invoke packet callback. Reason: {}", e);
								}
							}
							log.info("Invoked {} packet callbacks for PacketType {} using reflection in {} nanos",
									packetHandles.size(), PacketType.valueOf(created.getId()).getY(),
									(System.nanoTime() - start));
						}
						start = System.nanoTime();
						if (this.mapper.getPacketCallbacksServer().get(created.getId()) == null) {
							final List<MethodHandle> callBacHandles = this.mapper.getUserPacketCallbacksServer()
									.get(created.getId());
							if (callBacHandles != null) {
								callBacHandles.forEach(callBack -> {
									try {
										callBack.invokeExact(this, created);
									} catch (Throwable e) {
										log.error(
												"Failed to invoke user server packet callback for packet id {}. Callback: {}. Reason: {}",
												created.getId(), callBack, e.getMessage());
									}
								});
							}
						} else {
							List<Function<Packet, Packet>> callbacks = this.mapper.getPacketCallbacksServer()
									.get(created.getId());
							for (Function<Packet, Packet> callback : callbacks) {
								callback.apply(created);
							}
						}
						log.debug("Invoked callback for PacketType {} using map in {} nanos",
								PacketType.valueOf(created.getId()).getY(), (System.nanoTime() - start));
					} catch (Exception e) {
						TestServer.log.error("Failed to process server packets {}", e);
						thread.getValue().shutDown();
					}
				}
			} else {
				// Player Disconnect routine
				final Long dcPlayerId = this.server.getRemoteAddresses().get(thread.getKey());
				if (dcPlayerId == null) {
					thread.getValue().shutDown();
					return;
				}

				this.server.getClients().remove(thread.getKey());
			}
		}
	}


	private void enqueueClientPackets() {
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
					final Class<?> packetClass = PacketType.valueOf(packetId).getX();
					final Packet nPacket = IOService.readStream(packetClass, packetBytes);
					nPacket.setSrcIp(this.clientSocket.getInetAddress().getHostAddress());
					this.packetQueue.add(nPacket);
				}
			}
		} catch (Exception e) {
			this.shutdownProcessing = true;
			TestServer.log.error("Failed to parse client input {}", e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	private void monitorLastReceived() {
		Runnable monitorLastRecieved = () -> {
			while (!this.shutdownProcessing) {
				try {
					if ((System.currentTimeMillis() - this.lastDataTime) > 5000) {
						this.server.getClients().remove(this.clientSocket.getInetAddress().getHostAddress());
						this.shutdownProcessing = true;
					}
				} catch (Exception e) {
					log.error("Failed to monitor last recieved data. Reason: {}", e);
				}

			}
		};
		WorkerThread.submitAndForkRun(monitorLastRecieved);
	}
}
