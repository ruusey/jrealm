package com.jrealm.game.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OverworldZone {
    private String zoneId;
    private String displayName;
    @Builder.Default
    private float minRadius = 0f;
    @Builder.Default
    private float maxRadius = 1f;
    @Builder.Default
    private int tileGroupOrdinal = 0;
    @Builder.Default
    private int enemyGroupOrdinal = 0;
    @Builder.Default
    private float difficulty = 1.0f;
    private Map<String, Float> portalDrops;
}
