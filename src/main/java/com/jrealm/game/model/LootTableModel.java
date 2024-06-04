package com.jrealm.game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.realm.Realm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LootTableModel {
    private Integer enemyId;
    private Map<String, Float> drops;

    public List<GameItem> getLootDrop() {
        final List<GameItem> itemsToDrop = new ArrayList<>();
        itemsToDrop.addAll(this.getGuarunteedDrops());

        for (final Map.Entry<String, Float> entry : this.drops.entrySet()) {
            if (this.isLootGroup(entry.getKey()) && (Realm.RANDOM.nextFloat() < entry.getValue())) {
                final LootGroupModel lootGroup = GameDataManager.LOOT_GROUPS.get(this.getLootGroupId(entry.getKey()));
                final int itemFromGroup = lootGroup.getPotentialDrops()
                        .get(Realm.RANDOM.nextInt(lootGroup.getPotentialDrops().size()));
                itemsToDrop.add(GameDataManager.GAME_ITEMS.get(itemFromGroup));
            } else if (!this.isLootGroup(entry.getKey()) && (Realm.RANDOM.nextFloat() < entry.getValue())) {
                final GameItem item = GameDataManager.GAME_ITEMS.get(this.getLootGroupId(entry.getKey()));
                itemsToDrop.add(item);
            }
        }
        return itemsToDrop;
    }

    public List<GameItem> getLootGroup(String groupKey) throws Exception {
        String[] split = groupKey.split(":");
        if ((split.length != 2) || !split[0].equalsIgnoreCase("group"))
            throw new Exception("This loot table key entry is not a loot group!");
        LootGroupModel model = GameDataManager.LOOT_GROUPS.get(Integer.parseInt(split[1]));
        final List<GameItem> results = new ArrayList<>();
        for (int i : model.getPotentialDrops()) {
            results.add(GameDataManager.GAME_ITEMS.get(i));
        }
        return results;
    }

    private boolean isLootGroup(String key) {
        String[] split = key.split(":");
        if ((split.length != 2) || !split[0].equalsIgnoreCase("group"))
            return false;
        return true;
    }

    private int getLootGroupId(String key) {
        return Integer.parseInt(key.split(":")[1]);
    }

    private List<GameItem> getGuarunteedDrops() {
        List<GameItem> res = new ArrayList<>();
        for (Entry<String, Float> drop : this.drops.entrySet()) {
            if (drop.getValue() == 1.0d) {
                if (this.isLootGroup(drop.getKey())) {
                    final LootGroupModel lootGroup = GameDataManager.LOOT_GROUPS
                            .get(this.getLootGroupId(drop.getKey()));
                    final int itemFromGroup = lootGroup.getPotentialDrops()
                            .get(Realm.RANDOM.nextInt(lootGroup.getPotentialDrops().size()));
                    res.add(GameDataManager.GAME_ITEMS.get(itemFromGroup));
                } else {
                    res.add(GameDataManager.GAME_ITEMS.get(this.getLootGroupId(drop.getKey())));
                }
            }
        }
        return res;
    }

    // private List<GameItem> getPossibleGameItems() {
    // return this.drops.keySet().stream().filter(key -> !this.isLootGroup(key))
    // .map(key -> Integer.parseInt(key.split(":")[1])).map(itemId ->
    // GameDataManager.GAME_ITEMS.get(itemId))
    // .collect(Collectors.toList());
    // }
}
