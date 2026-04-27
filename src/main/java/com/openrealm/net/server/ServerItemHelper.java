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

        final int fromIdx = moveItemPacket.getFromSlotIndex();
        final int targetIdx = moveItemPacket.getTargetSlotIndex();

        // Consume HP/MP potion from potion storage (virtual slots 28/29)
        if (moveItemPacket.isConsume()) {
            if (fromIdx == MoveItemPacket.HP_POTION_SLOT) {
                player.consumeHpPotion();
                return;
            }
            if (fromIdx == MoveItemPacket.MP_POTION_SLOT) {
                player.consumeMpPotion();
                return;
            }
        }

        final boolean fromIsGroundLoot = MoveItemPacket.isGroundLoot(fromIdx);
        final boolean fromIsInventory = MoveItemPacket.isInventory(fromIdx) || MoveItemPacket.isEquipment(fromIdx);

        // Reject any fromSlot that isn't a known valid range
        if (!fromIsInventory && !fromIsGroundLoot) {
            ServerItemHelper.log.warn("Player {} sent invalid from slot index {}", player.getId(), fromIdx);
            return;
        }

        // Reject any targetSlot that isn't -1 (drop) or a valid inventory/equipment slot
        if (targetIdx != -1 && !MoveItemPacket.isEquipment(targetIdx) && !MoveItemPacket.isInventory(targetIdx)) {
            ServerItemHelper.log.warn("Player {} sent invalid target slot index {}", player.getId(), targetIdx);
            return;
        }

        // Check for consumable item scripts (inventory only, not ground loot)
        if (moveItemPacket.isConsume() && fromIsInventory) {
            GameItem targetItem = player.getInventory()[fromIdx];
            if (targetItem == null) return;
            final UseableItemScriptBase script = mgr.getItemScript(targetItem.getItemId());
            if (script != null) {
                log.info("[ItemMoveHelper] Invoking usable item script for game item {}, player {}", targetItem, player);
                script.invokeUseItem(realm, player, player.getInventory()[fromIdx]);
                return;
            }
        }

        // Resolve source item
        final GameItem currentEquip = targetIdx == -1 ? null
                : player.getInventory()[targetIdx];
        GameItem from = null;
        if (fromIsInventory) {
            from = player.getInventory()[fromIdx];
        } else if (fromIsGroundLoot) {
            LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(), 32);
            if (nearLoot != null) {
                int lootIdx = fromIdx - 20;
                if (lootIdx >= 0 && lootIdx < nearLoot.getItems().length) {
                    from = nearLoot.getItems()[lootIdx];
                }
            }
        }

        // Drop item from inventory/equipment to ground (ground loot items are already on the ground)
        if ((from != null) && moveItemPacket.isDrop() && fromIsInventory) {
            final LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(), 32);
            if (nearLoot == null) {
                realm.addLootContainer(new LootContainer(LootTier.BROWN, player.getPos().clone(), from.clone()));
                player.getInventory()[fromIdx] = null;
            } else if (nearLoot.getFirstNullIdx() > -1) {
                nearLoot.setItem(nearLoot.getFirstNullIdx(), from.clone());
                player.getInventory()[fromIdx] = null;
            }

        // Consume item (potions, food)
        } else if ((from != null) && from.isConsumable() && moveItemPacket.isConsume() && player.canConsume(from)
                && !MoveItemPacket.isGroundLoot(fromIdx)) {
            final Stats newStats = player.getStats().concat(from.getStats());
            player.setStats(newStats);
            if (from.getStats().getHp() > 0) {
                player.drinkHp();
            } else if (from.getStats().getMp() > 0) {
                player.drinkMp();
            }
            player.getInventory()[fromIdx] = null;

        // Equip: inventory (4-19) → equipment (0-3)
        } else if (MoveItemPacket.isInventory(fromIdx)
                && MoveItemPacket.isEquipment(targetIdx) && (from != null)) {
            // Consumable items (potions, food) cannot be equipped
            if (from.isConsumable()) {
                ServerItemHelper.log.warn("Player {} attempted to equip a consumable item {}", player.getId(), from.getName());
                return;
            }
            // Item must be designed for this equipment slot (or auto-assign with targetSlot=-1)
            if (from.getTargetSlot() >= 0 && from.getTargetSlot() != targetIdx) {
                ServerItemHelper.log.warn("Player {} attempted to equip item {} (targetSlot={}) in slot {}",
                        player.getId(), from.getName(), from.getTargetSlot(), targetIdx);
                return;
            }
            if (!CharacterClass.isValidUser(player, from.getTargetClass())) {
                ServerItemHelper.log.warn("Player {} attempted to equip an item not useable by their class",
                        player.getId());
                return;
            }
            if (currentEquip != null) {
                player.getInventory()[fromIdx] = currentEquip.clone();
            } else {
                player.getInventory()[fromIdx] = null;
            }
            player.getInventory()[targetIdx] = from.clone();

        // Swap within inventory (4-19, including cross-bag)
        } else if (MoveItemPacket.isInventory(fromIdx)
                && MoveItemPacket.isInventory(targetIdx)) {
            GameItem to = player.getInventory()[targetIdx];
            if (to == null) {
                player.getInventory()[targetIdx] = from.clone();
                player.getInventory()[fromIdx] = null;
            } else {
                GameItem fromClone = from.clone();
                player.getInventory()[fromIdx] = to.clone();
                player.getInventory()[targetIdx] = fromClone;
            }

        // Unequip: equipment (0-3) → inventory (4-19)
        } else if (MoveItemPacket.isEquipment(fromIdx)
                && MoveItemPacket.isInventory(targetIdx) && (from != null)) {
            if (currentEquip != null) {
                player.getInventory()[fromIdx] = currentEquip.clone();
            } else {
                player.getInventory()[fromIdx] = null;
            }
            player.getInventory()[targetIdx] = from.clone();

        // Ground loot pickup (fromSlot 20-27)
        } else if (MoveItemPacket.isGroundLoot(fromIdx)) {
            final LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(),
                    (int)(player.getSize() * 0.75));
            if (nearLoot == null) return;

            int lootIdx = fromIdx - 20;
            if (lootIdx < 0 || lootIdx >= nearLoot.getItems().length) return;

            final GameItem lootItem = nearLoot.getItems()[lootIdx];
            if (lootItem == null) return;

            // Intercept consumable HP/MP potions — route to potion storage, not inventory
            if (lootItem.getItemId() == Player.HP_POTION_ITEM_ID) {
                if (!player.addHpPotion()) {
                    ServerItemHelper.log.info("Player {} HP potion storage full (max {})", player.getId(), Player.MAX_CONSUMABLE_POTIONS);
                    return;
                }
                nearLoot.setItem(lootIdx, null);
                nearLoot.repackItems();
                nearLoot.setContentsChanged(true);
                return;
            }
            if (lootItem.getItemId() == Player.MP_POTION_ITEM_ID) {
                if (!player.addMpPotion()) {
                    ServerItemHelper.log.info("Player {} MP potion storage full (max {})", player.getId(), Player.MAX_CONSUMABLE_POTIONS);
                    return;
                }
                nearLoot.setItem(lootIdx, null);
                nearLoot.repackItems();
                nearLoot.setContentsChanged(true);
                return;
            }

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
