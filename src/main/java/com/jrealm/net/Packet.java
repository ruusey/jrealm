package com.jrealm.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class Packet implements GameMessage{
	private byte id;
	private byte[] data;
	
	public Packet() {
		this.id = -1;
		this.data = null;
	}
	
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
	
	public void addHeader(DataOutputStream stream, byte packetId, int contentLength) throws Exception{
		stream.writeByte(packetId);
		stream.writeInt(contentLength + NetConstants.PACKET_HEADER_SIZE);
	}

	@Override
	public void readData(DataInputStream stream) throws Exception {
		
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		
	}

	@Override
	public void readData(Packet packet) throws Exception {
		
	}
}
