package com.jrealm.net.server.packet;

import lombok.Data;

@Data
public class Packet {

	private byte packetId;
	private byte[] body;

	public Packet(byte packetId, byte[] body) {
		this.packetId = packetId;
		this.body = body;
	}

	public byte getPacketId() {
		return this.packetId;
	}

}
