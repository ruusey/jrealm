package com.openrealm.game.tile;

public enum RoomShapeTemplate {
    RECTANGLE, OVAL, DIAMOND, CROSS, L_SHAPE,
    /** Equilateral-ish triangle pointing in a random direction. */
    TRIANGLE,
    /** Organic cave-like blob generated via cellular automata. */
    IRREGULAR
}
