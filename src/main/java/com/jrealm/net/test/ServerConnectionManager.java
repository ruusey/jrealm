package com.jrealm.net.test;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.Player;
import com.jrealm.net.Packet;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.ProcessingThread;
import com.jrealm.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class ServerConnectionManager implements Runnable {
	public static final String LOCALHOST = "127.0.0.1";

	private ServerSocket serverSocket;
	private boolean shutdownSocketAccept = false;
	private boolean shutdownProcessing = false;

	private volatile Map<String, TestServer> clients = new ConcurrentHashMap<>();
	private volatile Map<String, Long> clientConnectTime = new ConcurrentHashMap<>();

	private long lastWriteSampleTime = Instant.now().toEpochMilli();
	private Map<String, Long> remoteAddresses = new HashMap<>();


	public ServerConnectionManager(int port) {
		ServerConnectionManager.log.info("Creating local server at port {}", port);
		try {
			this.serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			ServerConnectionManager.log.error("Failed to create server socket. Reason: {}", e.getMessage());
			System.exit(-1);
		}
	}

	
	public Map.Entry<String, TestServer> getPlayerProcessingThreadEntry(Player player) {
		Map.Entry<String, TestServer> result = null;
		for (final Entry<String, TestServer> client : this.getClients().entrySet()) {
			if (this.remoteAddresses.get(client.getKey()) == player.getId()) {
				result = client;
			}
		}
		return result;
	}

	public TestServer getPlayerProcessingThread(Player player) {
		return getPlayerProcessingThreadEntry(player).getValue();
	}

	public String getPlayerRemoteAddress(Player player) {
		return getPlayerProcessingThreadEntry(player).getKey();
	}

	public void disconnectPlayer(Player player) {
		final TestServer playerThread = this.getPlayerProcessingThread(player);
		final String playerRemoteAddr = this.getPlayerRemoteAddress(player);
		try {
			log.info("Disconnecting Player {}", player.getName());
			playerThread.shutDown();
			this.getClients().remove(playerRemoteAddr);
		} catch (Exception e) {
			log.error("Failed to disconnect player. Reason:  {}", e);
		}
	}

	@Override
	public void run() {
		final Runnable socketAccept = () -> {
			ServerConnectionManager.log.info("Server now accepting inbound connections...");
			while (!this.shutdownSocketAccept) {
				try {
					final Socket socket = this.serverSocket.accept();
					socket.setTcpNoDelay(true);
					socket.setSoTimeout((int) GlobalConstants.SOCKET_READ_TIMEOUT);
					final String remoteAddr = socket.getInetAddress().getHostAddress();
					final TestServer processingThread = new TestServer(this, socket);
					this.clients.put(remoteAddr, processingThread);
					processingThread.start();
					ServerConnectionManager.log.info("Server accepted new connection from Remote Address {}",
							remoteAddr);
					this.clientConnectTime.put(remoteAddr, Instant.now().toEpochMilli());
				} catch (Exception e) {
					ServerConnectionManager.log.error("Failed to accept incoming socket connection, exiting...", e);
				}
			}
		};

		// Expire connections if the handshake is not complete after 2.5 seconds
		final Runnable timeoutCheck = () -> {
			ServerConnectionManager.log.info("Beginning connection timeout check...");
			while (!this.shutdownSocketAccept) {
				try {
					final Set<String> toRemove = new HashSet<>();
					for (Map.Entry<String, TestServer> entry : this.clients.entrySet()) {
						final long timeSinceConnect = Instant.now().toEpochMilli()
								- this.clientConnectTime.get(entry.getKey());
						if (timeSinceConnect > GlobalConstants.SOCKET_READ_TIMEOUT
								&& !entry.getValue().isHandshakeComplete()) {
							toRemove.add(entry.getKey());
						}
					}

					for (String remove : toRemove) {
						TestServer thread = this.clients.remove(remove);
						thread.shutDown();
						ServerConnectionManager.log.info("Removed expired connection {}", thread.getClientSocket());
					}
					Thread.sleep(250);
				} catch (Exception e) {
					ServerConnectionManager.log.error("Failed to check expired connections. Reason: {}", e);
				}
			}
		};
		WorkerThread.submitAndForkRun(socketAccept, timeoutCheck);
	}

}
