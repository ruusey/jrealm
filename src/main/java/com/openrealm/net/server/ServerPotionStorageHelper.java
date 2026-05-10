package com.openrealm.net.server;

import java.util.ArrayList;
import java.util.List;

import com.openrealm.game.entity.Player;
import com.openrealm.game.entity.item.AttributeModifier;
import com.openrealm.game.entity.item.Enchantment;
import com.openrealm.game.entity.item.GameItem;
import com.openrealm.game.entity.item.PotionStorage;
import com.openrealm.net.Packet;
import com.openrealm.net.client.packet.OpenPotionStoragePacket;
import com.openrealm.net.client.packet.PotionStorageUpdatePacket;
import com.openrealm.net.client.packet.UpdatePacket;
import com.openrealm.net.core.IOService;
import com.openrealm.net.entity.NetAttributeModifier;
import com.openrealm.net.entity.NetEnchantment;
import com.openrealm.net.entity.NetGameItem;
import com.openrealm.net.realm.Realm;
import com.openrealm.net.realm.RealmManagerServer;
import com.openrealm.net.server.packet.PotionStorageMovePacket;

import lombok.extern.slf4j.Slf4j;

/**
 * Server-side handlers for the per-player Potion Storage container.
 *
 * Behavior parallels the existing vault-chest plumbing but is keyed to a
 * fixed interaction tile (id 328 in vault map 1, row 20 col 16) rather than
 * placed map entities. Containers are 32 slots, restricted to stackable
 * items and gems via {@link PotionStorage#canStore(GameItem)}.
 *
 * Persistence flows through {@link Realm#serializePotionStorageForSave()}
 * just like vault chests; the same chestsLoaded gate prevents wipe-on-error.
 */
@Slf4j
public class ServerPotionStorageHelper {

    /** Open the player's potion-storage container[0], creating one if absent. */
    public static void handleOpen(RealmManagerServer mgr, Player player) {
        try {
            final Realm realm = mgr.findPlayerRealm(player.getId());
            if (realm == null) return;
            final PotionStorage container = ensureContainer(realm, player);
            final NetGameItem[] netItems = toNetItems(container.getItems());
            mgr.enqueueServerPacket(player, new OpenPotionStoragePacket(player.getId(), netItems));
        } catch (Exception e) {
            log.error("[PotionStorage] handleOpen failed for player {}: {}", player.getId(), e.getMessage());
        }
    }

    /** Validate + execute a move/swap/merge between inventory and storage. */
    public static void handleMove(RealmManagerServer mgr, Packet packet) {
        try {
            final PotionStorageMovePacket p = (PotionStorageMovePacket) packet;
            final Realm realm = mgr.findPlayerRealm(p.getPlayerId());
            if (realm == null) return;
            final Player player = realm.getPlayer(p.getPlayerId());
            if (player == null) return;
            final PotionStorage container = ensureContainer(realm, player);

            final byte fromSide = p.getFromSide();
            final byte toSide = p.getToSide();
            final int fromIdx = p.getFromIdx();
            final int toIdx = p.getToIdx();
            if (!isValidIdx(fromSide, fromIdx, player) || !isValidIdx(toSide, toIdx, player)) {
                log.warn("[PotionStorage] Invalid move indices from p{}", player.getId());
                return;
            }
            if (fromSide == toSide && fromIdx == toIdx) return;

            final GameItem src = readSlot(player, container, fromSide, fromIdx);
            if (src == null) return;
            final GameItem dst = readSlot(player, container, toSide, toIdx);

            // Whitelist: anything entering the storage side must pass canStore.
            if (toSide == PotionStorageMovePacket.SIDE_STORAGE && !PotionStorage.canStore(src)) {
                log.info("[PotionStorage] Rejected non-stackable/non-gem item {} from p{}",
                        src.getName(), player.getId());
                return;
            }
            // The item we displace from a storage slot also has to qualify if it
            // ends up going back to inventory (it always does, since we don't
            // gate inv→storage on dst). Inventory is unrestricted, so no check
            // is needed for moves landing in inventory.

            // Stack-merge: same item id + both stackable + dst not full.
            if (dst != null && canMerge(src, dst)) {
                final int room = dst.getMaxStack() - dst.getStackCount();
                if (room > 0) {
                    final int moved = Math.min(room, src.getStackCount());
                    dst.setStackCount(dst.getStackCount() + moved);
                    if (moved >= src.getStackCount()) {
                        writeSlot(player, container, fromSide, fromIdx, null);
                    } else {
                        src.setStackCount(src.getStackCount() - moved);
                    }
                    pushUpdates(mgr, realm, player, container);
                    return;
                }
            }

            // Otherwise: plain swap.
            writeSlot(player, container, toSide, toIdx, src);
            writeSlot(player, container, fromSide, fromIdx, dst);
            pushUpdates(mgr, realm, player, container);
        } catch (Exception e) {
            log.error("[PotionStorage] handleMove failed: {}", e.getMessage(), e);
        }
    }

