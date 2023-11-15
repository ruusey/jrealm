package com.jrealm.game.messaging;

import java.util.HashMap;
import java.util.Map;

import com.jrealm.net.server.packet.CommandPacket;

public enum CommandType {
	LOGIN_REQUEST  ((byte) 1, LoginRequestMessage.class),
	LOGIN_RESPONSE ((byte) 2, LoginRequestMessage.class);
	
	private byte commandId;
	private Class<?> commandClass;
	
	private static Map<Byte, Class<?>> map = new HashMap<>();

	static {
		for (CommandType et : CommandType.values()) {
			map.put(et.getCommandId(), et.getCommandClass());
		}
	}

	private CommandType(byte commandId, Class<?> packetClass) {
		this.commandId = commandId;
		this.commandClass = packetClass;
	}
	
	public byte getCommandId() {
		return this.commandId;
	}
	
	public Class<?> getCommandClass(){
		return this.commandClass;
	}
	public static Class<?> valueOf(byte value) {
		return map.get(Byte.valueOf(value));
	}

	public static Class<?> valueOf(int value) {
		return map.get(Byte.valueOf((byte) value));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T fromPacket(CommandPacket packet) throws Exception{
		Class<T> target = (Class<T>)valueOf(packet.getCommandId());
		return packet.messageAs(target);
	}
}
