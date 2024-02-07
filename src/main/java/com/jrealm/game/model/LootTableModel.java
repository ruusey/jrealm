package com.jrealm.game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
		List<Integer> possibleItems = this.getPossibleGameItems();
		List<GameItem> itemsToDrop = this.getGuarunteedDrops();
		int randIdx = Realm.RANDOM.nextInt(possibleItems.size());
		int gameItemIdToDrop = possibleItems.get(randIdx);
		GameItem item = GameDataManager.GAME_ITEMS.get(gameItemIdToDrop);

		if (Realm.RANDOM.nextFloat() < this.drops.get(gameItemIdToDrop + "")) {
			itemsToDrop.add(item);
		}

		return itemsToDrop;

	}

	private List<GameItem> getGuarunteedDrops() {
		List<GameItem> res = new ArrayList<>();
		for (Entry<String, Float> drop : this.drops.entrySet()) {
			if (drop.getValue() == 1.0d) {
				res.add(GameDataManager.GAME_ITEMS.get(Integer.parseInt(drop.getKey())));
			}
		}
		return res;
	}

	private List<Integer> getPossibleGameItems() {
		return this.drops.keySet().stream().map(Integer::parseInt).collect(Collectors.toList());
	}
}
