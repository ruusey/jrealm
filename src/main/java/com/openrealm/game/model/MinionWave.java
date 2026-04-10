package com.openrealm.game.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Defines a wave of minions that a realm event boss summons
 * when its HP drops below a threshold.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinionWave {
    private float triggerHpPercent; // Spawn when boss HP% drops below this (e.g. 0.75 = 75%)
    private int enemyId;
    private int count;
    private int eventMultiplier;
    private float offset;           // Spawn distance from boss in pixels
}
