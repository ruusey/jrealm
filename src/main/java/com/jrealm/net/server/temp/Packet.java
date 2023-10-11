package com.jrealm.net.server.temp;

import java.io.DataOutputStream;

import com.jrealm.net.packet.client.temp.GameMessage;

public abstract class Packet implements GameMessage{
	private byte id;
	private byte[] data;
	
	public Packet(byte id, byte[] data) {
		this.id = id;
		this.data = data;
	}
	
	public byte getId() {
		return this.id;
	}
	
	public byte[] getData() {
		return this.data;
	}
	
	
	public void addHeader(DataOutputStream stream) throws Exception{
		stream.writeByte(this.id);
		stream.writeInt(this.data.length + NetConstants.PACKET_HEADER_SIZE);
	}
}
