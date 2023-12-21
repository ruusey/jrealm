package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@Slf4j
public class UsePortalPacket extends Packet {
	private long portalId;
	private long fromRealmId;
	private long playerId;

	public UsePortalPacket() {

	}

	public UsePortalPacket(final byte id, final byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			UsePortalPacket.log.error("Failed to parse UsePortal packet, Reason: {}", e);
		}
	}

	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if ((dis == null) || (dis.available() < 5))
			throw new IllegalStateException("No Packet data available to read from DataInputStream");

		this.portalId = dis.readLong();
		this.fromRealmId = dis.readLong();
		this.playerId = dis.readLong();
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if ((this.getId() < 1) || (this.getData() == null) || (this.getData().length < 5))
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");
		this.addHeader(stream);
		stream.writeLong(this.portalId);
		stream.writeLong(this.fromRealmId);
		stream.writeLong(this.playerId);
	}

	public static UsePortalPacket from(long portalId, long fromRealmId, long playerId) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeLong(portalId);
		dos.writeLong(fromRealmId);
		dos.writeLong(playerId);
		return new UsePortalPacket(PacketType.USE_PORTAL.getPacketId(), baos.toByteArray());
	}
}
