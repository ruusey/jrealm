package com.jrealm.net.server.packet;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Player;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableString;
import com.jrealm.net.messaging.CommandType;
import com.jrealm.net.messaging.ServerErrorMessage;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Streamable
@NoArgsConstructor
public class CommandPacket extends Packet {
	
	@SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;
	@SerializableField(order = 1, type = SerializableByte.class)
    private byte commandId;
	@SerializableField(order = 2, type = SerializableString.class)
    private String command;

    public <T> T messageAs(Class<T> type) throws Exception {
        return GameDataManager.JSON_MAPPER.readValue(this.command, type);
    }

    public static CommandPacket from(Player target, byte commandId, String command) throws Exception {
    	final CommandPacket com = new CommandPacket();
    	com.setPlayerId(target.getId());
    	com.setCommandId(commandId);
    	com.setCommand(command);
        return com;
    }

    public static CommandPacket from(long targetEntity, byte commandId, String command) throws Exception {
    	final CommandPacket com = new CommandPacket();
    	com.setPlayerId(targetEntity);
    	com.setCommandId(commandId);
    	com.setCommand(command);
        return com;
    }

    public static CommandPacket from(CommandType cmd, Object command) throws Exception {
    	final CommandPacket com = new CommandPacket();
    	com.setPlayerId(-1l);
    	com.setCommandId(cmd.getCommandId());
    	com.setCommand(GameDataManager.JSON_MAPPER.writeValueAsString(command));
        return com;
    }

    public static CommandPacket create(Player target, CommandType type, Object command) {
        return create(target.getId(), type, command);
    }

    public static CommandPacket create(long targetEntityId, CommandType type, Object command) {
        CommandPacket created = null;
        try {
            created = from(targetEntityId, type.getCommandId(), GameDataManager.JSON_MAPPER.writeValueAsString(command));
        } catch (Exception e) {
            log.error("Failed to create Command Packet. Reason: {}", e);
        }
        return created;
    }

    public static CommandPacket createError(final Player target, final int code, final String message) {
        return createError(target.getId(), code, message);
    }

    public static CommandPacket createError(final long targetEntityId, final int code, final String message) {
        final ServerErrorMessage error = ServerErrorMessage.from(code, message);
        return CommandPacket.create(targetEntityId, CommandType.SERVER_ERROR, error);
    }

	@Override
	public byte getPacketId() {
		return (byte) 7;
	}
}
