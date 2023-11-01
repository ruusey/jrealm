package com.jrealm.net;

import java.io.DataOutputStream;

import com.jrealm.game.util.Tuple;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Packet implements GameMessage{
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
	
	public static Packet newPacketInstance(final byte packetId, final byte[] data) {
		final Tuple<Class<? extends Packet>, PacketType> typeInfo = PacketType.valueOf(packetId);
		final Class<? extends Packet> packetClass = typeInfo.getX();
		try {
			Packet packetObj = packetClass.getDeclaredConstructor(byte.class, byte[].class).newInstance(packetId, data);
			//packetObj.readData(data);
			return packetObj;
		} catch (Exception e) {
			log.error("Failed to construct instance of {}. Reason: {}", packetClass, e.getMessage());
		}
		return null;
	}
}
