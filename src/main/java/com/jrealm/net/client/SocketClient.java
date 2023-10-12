package com.jrealm.net.client;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.net.server.packet.Packet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocketClient extends Thread {
	private static final int BUFFER_CAPACITY = 65536 * 10;

	private Socket clientSocket;
	private boolean shutdown = false;
	public byte[] localBuffer = new byte[SocketClient.BUFFER_CAPACITY];
	public int localBufferIndex = 0;
	public byte[] remoteBuffer = new byte[SocketClient.BUFFER_CAPACITY];
	public int remoteBufferIndex = 0;
	public long lastUpdate = 0;
	public long previousTime = 0;
	public long localNoDataTime = System.currentTimeMillis();
	public long remoteNoDataTime = System.currentTimeMillis();

	public static final Queue<Packet> packetQueue = new ConcurrentLinkedQueue<Packet>();


	public SocketClient(int port) {
		try {

			this.clientSocket = new Socket("127.0.0.1", port);
		} catch (Exception e) {
			SocketClient.log.error("Failed to create ClientSocket, Reason: {}", e.getMessage());
		}
	}

	public void writeString(String txt) throws Exception {
		OutputStream stream = this.clientSocket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(stream);
		dos.writeByte(1);
		dos.writeInt(txt.length() + 5 + 4);
		dos.writeUTF(txt);
		dos.writeInt(99);
		dos.flush();

	}

}
