package com.jrealm.game.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data model for a realm event loaded from realm-events.json.
 * A realm event is a globally announced boss encounter that stamps custom terrain,
 * spawns a boss with phased minion waves, and requires coordinated group effort.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealmEventModel {
    private int eventId;
    private String name;
    private String announceMessage;   // Formatted with zone name, e.g. "A %s has appeared in the %s!"
    private String defeatMessage;
    private String timeoutMessage;
    private int bossEnemyId;
    private int eventMultiplier;
    private int setPieceId;           // SetPieceModel to stamp as arena (-1 = none)
    private List<String> allowedZones;
    private int durationSeconds;
    private List<MinionWave> minionWaves;
}
