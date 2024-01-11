package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class UseAbilityPacket extends Packet {
	private long playerId;
	private float posX;
	private float posY;
	public UseAbilityPacket() {

	}

	public UseAbilityPacket(byte packetId, byte[] data) {
		super(packetId, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			UseAbilityPacket.log.error("Failed to create Use Ability Packet. Reason: {}", e);
		}
	}

	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if ((dis == null) || (dis.available() < 5))
			throw new IllegalStateException("No Packet data available to read from DataInputStream");
		this.playerId = dis.readLong();
		this.posX = dis.readFloat();
		this.posY = dis.readFloat();
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if ((this.getId() < 1) || (this.getData() == null) || (this.getData().length < 5))
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");
		this.addHeader(stream);
		stream.writeLong(this.playerId);
		stream.writeFloat(this.posX);
		stream.writeFloat(this.posY);
	}

	public static UseAbilityPacket from(Player player, Vector2f pos) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream stream = new DataOutputStream(baos);
		stream.writeLong(player.getId());
		stream.writeFloat(pos.x);
		stream.writeFloat(pos.y);
		return new UseAbilityPacket(PacketType.USE_ABILITY.getPacketId(), baos.toByteArray());
	}
}
