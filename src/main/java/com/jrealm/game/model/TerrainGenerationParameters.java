package com.jrealm.game.model;

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

}
