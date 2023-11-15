package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jrealm.game.entity.Player;
import com.jrealm.game.messaging.CommandType;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;


@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class CommandPacket extends Packet {
	private long playerId;
	private byte commandId;
	private String command;
	
	public CommandPacket() {
		
	}
	
	public <T> T messageAs(Class<T> type) throws Exception{
		ObjectMapper m = new ObjectMapper();
		return m.readValue(this.command, type);
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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeLong(target.getId());
		dos.writeByte(commandId);
		dos.writeUTF(command);

		return new CommandPacket(PacketType.COMMAND.getPacketId(), baos.toByteArray());
	}
	
	public static CommandPacket create(Player target, CommandType type, Object command) {
		CommandPacket created = null;
		try {
			created = from(target, type.getCommandId(), new ObjectMapper().writeValueAsString(command));
		}catch(Exception e) {
			log.error("Failed to create Command Packet. Reason: {}", e);
		}
		return created;
	}
}
