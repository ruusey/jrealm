package com.jrealm.net.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class SocketServer implements Runnable {
	
	public static final String LOCALHOST = "127.0.0.1";

	private ServerSocket serverSocket;
	private boolean shutdownSocketAccept = false;
	private boolean shutdownProcessing = false;

	private volatile Map<String, ProcessingThread> clients = new ConcurrentHashMap<>();
	private volatile Map<String, Long> clientConnectTime = new ConcurrentHashMap<>();

	public SocketServer(int port) {
		SocketServer.log.info("Creating local server at port {}", port);
		try {
			this.serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			SocketServer.log.error("Failed to create server socket. Reason: {}", e.getMessage());
			System.exit(-1);
		}
	}

	@Override
	public void run() {
		final Runnable socketAccept = () -> {
			SocketServer.log.info("Server now accepting inbound connections...");
			while (!this.shutdownSocketAccept) {
				try {
					final Socket socket = this.serverSocket.accept();
					socket.setTcpNoDelay(true);
					socket.setSoTimeout((int) GlobalConstants.SOCKET_READ_TIMEOUT);
					final String remoteAddr = socket.getInetAddress().getHostAddress();
					final ProcessingThread processingThread = new ProcessingThread(this, socket);
					this.clients.put(remoteAddr, processingThread);
					processingThread.start();
					SocketServer.log.info("Server accepted new connection from Remote Address {}", remoteAddr);
					this.clientConnectTime.put(remoteAddr, Instant.now().toEpochMilli());
				} catch (Exception e) {
					SocketServer.log.error("Failed to accept incoming socket connection, exiting...", e);
				}
			}
		};

		// Expire connections if the handshake is not complete after 2.5 seconds
		final Runnable timeoutCheck = () -> {
			SocketServer.log.info("Beginning connection timeout check...");
			while (!this.shutdownSocketAccept) {
				try {
					final Set<String> toRemove = new HashSet<>();
					for (Map.Entry<String, ProcessingThread> entry : this.clients.entrySet()) {
						final long timeSinceConnect = Instant.now().toEpochMilli()
								- this.clientConnectTime.get(entry.getKey());
						if (timeSinceConnect > GlobalConstants.SOCKET_READ_TIMEOUT
								&& !entry.getValue().isHandshakeComplete()) {
							toRemove.add(entry.getKey());
						}
					}

					for (String remove : toRemove) {
						ProcessingThread thread = this.clients.remove(remove);
						thread.setShutdownProcessing(true);
						SocketServer.log.info("Removed expired connection {}", thread.getClientSocket());
					}
					Thread.sleep(250);
				} catch (Exception e) {
					SocketServer.log.error("Failed to check expired connections. Reason: {}", e);
				}
			}
		};
		WorkerThread.submitAndForkRun(socketAccept, timeoutCheck);
	}
}
