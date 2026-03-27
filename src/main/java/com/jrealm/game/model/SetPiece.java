package com.jrealm.game.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetPiece {
    private String name;
    private int minCount;
    private int maxCount;
    private List<String> allowedZones;
    private int width;
    private int height;
    private int baseTileId;
    private int[][] collisionLayout;
}
