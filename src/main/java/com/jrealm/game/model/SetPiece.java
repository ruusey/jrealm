package com.jrealm.game.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Placement reference for a setpiece within a terrain definition.
 * Points to a {@link SetPieceModel} by ID and carries placement rules
 * (how many instances, which zones).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetPiece {
    private int setPieceId;
    private int minCount;
    private int maxCount;
    private List<String> allowedZones;
}
