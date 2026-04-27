package com.openrealm.net.client.packet;

import java.util.ArrayList;
import java.util.List;

import com.openrealm.game.entity.Player;
import com.openrealm.game.entity.item.Enchantment;
import com.openrealm.game.entity.item.GameItem;
import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.IOService;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.nettypes.SerializableBoolean;
import com.openrealm.net.entity.NetEnchantment;
import com.openrealm.net.entity.NetGameItem;
import com.openrealm.net.entity.NetPlayer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Streamable
@PacketId(packetId=(byte)17)
public class AcceptTradeRequestPacket extends Packet{
	
	@SerializableField(order = 0, type = SerializableBoolean.class)
	private boolean accepted;
	@SerializableField(order = 1, type = NetPlayer.class)
	private NetPlayer player0;
	@SerializableField(order = 2, type = NetPlayer.class)
	private NetPlayer player1;
	@SerializableField(order = 3, type = NetGameItem.class, isCollection = true)
	private NetGameItem[] player0Inv;
	@SerializableField(order = 4, type = NetGameItem.class, isCollection = true)
	private NetGameItem[] player1Inv;
	
	public AcceptTradeRequestPacket(boolean accepted, Player p0, Player p1) {
		this.accepted = accepted;
		this.player0 = (IOService.mapModel(p0, NetPlayer.class));
		this.player1 = (IOService.mapModel(p1, NetPlayer.class));
		// Build inventories explicitly so trade visibility includes enchantments
		// and stack counts even if ModelMapper drops nested generic fields.
		this.player0Inv = inventoryToNet(p0.getInventory());
		this.player1Inv = inventoryToNet(p1.getInventory());
	}

	private static NetGameItem[] inventoryToNet(GameItem[] inventory) {
		if (inventory == null) return new NetGameItem[0];
		final NetGameItem[] out = new NetGameItem[inventory.length];
		for (int i = 0; i < inventory.length; i++) {
			out[i] = toNetGameItem(inventory[i]);
		}
		return out;
	}

	private static NetGameItem toNetGameItem(GameItem item) {
		if (item == null) return new NetGameItem();
		// Start from ModelMapper to copy the easy primitive fields & nested Stats/Damage/Effect.
		final NetGameItem net = IOService.mapModel(item, NetGameItem.class);
		// Defensively re-attach forge-related fields and the enchantments list so
		// the receiving client always sees painted pixels and stack counts.
		net.setStackable(item.isStackable());
		net.setMaxStack(item.getMaxStack());
		net.setStackCount(item.getStackCount());
		net.setCategory(item.getCategory());
		net.setForgeStatId(item.getForgeStatId());
		net.setForgeSlotId(item.getForgeSlotId());
		final List<NetEnchantment> ench;
		if (item.getEnchantments() != null && !item.getEnchantments().isEmpty()) {
			ench = new ArrayList<>(item.getEnchantments().size());
			for (Enchantment e : item.getEnchantments()) {
				ench.add(new NetEnchantment(e.getStatId(), e.getDeltaValue(), e.getPixelX(), e.getPixelY(), e.getPixelColor()));
			}
		} else {
			ench = new ArrayList<>();
		}
		net.setEnchantments(ench);
		return net;
	}
}
