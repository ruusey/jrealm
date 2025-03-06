package com.jrealm.net.server.packet;

import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableFloat;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Streamable
@AllArgsConstructor
public class UseAbilityPacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
	private long playerId;
	@SerializableField(order = 1, type = SerializableFloat.class)
	private float posX;
	@SerializableField(order = 2, type = SerializableFloat.class)
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
		final UseAbilityPacket packet = IOService.readPacket(this.getClass(), data);
		this.playerId = packet.getPlayerId();
		this.posX = packet.getPosX();
		this.posY = packet.getPosY();
	}

	@Override
	public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
	}

	public static UseAbilityPacket from(Player player, Vector2f pos) throws Exception {
		final UseAbilityPacket packet = new UseAbilityPacket(player.getId(), pos.x, pos.y);
		packet.setId(PacketType.USE_ABILITY.getPacketId());
		return packet;
	}
}
