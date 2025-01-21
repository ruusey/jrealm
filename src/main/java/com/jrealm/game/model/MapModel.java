package com.jrealm.game.model;

import java.util.Map;

import com.jrealm.game.math.Vector2f;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class MapModel {
    private int mapId;
    private String mapName;
    private String mapKey;
    private int tileSize;
    private int width;
    private int height;
    private int terrainId;
    private int dungeonId;
    private Map<String, int[][]> data;
    private DungeonGenerationParams dungeonParams;

    public Vector2f getCenter() {
        return new Vector2f((this.width / 2) * this.tileSize, ((this.height / 2) * (this.tileSize)));
    }
}
