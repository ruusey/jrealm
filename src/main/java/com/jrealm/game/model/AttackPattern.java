package com.jrealm.game.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttackPattern {
    private int projectileGroupId;
    @Builder.Default
    private int cooldownMs = 1000;
    @Builder.Default
    private int burstCount = 1;
    @Builder.Default
    private int burstDelayMs = 100;
    @Builder.Default
    private float angleOffsetPerBurst = 0f;
    @Builder.Default
    private float minRange = 0f;
    @Builder.Default
    private float maxRange = 9999f;
    @Builder.Default
    private boolean predictive = false;
}
