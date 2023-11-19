package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.Player;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class LoadMapPacket extends Packet {

	private long playerId;
	private String mapKey;

	public LoadMapPacket() {

	}

	public LoadMapPacket(final byte id, final byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			log.error("Failed to parse ObjectMove packet, Reason: {}", e);
		}
	}

	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if (dis == null || dis.available() < 5)
			throw new IllegalStateException("No Packet data available to read from DataInputStream");

		this.playerId = dis.readLong();
		this.mapKey = dis.readUTF();

	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");

		this.addHeader(stream);
		stream.writeLong(this.playerId);
		stream.writeUTF(this.mapKey);
	}

	public static LoadMapPacket from(Player player, String mapKey) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		DataOutputStream stream = new DataOutputStream(byteStream);

		stream.writeLong(player.getId());
		stream.writeUTF(mapKey);

		return new LoadMapPacket(PacketType.LOAD_MAP.getPacketId(), byteStream.toByteArray());
	}
}
