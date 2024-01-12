package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

import com.jrealm.game.util.Tuple;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.PlayerDeathPacket;
import com.jrealm.net.client.packet.TextEffectPacket;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.MoveItemPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.net.server.packet.UseAbilityPacket;
import com.jrealm.net.server.packet.UsePortalPacket;

public enum PacketType {
	PLAYER_MOVE    ((byte) 1, PlayerMovePacket.class),
	UPDATE         ((byte) 2, UpdatePacket.class),
	OBJECT_MOVE    ((byte) 3, ObjectMovePacket.class),
	TEXT           ((byte) 4, TextPacket.class),
	HEARTBEAT      ((byte) 5, HeartbeatPacket.class),
	PLAYER_SHOOT   ((byte) 6, PlayerShootPacket.class),
	COMMAND		   ((byte) 7, CommandPacket.class),
	LOAD_MAP	   ((byte) 8, LoadMapPacket.class),
	LOAD	   	   ((byte) 9, LoadPacket.class),
	UNLOAD         ((byte) 10, UnloadPacket.class),
	USE_ABILITY    ((byte) 11, UseAbilityPacket.class),
	MOVE_ITEM      ((byte) 12, MoveItemPacket.class),
	USE_PORTAL     ((byte) 13, UsePortalPacket.class),
	TEXT_EFFECT    ((byte) 14, TextEffectPacket.class),
	PLAYER_DEATH   ((byte) 15, PlayerDeathPacket.class);

	private static Map<Byte, Tuple<Class<? extends Packet>, PacketType>> map = new HashMap<>();

	private byte packetId;
	private Class<? extends Packet> packetClass;

	static {
		for (PacketType et : PacketType.values()) {
			PacketType.map.put(et.getPacketId(), new Tuple<Class<? extends Packet>, PacketType>(et.getPacketClass(), et));
		}
	}

	private PacketType(byte entityTypeId, Class<? extends Packet> packetClass) {
		this.packetId = entityTypeId;
		this.packetClass = packetClass;
	}

	public byte getPacketId() {
		return this.packetId;
	}

	public Class<? extends Packet> getPacketClass() {
		return this.packetClass;
	}

	public static Tuple<Class<? extends Packet>, PacketType> valueOf(byte value) {
		return PacketType.map.get(Byte.valueOf(value));
	}

	public static Tuple<Class<? extends Packet>, PacketType> valueOf(int value) {
		return PacketType.map.get(Byte.valueOf((byte) value));
	}
}
