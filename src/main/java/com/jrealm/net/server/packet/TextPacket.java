package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import lombok.Data;

@Data
public class TextPacket {
	private String message;

	public TextPacket(Packet packet) {
		DataInputStream stream = new DataInputStream(new ByteArrayInputStream(packet.getBody()));
		try {
			this.message = stream.readUTF();
			int test = stream.readInt();
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
