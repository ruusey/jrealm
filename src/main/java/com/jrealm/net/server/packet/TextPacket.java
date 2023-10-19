package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class TextPacket extends Packet {
	private String from;
	private String to;
	private String message;

	public TextPacket() {

	}

	public TextPacket(byte packetId, byte[] data) {
		super(packetId, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			log.error("Failed to create Text Packet. Reason: {}", e);
		}
	}

	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if (dis == null || dis.available() < 5)
			throw new IllegalStateException("No Packet data available to read from DataInputStream");
		this.from = dis.readUTF();
		this.to = dis.readUTF();
		this.message = dis.readUTF();
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");
		this.addHeader(stream);
		stream.writeUTF(this.from);
		stream.writeUTF(this.to);
		stream.writeUTF(this.message);
	}

	public TextPacket from(String from, String to, String text) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF(from);
		dos.writeUTF(to);
		dos.writeUTF(text);

		return new TextPacket(PacketType.TEXT.getPacketId(), baos.toByteArray());
	}
	
	public static TextPacket create(String from, String to, String text) {
		TextPacket created = null;
		try {
			created = new TextPacket().from(from, to, text);
		}catch(Exception e) {
			log.error("Failed to create Text Packet. Reason: {}", e);
		}
		return created;
	}
}
