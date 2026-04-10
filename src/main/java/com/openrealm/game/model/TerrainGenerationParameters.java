package com.openrealm.game.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerrainGenerationParameters {
    private int terrainId;
    private String name;
    private int width;
    private int height;
    private int tileSize;
    private List<TileGroup> tileGroups;
    private List<EnemyGroup> enemyGroups;
    private List<OverworldZone> zones;
    private List<SetPiece> setPieces;
    /** Global terrain difficulty multiplier — used when terrain has no zones.
     *  Scales enemy health and XP. */
    private float difficulty;
    /** Enemy spawn density 0.0-1.0 (fraction of eligible tiles that get an enemy).
     *  If absent/zero, falls back to legacy threshold logic. */
    private float enemyDensity;
}
