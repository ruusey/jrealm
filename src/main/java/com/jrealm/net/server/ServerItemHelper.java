package com.jrealm.net.server;

import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.LootTier;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.net.Packet;
import com.jrealm.net.server.packet.MoveItemPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerItemHelper {
	// I like spaghetti, what about you?
	public static void handleMoveItemPacket(RealmManagerServer mgr, Packet packet) throws Exception {
		final MoveItemPacket moveItemPacket = (MoveItemPacket) packet;
		ServerItemHelper.log.info("[SERVER] Recieved MoveItem Packet from player {}", moveItemPacket.getPlayerId());

		final Realm realm = mgr.searchRealmsForPlayer(moveItemPacket.getPlayerId());

		final Player player = realm.getPlayer(moveItemPacket.getPlayerId());
		// if moving item from inventory
		final GameItem currentEquip = moveItemPacket.getTargetSlotIndex() == -1 ? null
				: player.getInventory()[moveItemPacket.getTargetSlotIndex()];
		GameItem from = null;
		if (MoveItemPacket.isInv1(moveItemPacket.getFromSlotIndex())
				|| MoveItemPacket.isEquipment(moveItemPacket.getFromSlotIndex())) {
			from = player.getInventory()[moveItemPacket.getFromSlotIndex()];
		} else if (MoveItemPacket.isGroundLoot(moveItemPacket.getFromSlotIndex())) {
			LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(), 32);
			from = nearLoot.getItems()[moveItemPacket.getFromSlotIndex() - 20];
		}
		// If the player requested to drop an item from their inventory
		if ((from != null) && moveItemPacket.isDrop()) {
			final LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(), 32);
			if (nearLoot == null) {
				realm.addLootContainer(new LootContainer(LootTier.BROWN, player.getPos().clone(), from.clone()));
				player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
			} else if (nearLoot.getFirstNullIdx() > -1) {
				nearLoot.setItem(nearLoot.getFirstNullIdx(), from.clone());
				player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
			}
			// If the Item isnt ground loot and is consumable
		} else if ((from != null) && from.isConsumable() && moveItemPacket.isConsume() && player.canConsume(from)
				&& !MoveItemPacket.isGroundLoot(moveItemPacket.getFromSlotIndex())) {

			final Stats newStats = player.getStats().concat(from.getStats());
			player.setStats(newStats);

			if (from.getStats().getHp() > 0) {
				player.drinkHp();
			} else if (from.getStats().getMp() > 0) {
				player.drinkMp();
			}
			player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
			// If an item is being moved from the inventory slots 4-12 to the players
			// equipment slots
		} else if (MoveItemPacket.isInv1(moveItemPacket.getFromSlotIndex())
				&& MoveItemPacket.isEquipment(moveItemPacket.getTargetSlotIndex()) && (from != null)) {
			if (!CharacterClass.isValidUser(player, from.getTargetClass())) {
				ServerItemHelper.log.warn("Player {} attempted to equip an item not useable by their class",
						player.getId());
				return;
			}
			if (currentEquip != null) {
				player.getInventory()[moveItemPacket.getFromSlotIndex()] = currentEquip.clone();
			} else {
				player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
			}
			player.getInventory()[moveItemPacket.getTargetSlotIndex()] = from.clone();
			// If the player is swapping items in their inventory (not impl cuz my client is
			// garabage)
		} else if (MoveItemPacket.isInv1(moveItemPacket.getFromSlotIndex())
				&& MoveItemPacket.isInv1(moveItemPacket.getTargetSlotIndex())) {

			GameItem to = player.getInventory()[moveItemPacket.getTargetSlotIndex()];
			if (to == null) {
				player.getInventory()[moveItemPacket.getTargetSlotIndex()] = from;
			} else {
				GameItem fromClone = from.clone();
				player.getInventory()[moveItemPacket.getFromSlotIndex()] = to;
				player.getInventory()[moveItemPacket.getTargetSlotIndex()] = fromClone;
			}
			// If the player is attempting to pick up ground loot into their inventory
		} else if (MoveItemPacket.isGroundLoot(moveItemPacket.getFromSlotIndex())
				&& MoveItemPacket.isInv1(moveItemPacket.getTargetSlotIndex())) {

			final LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(),
					player.getSize() / 2);
			if (nearLoot == null)
				return;
			final GameItem lootItem = nearLoot.getItems()[moveItemPacket.getFromSlotIndex() - 20];
			final GameItem currentInvItem = player.getInventory()[moveItemPacket.getTargetSlotIndex()];

			if ((lootItem != null) && (currentInvItem == null)) {
				player.getInventory()[player.firstEmptyInvSlot()] = lootItem.clone();
				nearLoot.setItem(moveItemPacket.getFromSlotIndex() - 20, null);
				nearLoot.setItemsUncondensed(LootContainer.getCondensedItems(nearLoot));
			} else if ((lootItem != null) & (currentInvItem != null)) {
				GameItem lootClone = lootItem.clone();
				// GameItem currentInvItemClone = currentInvItem.clone();
				player.getInventory()[player.firstEmptyInvSlot()] = lootClone;
				// nearLoot.setItem(moveItemPacket.getFromSlotIndex()-20, currentInvItemClone);
				nearLoot.setItemsUncondensed(LootContainer.getCondensedItems(nearLoot));
			}
		}
	}
}
