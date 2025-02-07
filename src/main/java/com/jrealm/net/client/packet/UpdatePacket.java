package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.List;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.*;
import com.jrealm.net.entity.NetGameItem;
import com.jrealm.net.entity.NetStats;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@Streamable
public class UpdatePacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
	private long playerId;
	@SerializableField(order = 1, type = SerializableString.class)
	private String playerName;
	@SerializableField(order = 2, type = NetStats.class)
	private NetStats stats;
	@SerializableField(order = 3, type = SerializableInt.class)
	private int health;
	@SerializableField(order = 4, type = SerializableInt.class)
	private int mana;
	@SerializableField(order = 5, type = SerializableLong.class)
	private long experience;
	@SerializableField(order = 6, type = NetGameItem.class, isCollection = true)
	private NetGameItem[] inventory;
	@SerializableField(order = 7, type = SerializableShort.class, isCollection = true)
	private Short[] effectIds;
	@SerializableField(order = 8, type = SerializableLong.class, isCollection = true)
	private Long[] effectTimes;

	// TODO: Rewrite this to only include delta data within the character not the
	// entire character
	public UpdatePacket() {

	}

	public UpdatePacket(byte id, byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			e.printStackTrace();
			UpdatePacket.log.error("Failed to build Stats Packet. Reason: {}", e.getMessage());
		}
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		IOService.writePacket(this, stream);
	}

	@Override
	public void readData(byte[] data) throws Exception {
		final ByteArrayInputStream bis = new ByteArrayInputStream(data);
		final DataInputStream dis = new DataInputStream(bis);
		if ((dis == null) || (dis.available() < 5))
			throw new IllegalStateException("No Packet data available to read from DataInputStream");
		final UpdatePacket read = IOService.readPacket(getClass(), dis);
		this.setPlayerId(read.getId());
		this.setHealth(read.getHealth());
		this.setMana(read.getMana());
		this.setPlayerName(read.getPlayerName());
		this.setStats(read.getStats());
		this.setInventory(read.getInventory());
		this.setEffectTimes(read.getEffectTimes());
		this.setEffectIds(read.getEffectIds());
		this.setExperience(read.getExperience());
	}

	public static UpdatePacket from(Enemy enemy) throws Exception {
		if (enemy == null)
			return null;
		final List<NetGameItem> lootToDrop = Arrays
				.asList(IOService.mapModel(GameDataManager.GAME_ITEMS.get(48), NetGameItem.class));

		UpdatePacket test = new UpdatePacket();
		test.setPlayerId(enemy.getId());
		test.setHealth(enemy.getHealth());
		test.setMana(enemy.getMana());
		test.setPlayerName("enemy[" + enemy.getId() + "]");
		test.setStats(IOService.mapModel(enemy.getStats(), NetStats.class));
		test.setInventory(lootToDrop.toArray(new NetGameItem[0]));
		test.setEffectTimes(enemy.getEffectTimes());
		test.setEffectIds(enemy.getEffectIds());
		test.setId(PacketType.UPDATE.getPacketId());
		test.setExperience(1l);
		return test;

	}

	public static UpdatePacket from(Player player) throws Exception {

		if (player == null)
			return null;

		UpdatePacket test = new UpdatePacket();
		test.setPlayerId(player.getId());
		test.setHealth(player.getHealth());
		test.setMana(player.getMana());
		test.setPlayerName(player.getName());
		test.setStats(IOService.mapModel(player.getStats(), NetStats.class));
		test.setInventory(IOService.mapModel(player.getInventory(), NetGameItem[].class));
		test.setEffectTimes(player.getEffectTimes());
		test.setEffectIds(player.getEffectIds());
		test.setExperience(player.getExperience());
		test.setId(PacketType.UPDATE.getPacketId());
		return test;
	}

	public boolean equals(UpdatePacket other, boolean thinMatch) {
		boolean basic = (this.playerId == other.getPlayerId()) && this.playerName.equals(other.getPlayerName())
				&& (this.health == other.getHealth()) && (this.mana == other.getMana());

		boolean stats = this.stats.equals(other.getStats());
		if (thinMatch)
			stats = true;
		boolean inv = true;
		for (int i = 0; i < this.inventory.length; i++) {
			if ((this.inventory[i] != null) && (other.getInventory()[i] != null)) {
				if (this.inventory[i].equals(other.getInventory()[i])) {
					continue;
				}
				inv = false;
				break;
			}
			if ((this.inventory[i] == null) && (other.getInventory()[i] == null)) {
				continue;
			}
			inv = false;
			break;
		}
		if (thinMatch)
			inv = true;

		boolean effects = true;
		for (int i = 0; i < this.effectIds.length; i++) {
			if ((this.effectIds[i] != other.getEffectIds()[i]) || (this.effectTimes[i] != other.getEffectTimes()[i])) {
				effects = false;
				break;
			}
		}
		if (thinMatch) {
			inv = true;
			effects = true;
		}
		
		boolean expEqual = this.experience == other.getExperience();
		boolean result = basic && stats && inv && effects && expEqual;

		return result;
	}

	public boolean equals(UpdatePacket other) {
		boolean basic = (this.playerId == other.getPlayerId()) && this.playerName.equals(other.getPlayerName())
				&& (this.health == other.getHealth()) && (this.mana == other.getMana());

		boolean stats = this.stats.equals(other.getStats());
		if (inventory.length == 1) {
			// System.out.println("");
		}
		boolean inv = true;
		for (int i = 0; i < this.inventory.length; i++) {
			if ((this.inventory[i] != null) && (other.getInventory()[i] != null)) {
				if (this.inventory[i].equals(other.getInventory()[i])) {
					continue;
				}
				inv = false;
				break;
			}
			if ((this.inventory[i] == null) && (other.getInventory()[i] == null)) {
				continue;
			}
			inv = false;
			break;
		}

		boolean effects = true;
		for (int i = 0; i < this.effectIds.length; i++) {
			if ((this.effectIds[i] != other.getEffectIds()[i]) || (this.effectTimes[i] != other.getEffectTimes()[i])) {
				effects = false;
				break;
			}
		}
		boolean expEqual = this.experience == other.getExperience();
		boolean result = basic && stats && inv && effects && expEqual;
		return result;
	}
}
