package com.openrealm.game.model;

import java.util.HashMap;
import java.util.Map;

import com.openrealm.game.data.GameDataManager;
import com.openrealm.game.entity.item.GameItem;
import com.openrealm.game.entity.item.Stats;
import com.openrealm.net.realm.Realm;

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
    private Map<Integer, Integer> startingEquipment;

    public Stats getRandomLevelUpStats() {
        final ExperienceModel expModel = GameDataManager.EXPERIENCE_LVLS;
        final Stats difference = this.maxStats.subtract(this.baseStats);
        int hpRange = (difference.getHp() / expModel.maxLevel()) + 15;
        if (hpRange < 0)
            hpRange = 1;
        int mpRange = (difference.getMp() / expModel.maxLevel()) + 2;
        if (mpRange < 0)
            mpRange = 1;
        int spdRange = (difference.getSpd() / expModel.maxLevel()) + 1;
        if (spdRange < 0)
            spdRange = 1;
        int dexRange = (difference.getDex() / expModel.maxLevel()) + 1;
        if (dexRange < 0)
            dexRange = 1;
        int attRange = (difference.getAtt() / expModel.maxLevel()) + 1;
        if (attRange < 0)
            attRange = 1;
        int wisRange = (difference.getWis() / expModel.maxLevel()) + 1;
        if (wisRange < 0)
            wisRange = 1;
        int vitRange = (difference.getVit() / expModel.maxLevel()) + 1;
        if (vitRange < 0)
            vitRange = 1;
        // Guarantee at least 1/3 of each range as a minimum gain per level,
        // so players never get a "dead" level-up with +0 to important stats.
        int randomHp = hpRange / 3 + Realm.RANDOM.nextInt(hpRange - hpRange / 3 + 1);
        int randomMp = mpRange / 3 + Realm.RANDOM.nextInt(mpRange - mpRange / 3 + 1);
        int randomSpd = Math.max(1, spdRange / 3) + Realm.RANDOM.nextInt(Math.max(1, spdRange - spdRange / 3));
        int randomDex = Math.max(1, dexRange / 3) + Realm.RANDOM.nextInt(Math.max(1, dexRange - dexRange / 3));
        int randomAtt = Math.max(1, attRange / 3) + Realm.RANDOM.nextInt(Math.max(1, attRange - attRange / 3));
        int randomWis = Math.max(1, wisRange / 3) + Realm.RANDOM.nextInt(Math.max(1, wisRange - wisRange / 3));
        int randomVit = Math.max(1, vitRange / 3) + Realm.RANDOM.nextInt(Math.max(1, vitRange - vitRange / 3));

        // Return a random stat increase with the defense always being 0
        return new Stats((short) randomHp, (short) randomMp, (short) 0, (short) randomAtt, (short) randomSpd,
                (short) randomDex, (short) randomVit, (short) randomWis);
    }

    public int countMaxedStats(final Stats characterStats) {
        Stats combined = this.maxStats.subtract(characterStats);
        int count = 0;
        if (combined.getHp() <= 0) {
            count++;
        }
        if (combined.getMp() <= 0) {
            count++;
        }
        if (combined.getAtt() <= 0) {
            count++;
        }
        if (combined.getDef() <= 0) {
            count++;
        }
        if (combined.getSpd() <= 0) {
            count++;
        }
        if (combined.getDex() <= 0) {
            count++;
        }
        if (combined.getVit() <= 0) {
            count++;
        }
        if (combined.getWis() <= 0) {
            count++;
        }
        return count;
    }
    
    public Map<Integer, GameItem> getStartingEquipmentMap(){
        final Map<Integer, GameItem> result = new HashMap<>();
        for(Map.Entry<Integer,Integer> entry : this.startingEquipment.entrySet()) {
            result.put(entry.getKey(), GameDataManager.GAME_ITEMS.get(entry.getValue()));
        }
        return result;
    }

    public static void main(String[] args) {
        GameDataManager.loadGameData(false);
        CharacterClassModel model = GameDataManager.CHARACTER_CLASSES.get(0);
        model.getStartingEquipmentMap();
        final ExperienceModel expModel = GameDataManager.EXPERIENCE_LVLS;
        Stats startStats = model.getBaseStats();
        System.out.println(startStats.toString());
        for (int i = 0; i < expModel.maxLevel(); i++) {
            startStats = startStats.concat(model.getRandomLevelUpStats());
            System.out.println(startStats.toString());
        }
    }
}
