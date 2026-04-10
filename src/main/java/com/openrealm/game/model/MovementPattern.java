package com.openrealm.game.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovementPattern {
    @Builder.Default
    private String type = "CHASE";
    @Builder.Default
    private float speed = 1.4f;
    // ORBIT
    @Builder.Default
    private float radius = 120f;
    @Builder.Default
    private String direction = "CW";
    // CHARGE
    @Builder.Default
    private float chargeDistanceMin = 80f;
    @Builder.Default
    private int pauseMs = 500;
    // FLEE
    @Builder.Default
    private float fleeRange = 60f;
    // WANDER
    @Builder.Default
    private float wanderRadius = 100f;
    // STRAFE
    @Builder.Default
    private float preferredRange = 100f;
    // ANCHOR
    @Builder.Default
    private float anchorRadius = 150f;
}
