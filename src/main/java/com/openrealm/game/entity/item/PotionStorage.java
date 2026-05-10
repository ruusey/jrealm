package com.openrealm.game.entity.item;

import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-player potion-storage container. Held in a Map on the vault Realm and
 * round-tripped through PlayerAccount.playerPotionStorage. Unlike Chest, this
 * is not placed as a LootContainer on the map — the interaction tile (id 328)
 * is the visual; the data lives in this POJO and the server pushes contents
 * to the client via OpenPotionStoragePacket / PotionStorageUpdatePacket.
 */
@Data
@NoArgsConstructor
public class PotionStorage {
    public static final int SIZE = 32;

    private String chestUid;
    private int ordinal;
    private GameItem[] items;

    public PotionStorage(int ordinal) {
        this.chestUid = UUID.randomUUID().toString();
        this.ordinal = ordinal;
        this.items = new GameItem[SIZE];
    }

    public PotionStorage(String chestUid, int ordinal, GameItem[] items) {
        this.chestUid = chestUid != null ? chestUid : UUID.randomUUID().toString();
        this.ordinal = ordinal;
        this.items = items != null && items.length == SIZE ? items : new GameItem[SIZE];
    }

    /** Whitelist: only stackable items + gems may live in a potion-storage slot. */
    public static boolean canStore(GameItem item) {
        if (item == null) return true;
        return item.isStackable() || "gem".equals(item.getCategory());
    }
}
