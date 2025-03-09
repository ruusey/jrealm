package com.jrealm.net.client.packet;

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
	public void readData(byte[] data) throws Exception {
		final UpdatePacket readPacket = IOService.readPacket(this.getClass(), data);
		this.playerId = readPacket.getPlayerId();
		this.playerName = readPacket.getPlayerName();
		this.stats = readPacket.getStats();
		this.health = readPacket.getHealth();
		this.mana = readPacket.getMana();
		this.experience = readPacket.getExperience();
		this.inventory = readPacket.getInventory();
		this.effectIds = readPacket.getEffectIds();
		this.effectTimes = readPacket.getEffectTimes();
	}

	@Override
	public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
	}

	
	public static UpdatePacket from(Enemy enemy) throws Exception {
		if (enemy == null)
			return null;
		final List<NetGameItem> lootToDrop = Arrays
				.asList(IOService.mapModel(GameDataManager.GAME_ITEMS.get(48), NetGameItem.class));

		final UpdatePacket updatePacket = new UpdatePacket();
		updatePacket.setPlayerId(enemy.getId());
		updatePacket.setHealth(enemy.getHealth());
		updatePacket.setMana(enemy.getMana());
		updatePacket.setPlayerName("enemy[" + enemy.getId() + "]");
		updatePacket.setStats(IOService.mapModel(enemy.getStats(), NetStats.class));
		updatePacket.setInventory(lootToDrop.toArray(new NetGameItem[0]));
		updatePacket.setEffectTimes(enemy.getEffectTimes().clone());
		updatePacket.setEffectIds(enemy.getEffectIds().clone());
		updatePacket.setId(PacketType.UPDATE.getPacketId());
		updatePacket.setExperience(1l);
		return updatePacket;
	}

	public static UpdatePacket from(Player player) throws Exception {
		if (player == null)
			return null;

		final UpdatePacket updatePacket = new UpdatePacket();
		updatePacket.setPlayerId(player.getId());
		updatePacket.setHealth(player.getHealth());
		updatePacket.setMana(player.getMana());
		updatePacket.setPlayerName(player.getName());
		updatePacket.setStats(IOService.mapModel(player.getStats(), NetStats.class));
		updatePacket.setInventory(IOService.mapModel(player.getInventory(), NetGameItem[].class));
		updatePacket.setEffectTimes(player.getEffectTimes().clone());
		updatePacket.setEffectIds(player.getEffectIds().clone());
		updatePacket.setExperience(player.getExperience());
		updatePacket.setId(PacketType.UPDATE.getPacketId());
		return updatePacket;
	}

	public boolean equals(UpdatePacket other, boolean thinMatch) {
		if(other==null) return false;
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
