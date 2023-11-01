package com.jrealm.net;

import java.util.HashMap;
import java.util.Map;

import com.jrealm.game.util.Tuple;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.net.server.packet.TextPacket;

public enum PacketType {
	PLAYER_MOVE    ((byte) 1, false, PlayerMovePacket.class),
	UPDATE         ((byte) 2, false, UpdatePacket.class), 
	OBJECT_MOVE    ((byte) 3, false, ObjectMovePacket.class),
	TEXT           ((byte) 4, true, TextPacket.class), 
	HEARTBEAT      ((byte) 5, true, HeartbeatPacket.class),
	PLAYER_SHOOT   ((byte) 6, true, PlayerShootPacket.class);


	private static Map<Byte, Tuple<Class<? extends Packet>, PacketType>> map = new HashMap<>();

	private byte packetId;
	private boolean isServer;
	private Class<? extends Packet> packetClass;

	static {
		for (PacketType et : PacketType.values()) {
			map.put(et.getPacketId(), new Tuple<Class<? extends Packet>, PacketType>(et.getPacketClass(), et));
		}
	}

	private PacketType(byte entityTypeId, boolean isServer, Class<? extends Packet> packetClass) {
		this.packetId = entityTypeId;
		this.isServer = isServer;
		this.packetClass = packetClass;
	}

	public byte getPacketId() {
		return this.packetId;
	}

	public boolean getIsServer() {
		return this.isServer;
	}

	public Class<? extends Packet> getPacketClass() {
		return this.packetClass;
	}

	public static Tuple<Class<? extends Packet>, PacketType> valueOf(byte value) {
		return map.get(Byte.valueOf(value));
	}

	public static Tuple<Class<? extends Packet>, PacketType> valueOf(int value) {
		return map.get(Byte.valueOf((byte) value));
	}

	public static boolean isServerPacket(Packet packet) {
		return isServerPacket(packet.getId());
	}

	public static boolean isServerPacket(byte packetId) {
		return map.get(packetId).getY().getIsServer();
	}
}
