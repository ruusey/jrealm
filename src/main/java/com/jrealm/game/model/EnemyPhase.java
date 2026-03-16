package com.jrealm.game.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnemyPhase {
    @Builder.Default
    private String name = "default";
    @Builder.Default
    private float hpThreshold = 1.0f;
    private MovementPattern movement;
    private List<AttackPattern> attacks;
}
