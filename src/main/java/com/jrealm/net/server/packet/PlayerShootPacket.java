package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Data
@Slf4j
@EqualsAndHashCode(callSuper=true)
public class PlayerShootPacket extends Packet{
	private long entityId;
	private float destX;
	private float destY;
	
	public PlayerShootPacket() {

	}

	public PlayerShootPacket(final byte id, final byte[] data) {
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
		
		this.entityId = dis.readLong();
		this.destX = dis.readFloat();
		this.destY = dis.readFloat();
	}
	
	
	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");
		this.addHeader(stream);
		stream.writeLong(this.entityId);
		stream.writeFloat(this.destX);
		stream.writeFloat(this.destY);
	}
	
	public static PlayerShootPacket from(long playerId, Vector2f dest) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeLong(playerId);
		dos.writeFloat(dest.x);
		dos.writeFloat(dest.y); 
		return new PlayerShootPacket(PacketType.PLAYER_SHOOT.getPacketId(), baos.toByteArray());
	}
}
