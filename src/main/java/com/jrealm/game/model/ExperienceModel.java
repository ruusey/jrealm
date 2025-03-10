package com.jrealm.game.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jrealm.util.Tuple;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExperienceModel {
    private Map<String, String> levelExperienceMap;

    @JsonIgnore
    private Map<Integer, Tuple<Integer, Integer>> parsedMap;

    public void parseMap() {
        this.parsedMap = new HashMap<>();
        for (final Entry<String, String> entry : this.levelExperienceMap.entrySet()) {
            final String[] range = entry.getValue().split("-");
            final Tuple<Integer, Integer> value = new Tuple<Integer, Integer>(Integer.parseInt(range[0]),
                    Integer.parseInt(range[1]));
            this.parsedMap.put(Integer.parseInt(entry.getKey()), value);
        }
    }

    public boolean isMaxLvl(long experience) {
        return experience >= this.maxExperience();
    }

    public long getBaseFame(long experience) {
        long fame = 0;
        if (experience > this.maxExperience()) {
            fame = (experience - this.maxExperience()) / 2500;
        }
        return fame;
    }

    public int getLevel(long experience) {
        if (experience > this.maxExperience())
            return this.maxLevel();

        int level = 1;
        for (final Entry<Integer, Tuple<Integer, Integer>> entry : this.parsedMap.entrySet()) {
            if ((entry.getValue().getX() <= experience) && (entry.getValue().getY() >= experience)) {
                level = entry.getKey();
            }
        }

        return level;
    }

    public int maxExperience() {
        int best = 0;
        for (final Tuple<Integer, Integer> entry : this.parsedMap.values()) {
            if (entry.getY() > best) {
                best = entry.getY();
            }
        }
        return best;
    }

    public int maxLevel() {
        int best = 0;
        for (final Integer entry : this.parsedMap.keySet()) {
            if (entry > best) {
                best = entry;
            }
        }
        return best;
    }

    // Helper method to generate the exp-level curve
    private static void createExpModel() {
        int desiredLevels = 20;
        int currentXp = 0;
        int increment = 1000;
        float step = 1.15f;
        String content = "";
        for (int i = 1; i < (desiredLevels + 1); i++) {
            int targetXp = 0;
            if (i == -1) {
                targetXp = currentXp + increment;
            } else {
                targetXp = (int) ((currentXp + increment) * step);
            }
            content += "\"" + i + "\": " + "\"" + currentXp + "-" + targetXp + "\",\n";
            currentXp = targetXp + 1;

        }
        System.out.println(content);
    }

    public static void main(String[] args) {
        ExperienceModel.createExpModel();
    }

}
