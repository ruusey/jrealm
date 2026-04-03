package com.jrealm.game.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reusable setpiece template loaded from setpieces.json.
 * A setpiece is a small static map fragment with base and collision layers
 * that can be stamped into procedural terrains or static maps.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetPieceModel {
    private int setPieceId;
    private String name;
    private int width;
    private int height;
    private int[][] baseLayout;      // Layer 0 tile IDs (0 = don't overwrite existing terrain)
    private int[][] collisionLayout; // Layer 1 tile IDs (0 = don't overwrite)
}
