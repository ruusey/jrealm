package com.jrealm.net.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.Packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class SocketServer implements Runnable {
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

	private volatile Map<String, ProcessingThread> clients = new ConcurrentHashMap<>();

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
			SocketServer.log.info("Server now accepting inbound connections...");
			while (!this.shutdownSocketAccept) {
				try {
					Socket socket = this.serverSocket.accept();
					String remoteAddr = socket.getInetAddress().getHostAddress();
					ProcessingThread procesingThread = new ProcessingThread(this, socket);
					this.clients.put(remoteAddr, procesingThread);
					procesingThread.start();
					SocketServer.log.info("Server accepted new connection from Remote Address {}", remoteAddr);
				} catch (Exception e) {
					SocketServer.log.error("Failed to accept incoming socket connection, exiting...", e);
				}
			}
		};


		WorkerThread.submitAndForkRun(socketAccept);
	}
}
