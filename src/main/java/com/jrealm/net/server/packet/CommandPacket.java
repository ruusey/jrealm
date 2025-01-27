package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Player;
import com.jrealm.net.Packet;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableString;
import com.jrealm.net.messaging.CommandType;
import com.jrealm.net.messaging.ServerErrorMessage;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class CommandPacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;
	@SerializableField(order = 1, type = SerializableByte.class)
    private byte commandId;
	@SerializableField(order = 2, type = SerializableString.class)
    private String command;

    public CommandPacket() {

    }

    public <T> T messageAs(Class<T> type) throws Exception {
        return GameDataManager.JSON_MAPPER.readValue(this.command, type);
    }

    public CommandPacket(byte packetId, byte[] data) {
        super(packetId, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            log.error("Failed to create Command Packet. Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);
        if (dis == null || dis.available() < 5)
            throw new IllegalStateException("No Packet data available to read from DataInputStream");
        this.playerId = dis.readLong();
        this.commandId = dis.readByte();
        this.command = dis.readUTF();
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");
        this.addHeader(stream);
        stream.writeLong(this.playerId);
        stream.writeByte(this.commandId);
        stream.writeUTF(this.command);
    }

    public static CommandPacket from(Player target, byte commandId, String command) throws Exception {
    	final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	final DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(target.getId());
        dos.writeByte(commandId);
        dos.writeUTF(command);
        return new CommandPacket(PacketType.COMMAND.getPacketId(), baos.toByteArray());
    }

    public static CommandPacket from(long targetEntity, byte commandId, String command) throws Exception {
    	final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(targetEntity);
        dos.writeByte(commandId);
        dos.writeUTF(command);
        return new CommandPacket(PacketType.COMMAND.getPacketId(), baos.toByteArray());
    }

    public static CommandPacket from(CommandType cmd, Object command) throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(-1l);
        dos.writeByte(cmd.getCommandId());
        dos.writeUTF(GameDataManager.JSON_MAPPER.writeValueAsString(command));
        return new CommandPacket(PacketType.COMMAND.getPacketId(), baos.toByteArray());
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
}
