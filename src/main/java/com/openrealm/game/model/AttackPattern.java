package com.openrealm.game.model;

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
    @Builder.Default
    private String aimMode = "PLAYER";
    @Builder.Default
    private int shotCount = 1;
    @Builder.Default
    private float spreadAngle = 0f;
    @Builder.Default
    private float fixedAngle = 0f;
    @Builder.Default
    private boolean mirror = false;
    @Builder.Default
    private int sourceNoise = 0;

    // Spiral: angle added to base angle each time this attack fires (accumulates over firings)
    @Builder.Default
    private float angleIncrementPerFiring = 0f;

    // Speed stacking: fire multiple bullets at same angle with different speeds
    @Builder.Default
    private int speedCount = 1;
    @Builder.Default
    private float minSpeedMult = 1.0f;
    @Builder.Default
    private float maxSpeedMult = 1.0f;
}
