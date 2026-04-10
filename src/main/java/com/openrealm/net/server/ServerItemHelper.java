package com.openrealm.net.server;

import com.openrealm.game.contants.CharacterClass;
import com.openrealm.game.contants.LootTier;
import com.openrealm.game.entity.Player;
import com.openrealm.game.entity.item.GameItem;
import com.openrealm.game.entity.item.LootContainer;
import com.openrealm.game.entity.item.Stats;
import com.openrealm.game.script.item.UseableItemScriptBase;
import com.openrealm.net.Packet;
import com.openrealm.net.realm.Realm;
import com.openrealm.net.realm.RealmManagerServer;
import com.openrealm.net.server.packet.MoveItemPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerItemHelper {
    public static void handleMoveItemPacket(RealmManagerServer mgr, Packet packet) throws Exception {
        final MoveItemPacket moveItemPacket = (MoveItemPacket) packet;
        ServerItemHelper.log.info("[ItemMoveHelper] Recieved MoveItem Packet from player {}", moveItemPacket.getPlayerId());

        final Realm realm = mgr.findPlayerRealm(moveItemPacket.getPlayerId());
        final Player player = realm.getPlayer(moveItemPacket.getPlayerId());

        // Check for consumable item scripts
        if (moveItemPacket.isConsume()) {
            GameItem targetItem = player.getInventory()[moveItemPacket.getFromSlotIndex()];
            if (targetItem == null) return;
            final UseableItemScriptBase script = mgr.getItemScript(targetItem.getItemId());
            if (script != null) {
                log.info("[ItemMoveHelper] Invoking usable item script for game item {}, player {}", targetItem, player);
                script.invokeUseItem(realm, player, player.getInventory()[moveItemPacket.getFromSlotIndex()]);
                return;
            }
        }

        // Resolve source item
        final GameItem currentEquip = moveItemPacket.getTargetSlotIndex() == -1 ? null
                : player.getInventory()[moveItemPacket.getTargetSlotIndex()];
        GameItem from = null;
        if (MoveItemPacket.isInv1(moveItemPacket.getFromSlotIndex())
                || MoveItemPacket.isEquipment(moveItemPacket.getFromSlotIndex())) {
            from = player.getInventory()[moveItemPacket.getFromSlotIndex()];
        } else if (MoveItemPacket.isGroundLoot(moveItemPacket.getFromSlotIndex())) {
            LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(), 32);
            if (nearLoot != null) {
                int lootIdx = moveItemPacket.getFromSlotIndex() - 20;
                if (lootIdx >= 0 && lootIdx < nearLoot.getItems().length) {
                    from = nearLoot.getItems()[lootIdx];
                }
            }
        }

        // Drop item from inventory to ground
        if ((from != null) && moveItemPacket.isDrop()) {
            final LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(), 32);
            if (nearLoot == null) {
                realm.addLootContainer(new LootContainer(LootTier.BROWN, player.getPos().clone(), from.clone()));
                player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
            } else if (nearLoot.getFirstNullIdx() > -1) {
                nearLoot.setItem(nearLoot.getFirstNullIdx(), from.clone());
                player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
            }

        // Consume item (potions, food)
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

        // Equip: inventory (4-11) → equipment (0-3)
        } else if (MoveItemPacket.isInv1(moveItemPacket.getFromSlotIndex())
                && MoveItemPacket.isEquipment(moveItemPacket.getTargetSlotIndex()) && (from != null)) {
            // Consumable items (potions, food) cannot be equipped
            if (from.isConsumable()) {
                ServerItemHelper.log.warn("Player {} attempted to equip a consumable item {}", player.getId(), from.getName());
                return;
            }
            // Item must be designed for this equipment slot (or auto-assign with targetSlot=-1)
            if (from.getTargetSlot() >= 0 && from.getTargetSlot() != moveItemPacket.getTargetSlotIndex()) {
                ServerItemHelper.log.warn("Player {} attempted to equip item {} (targetSlot={}) in slot {}",
                        player.getId(), from.getName(), from.getTargetSlot(), moveItemPacket.getTargetSlotIndex());
                return;
            }
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

        // Swap within inventory (4-11)
        } else if (MoveItemPacket.isInv1(moveItemPacket.getFromSlotIndex())
                && MoveItemPacket.isInv1(moveItemPacket.getTargetSlotIndex())) {
            GameItem to = player.getInventory()[moveItemPacket.getTargetSlotIndex()];
            if (to == null) {
                player.getInventory()[moveItemPacket.getTargetSlotIndex()] = from.clone();
                player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
            } else {
                GameItem fromClone = from.clone();
                player.getInventory()[moveItemPacket.getFromSlotIndex()] = to.clone();
                player.getInventory()[moveItemPacket.getTargetSlotIndex()] = fromClone;
            }

        // Unequip: equipment (0-3) → inventory (4-11)
        } else if (MoveItemPacket.isEquipment(moveItemPacket.getFromSlotIndex())
                && MoveItemPacket.isInv1(moveItemPacket.getTargetSlotIndex()) && (from != null)) {
            if (currentEquip != null) {
                player.getInventory()[moveItemPacket.getFromSlotIndex()] = currentEquip.clone();
            } else {
                player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
            }
            player.getInventory()[moveItemPacket.getTargetSlotIndex()] = from.clone();

        // Ground loot pickup (fromSlot 20-27)
        } else if (MoveItemPacket.isGroundLoot(moveItemPacket.getFromSlotIndex())) {
            final LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(),
                    (int)(player.getSize() * 0.75));
            if (nearLoot == null) return;

            int lootIdx = moveItemPacket.getFromSlotIndex() - 20;
            if (lootIdx < 0 || lootIdx >= nearLoot.getItems().length) return;

            final GameItem lootItem = nearLoot.getItems()[lootIdx];
            if (lootItem == null) return;

            int emptySlot = player.firstEmptyInvSlot();
            if (emptySlot < 0) {
                ServerItemHelper.log.warn("Player {} inventory full, cannot pick up item", player.getId());
                return;
            }

            player.getInventory()[emptySlot] = lootItem.clone();
            nearLoot.setItem(lootIdx, null);
            // Re-pack items to fill the gap left by the picked-up item
            nearLoot.repackItems();
            // Flag container as changed so the next LoadPacket broadcasts the update
            nearLoot.setContentsChanged(true);
        }
    }
}
