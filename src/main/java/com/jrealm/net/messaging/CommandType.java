package com.jrealm.net.messaging;

import java.util.HashMap;
import java.util.Map;

import com.jrealm.net.server.packet.CommandPacket;

public enum CommandType {
    LOGIN_REQUEST((byte) 1, LoginRequestMessage.class), LOGIN_RESPONSE((byte) 2, LoginResponseMessage.class),
    SERVER_COMMAND((byte) 3, ServerCommandMessage.class), SERVER_ERROR((byte) 4, ServerErrorMessage.class),
    PLAYER_ACCOUNT((byte) 5, PlayerAccountMessage.class);

    private byte commandId;
    private Class<?> commandClass;

    private static Map<Byte, Class<?>> map = new HashMap<>();

    static {
        for (CommandType et : CommandType.values()) {
            CommandType.map.put(et.getCommandId(), et.getCommandClass());
        }
    }

    private CommandType(byte commandId, Class<?> packetClass) {
        this.commandId = commandId;
        this.commandClass = packetClass;
    }

    public byte getCommandId() {
        return this.commandId;
    }

    public Class<?> getCommandClass() {
        return this.commandClass;
    }

    public static Class<?> valueOf(byte value) {
        return CommandType.map.get(Byte.valueOf(value));
    }

    public static Class<?> valueOf(int value) {
        return CommandType.map.get(Byte.valueOf((byte) value));
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromPacket(CommandPacket packet) throws Exception {
        Class<T> target = (Class<T>) CommandType.valueOf(packet.getCommandId());
        return packet.messageAs(target);
    }
}
