package com.jrealm.game.model;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.realm.Realm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterClassModel {
	private int spriteOffset;
	private int classId;
	private String className;
	private Stats baseStats;
	private Stats maxStats;

	public Stats getRandomLevelUpStats() {
		final ExperienceModel expModel = GameDataManager.EXPERIENCE_LVLS;
		final Stats difference = this.maxStats.subtract(this.baseStats);
		int randomHp = Realm.RANDOM.nextInt((difference.getHp() / expModel.maxLevel()) + 15);
		int randomMp = Realm.RANDOM.nextInt((difference.getMp() / expModel.maxLevel()) + 2);
		int randomSpd = Realm.RANDOM.nextInt((difference.getSpd() / expModel.maxLevel()) + 1);
		int randomDex = Realm.RANDOM.nextInt((difference.getDex() / expModel.maxLevel()) + 1);
		int randomAtt = Realm.RANDOM.nextInt((difference.getAtt() / expModel.maxLevel()) + 1);
		int randomWis = Realm.RANDOM.nextInt((difference.getWis() / expModel.maxLevel()) + 1);
		int randomVit = Realm.RANDOM.nextInt((difference.getVit() / expModel.maxLevel()) + 1);

		// Return a random stat increase with the defense always being 0
		return new Stats((short) randomHp, (short) randomMp, (short) 0, (short) randomAtt, (short) randomSpd,
				(short) randomDex, (short) randomVit, (short) randomWis);
	}

	public static void main(String[] args) {
		GameDataManager.loadGameData(true);
		CharacterClassModel model = GameDataManager.CHARACTER_CLASSES.get(0);
		final ExperienceModel expModel = GameDataManager.EXPERIENCE_LVLS;
		Stats startStats = model.getBaseStats();
		System.out.println(startStats.toString());
		for (int i = 0; i < expModel.maxLevel(); i++) {
			startStats = startStats.concat(model.getRandomLevelUpStats());
			System.out.println(startStats.toString());
		}
	}
}
