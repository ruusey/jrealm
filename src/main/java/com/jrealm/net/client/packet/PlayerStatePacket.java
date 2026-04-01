package com.jrealm.net.client.packet;

import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.PacketId;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableShort;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Lightweight packet for frequently-changing player/enemy state:
 * HP, MP, and active status effects. Sent at higher frequency than
 * the full UpdatePacket which carries inventory/stats/XP.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Streamable
@NoArgsConstructor
@PacketId(packetId = (byte) 24)
public class PlayerStatePacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
	private long playerId;
	@SerializableField(order = 1, type = SerializableInt.class)
	private int health;
	@SerializableField(order = 2, type = SerializableInt.class)
	private int mana;
	@SerializableField(order = 3, type = SerializableShort.class, isCollection = true)
	private Short[] effectIds;
	@SerializableField(order = 4, type = SerializableLong.class, isCollection = true)
	private Long[] effectTimes;

	public static PlayerStatePacket from(Player player) {
		final PlayerStatePacket packet = new PlayerStatePacket();
		packet.setPlayerId(player.getId());
		packet.setHealth(player.getHealth());
		packet.setMana(player.getMana());
		packet.setEffectIds(player.getEffectIds().clone());
		packet.setEffectTimes(player.getEffectTimes().clone());
		return packet;
	}

	public static PlayerStatePacket from(Enemy enemy) {
		final PlayerStatePacket packet = new PlayerStatePacket();
		packet.setPlayerId(enemy.getId());
		packet.setHealth(enemy.getHealth());
		packet.setMana(enemy.getMana());
		packet.setEffectIds(enemy.getEffectIds().clone());
		packet.setEffectTimes(enemy.getEffectTimes().clone());
		return packet;
	}

	/**
	 * Compare only the fields that matter for delta detection.
	 * Effect durations are ignored (they tick every frame).
	 */
	public boolean equalsState(PlayerStatePacket other) {
		if (other == null) return false;
		if (this.playerId != other.playerId) return false;
		if (this.health != other.health) return false;
		if (this.mana != other.mana) return false;
		if (this.effectIds.length != other.effectIds.length) return false;
		for (int i = 0; i < this.effectIds.length; i++) {
			if (!this.effectIds[i].equals(other.effectIds[i])) return false;
		}
		return true;
	}
}