    private static boolean canMerge(GameItem a, GameItem b) {
        if (a == null || b == null) return false;
        if (a.getItemId() != b.getItemId()) return false;
        if (!a.isStackable() || !b.isStackable()) return false;
        return b.getStackCount() < b.getMaxStack();
    }

    private static boolean isValidIdx(byte side, int idx, Player player) {
        if (side == PotionStorageMovePacket.SIDE_INV) {
            return idx >= 0 && player.getInventory() != null && idx < player.getInventory().length;
        }
        if (side == PotionStorageMovePacket.SIDE_STORAGE) {
            return idx >= 0 && idx < PotionStorage.SIZE;
        }
        return false;
    }

    private static GameItem readSlot(Player player, PotionStorage container, byte side, int idx) {
        return side == PotionStorageMovePacket.SIDE_INV ? player.getInventory()[idx] : container.getItems()[idx];
    }

    private static void writeSlot(Player player, PotionStorage container, byte side, int idx, GameItem item) {
        if (side == PotionStorageMovePacket.SIDE_INV) {
            player.getInventory()[idx] = item;
        } else {
            container.getItems()[idx] = item;
        }
    }

    private static PotionStorage ensureContainer(Realm realm, Player player) {
        List<PotionStorage> list = realm.getPlayerPotionStorage().get(player.getId());
        if (list == null) {
            list = new ArrayList<>();
            realm.getPlayerPotionStorage().put(player.getId(), list);
        }
        if (list.isEmpty()) {
            list.add(new PotionStorage(0));
        }
        return list.get(0);
    }

    private static void pushUpdates(RealmManagerServer mgr, Realm realm, Player player, PotionStorage container)
            throws Exception {
        mgr.enqueueServerPacket(player, new PotionStorageUpdatePacket(player.getId(), toNetItems(container.getItems())));
        final UpdatePacket inv = realm.getPlayerAsPacket(player.getId());
        if (inv != null) mgr.enqueueServerPacket(player, inv);
    }

    private static NetGameItem[] toNetItems(GameItem[] items) {
        final NetGameItem[] out = new NetGameItem[PotionStorage.SIZE];
        if (items == null) return out;
        for (int i = 0; i < PotionStorage.SIZE && i < items.length; i++) {
            out[i] = toNetGameItem(items[i]);
        }
        return out;
    }

    /**
     * Hand-rolled GameItem → NetGameItem copy that preserves stackCount,
     * enchantments, attribute modifiers, and forge metadata. ModelMapper drops
     * nested generics on this DTO, so the inventory broadcast path uses the
     * same approach (see UpdatePacket.toNetGameItem). Kept private here to
     * avoid coupling to that method's visibility.
     */
    private static NetGameItem toNetGameItem(GameItem item) {
        if (item == null) return new NetGameItem();
        final NetGameItem net = IOService.mapModel(item, NetGameItem.class);
        net.setStackable(item.isStackable());
        net.setMaxStack(item.getMaxStack());
        net.setStackCount(item.getStackCount());
        net.setCategory(item.getCategory());
        net.setForgeStatId(item.getForgeStatId());
        net.setForgeSlotId(item.getForgeSlotId());
        net.setRarity(item.getRarity());
        net.setGemEffectType(item.getGemEffectType());
        net.setGemParam1(item.getGemParam1());
        net.setGemMagnitude(item.getGemMagnitude());
        net.setGemDurationMs(item.getGemDurationMs());
        final List<NetEnchantment> ench;
        if (item.getEnchantments() != null && !item.getEnchantments().isEmpty()) {
            ench = new ArrayList<>(item.getEnchantments().size());
            for (Enchantment e : item.getEnchantments()) {
                ench.add(new NetEnchantment(e.getStatId(), e.getDeltaValue(), e.getPixelX(), e.getPixelY(),
                        e.getPixelColor(), e.getEffectType(), e.getParam1(), e.getMagnitude(), e.getDurationMs()));
            }
        } else {
            ench = new ArrayList<>();
        }
        net.setEnchantments(ench);
        final List<NetAttributeModifier> mods;
        if (item.getAttributeModifiers() != null && !item.getAttributeModifiers().isEmpty()) {
            mods = new ArrayList<>(item.getAttributeModifiers().size());
            for (AttributeModifier m : item.getAttributeModifiers()) {
                mods.add(new NetAttributeModifier(m.getStatId(), m.getDeltaValue()));
            }
        } else {
            mods = new ArrayList<>();
        }
        net.setAttributeModifiers(mods);
        return net;
    }
}
