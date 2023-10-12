package com.jrealm.net.server;

import com.jrealm.net.client.SocketClient;
import com.jrealm.net.server.packet.Packet;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Testbed {

	public static void main(String[] args) {
		SocketServer socketServer = new SocketServer(2222);
		socketServer.start();
		SocketClient socketClient = new SocketClient(2222);
		try {
			socketClient.writeString("HelloWorld");
		}catch(Exception e) {
			Testbed.log.error("Failed ", e);
		}

		while (true) {
			try {
				Packet packetReceived = socketServer.packetQueue.remove();
				TextPacket txtPacket = new TextPacket(packetReceived);
				System.out.println(txtPacket.getMessage());
			} catch (Exception e) {
				Testbed.log.error("Failed to get text packet, Reason: {}", e.getMessage());
			}
		}

	}

}
