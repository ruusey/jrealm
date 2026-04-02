package com.jrealm.net.realm;

/**
 * Server-side dead reckoning state for a single entity. Tracks the last
 * position/velocity that was actually transmitted to the client, so the server
 * can predict what the client believes the entity's position to be and only
 * send a correction when the real position diverges beyond a threshold.
 */
public class EntityMotionState {
    private float sentPosX;
    private float sentPosY;
    private float sentVelX;
    private float sentVelY;
    private long sentTick;

    // Dead reckoning thresholds
    // Position error (squared) before we send a correction — 3px radius
    private static final float POSITION_THRESHOLD_SQ = 3.0f * 3.0f;
    // Velocity change (squared) before we send a correction — catches direction changes
    private static final float VELOCITY_THRESHOLD_SQ = 0.25f;
    // Maximum ticks before forcing an update regardless (staleness cap at ~500ms)
    private static final int MAX_STALE_TICKS = 32;

    public EntityMotionState(float posX, float posY, float velX, float velY, long tick) {
        this.sentPosX = posX;
        this.sentPosY = posY;
        this.sentVelX = velX;
        this.sentVelY = velY;
        this.sentTick = tick;
    }

    /**
     * Returns what the client predicts this entity's position to be right now,
     * based on the last transmitted pos+vel and elapsed ticks.
     */
    public float predictedX(long currentTick, float tickDuration) {
        float elapsed = (currentTick - this.sentTick) * tickDuration;
        return this.sentPosX + this.sentVelX * elapsed;
    }

    public float predictedY(long currentTick, float tickDuration) {
        float elapsed = (currentTick - this.sentTick) * tickDuration;
        return this.sentPosY + this.sentVelY * elapsed;
    }

    /**
     * Determines whether the server needs to send a correction for this entity.
     * Compares actual server state against what the client would predict.
     */
    public boolean needsUpdate(float actualPosX, float actualPosY,
                               float actualVelX, float actualVelY,
                               long currentTick, float tickDuration) {
        // 1. Staleness cap — always correct after MAX_STALE_TICKS
        if (currentTick - this.sentTick >= MAX_STALE_TICKS) {
            return true;
        }

        // 2. Velocity changed significantly (direction change, start/stop)
        float dvx = actualVelX - this.sentVelX;
        float dvy = actualVelY - this.sentVelY;
        if (dvx * dvx + dvy * dvy > VELOCITY_THRESHOLD_SQ) {
            return true;
        }

        // 3. Position diverged from client prediction beyond threshold
        float predX = predictedX(currentTick, tickDuration);
        float predY = predictedY(currentTick, tickDuration);
        float dpx = actualPosX - predX;
        float dpy = actualPosY - predY;
        if (dpx * dpx + dpy * dpy > POSITION_THRESHOLD_SQ) {
            return true;
        }

        return false;
    }

    /**
     * Updates the sent state after a correction is transmitted.
     */
    public void markSent(float posX, float posY, float velX, float velY, long tick) {
        this.sentPosX = posX;
        this.sentPosY = posY;
        this.sentVelX = velX;
        this.sentVelY = velY;
        this.sentTick = tick;
    }

    public float getSentPosX() { return sentPosX; }
    public float getSentPosY() { return sentPosY; }
    public float getSentVelX() { return sentVelX; }
    public float getSentVelY() { return sentVelY; }
    public long getSentTick() { return sentTick; }
}
