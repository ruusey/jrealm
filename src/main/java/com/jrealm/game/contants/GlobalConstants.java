package com.jrealm.game.contants;

public class GlobalConstants {

    // Loot tier upgrade: in zones with difficulty > 1, each dropped item has a
    // (BASE + PER_DIFFICULTY * difficulty)% chance to be upgraded one tier.
    public static final float LOOT_TIER_UPGRADE_BASE_PERCENT = 0.0f;
    public static final float LOOT_TIER_UPGRADE_PER_DIFFICULTY = 5.0f;
    public static final float LOOT_TIER_UPGRADE_MIN_DIFFICULTY = 1.0f;

    public static final int BASE_SIZE = 32;
    
    public static final int BASE_TILE_SIZE = 32;

    public static final int BASE_SPRITE_SIZE = 8;

    public static final int PLAYER_CAP = 40;

    public static final int TILE_SIZE_NORM = 64;

    public static final int MEDIUM_ART_SIZE = 16;

    public static final int LARGE_ART_SIZE = 32;

    public static final int PLAYER_SIZE = 28;

    public static final int PLAYER_RENDER_SIZE = 32;
    
    public static final long SOCKET_READ_TIMEOUT = 15000;
}
