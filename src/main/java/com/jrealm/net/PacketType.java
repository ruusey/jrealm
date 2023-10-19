package com.jrealm.net;

import java.util.HashMap;
import java.util.Map;

public enum PacketType {
	UPDATE((byte)2, false), OBJECT_MOVE((byte)3, false), TEXT((byte)4, true), HEARTBEAT((byte)5, true);
	
	private static Map<Byte, PacketType> map = new HashMap<>();
	
	private byte packetId;
	private boolean isServer;
	static {
		for(PacketType et : PacketType.values()) {
			map.put(et.getEntityTypeId(), et);
		}
	}
	private PacketType(byte entityTypeId, boolean isServer) {
		this.packetId = entityTypeId;
		this.isServer = isServer;
	}
	
	public byte getEntityTypeId() {
		return this.packetId;
	}
	
	public boolean getIsServer() {
		return this.isServer;
	}
	
	public static PacketType valueOf(byte value) {
		return map.get(Byte.valueOf(value));
	}
	
	public static PacketType valueOf(int value) {
		return map.get(Byte.valueOf((byte)value));
	}
	
	public static boolean isServerPacket(Packet packet) {
		return isServerPacket(packet.getId());
	}
	
	public static boolean isServerPacket(byte packetId) {
		return map.get(packetId).getIsServer();
	}
}
