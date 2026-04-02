package com.jrealm.game.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.entity.NetObjectMovement;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public abstract class GameObject {
    protected long id;
    protected Rectangle bounds;
    protected Vector2f pos;
    protected int size;
    protected int spriteX;
    protected int spriteY;

    protected float dx;
    protected float dy;

    protected boolean teleported = false;
    protected String name = "";

    public boolean discovered;
    private SpriteSheet spriteSheet;

    public GameObject(long id, Vector2f origin, int spriteX, int spriteY, int size) {
        this(id, origin, size);
    }

    public void setSpriteSheet(final SpriteSheet spriteSheet) {
        this.spriteSheet = spriteSheet;
    }

    public GameObject(long id, Vector2f origin, int size) {
        this.id = id;
        this.bounds = new Rectangle(origin, size, size);
        this.pos = origin;
        this.size = size;
    }

    public void setPos(Vector2f pos) {
        this.pos = pos;
        this.bounds = new Rectangle(pos, this.size, this.size);
        this.teleported = true;
    }

    public boolean getTeleported() {
        return this.teleported;
    }

    public void setTeleported(final boolean teleported) {
        this.teleported = teleported;
    }

    public void addForce(float a, boolean vertical) {
        if (!vertical) {
            this.dx -= a;
        } else {
            this.dy -= a;
        }
    }

    public void update() {

    }

    public void applyMovementLerp(float velX, float velY, float pct) {
        final float lerpX = this.lerp(this.pos.x, this.pos.x + velX, pct);
        final float lerpY = this.lerp(this.pos.y, this.pos.y + velY, pct);

        this.pos = new Vector2f(lerpX, lerpY);
    }

    // Snap threshold: if server position is more than 3 tiles away, snap instead of lerp.
    // This handles teleports (planewalker cloak, portals) where lerp would cause a slow slide.
    private static final float SNAP_DISTANCE_SQ = (3 * 32) * (3 * 32);

    public void applyMovementLerp(NetObjectMovement packet, float pct) {
        float dx = packet.getPosX() - this.pos.x;
        float dy = packet.getPosY() - this.pos.y;
        if (dx * dx + dy * dy > SNAP_DISTANCE_SQ) {
            // Large jump — snap directly (teleport, portal, etc.)
            this.pos.x = packet.getPosX();
            this.pos.y = packet.getPosY();
        } else {
            this.pos.x = this.lerp(this.pos.x, packet.getPosX(), pct);
            this.pos.y = this.lerp(this.pos.y, packet.getPosY(), pct);
        }
        this.bounds = new Rectangle(this.pos, this.size, this.size);
        this.dx = packet.getVelX();
        this.dy = packet.getVelY();
    }

    public void applyMovementLerp(NetObjectMovement packet) {
        final float lerpX = this.lerp(this.pos.x, packet.getPosX(), 0.65f);
        final float lerpY = this.lerp(this.pos.y, packet.getPosY(), 0.65f);

        this.pos = new Vector2f(lerpX, lerpY);
        this.bounds = new Rectangle(this.pos, this.size, this.size);
        this.dx = packet.getVelX();
        this.dy = packet.getVelY();
    }

    public void applyMovement(NetObjectMovement packet) {
        this.pos = new Vector2f(packet.getPosX(), packet.getPosY());
        this.bounds = new Rectangle(this.pos, this.size, this.size);
        this.dx = packet.getVelX();
        this.dy = packet.getVelY();
    }

    // --- Dead reckoning support ---
    // When a server correction arrives, we store the offset between where we
    // predicted the entity to be and where the server says it actually is.
    // Each frame, we blend this offset toward zero so the entity smoothly
    // converges on the corrected position without visible snapping.
    protected float correctionOffsetX = 0f;
    protected float correctionOffsetY = 0f;
    // Correction blend rate per tick — higher = faster snap, lower = smoother.
    // 0.15 means ~85% of error corrected within 10 ticks (~160ms at 64Hz client).
    private static final float CORRECTION_BLEND_RATE = 0.15f;
    // If correction offset exceeds this, snap immediately (teleport/portal)
    private static final float CORRECTION_SNAP_THRESHOLD_SQ = (3 * 32) * (3 * 32);

    /**
     * Apply a dead reckoning server correction. Instead of snapping the entity,
     * we compute the error between our local position and the server's corrected
     * position, and store it as an offset to be blended out over subsequent frames.
     * Velocity is always updated immediately since it affects future extrapolation.
     */
    public void applyServerCorrection(NetObjectMovement packet) {
        float errorX = packet.getPosX() - this.pos.x;
        float errorY = packet.getPosY() - this.pos.y;

        if (errorX * errorX + errorY * errorY > CORRECTION_SNAP_THRESHOLD_SQ) {
            // Large error — snap directly (teleport, realm transition, etc.)
            this.pos.x = packet.getPosX();
            this.pos.y = packet.getPosY();
            this.correctionOffsetX = 0f;
            this.correctionOffsetY = 0f;
        } else {
            // Accumulate correction offset — will be blended out each tick
            this.correctionOffsetX += errorX;
            this.correctionOffsetY += errorY;
        }
        // Always update velocity immediately — it drives future extrapolation
        this.dx = packet.getVelX();
        this.dy = packet.getVelY();
        this.bounds = new Rectangle(this.pos, this.size, this.size);
    }

    /**
     * Advance position by velocity (dead reckoning extrapolation) and blend
     * any pending correction offset. Call this once per client tick for entities
     * that use dead reckoning (enemies). For players, use blendCorrectionOffset()
     * instead since movePlayer() handles velocity advancement with collision checks.
     */
    public void extrapolate() {
        // Advance position using current velocity
        this.pos.x += this.dx;
        this.pos.y += this.dy;

        // Blend correction offset
        this.blendCorrectionOffset();
    }

    /**
     * Blend pending correction offset toward zero without advancing by velocity.
     * Use this for entities where velocity advancement is handled elsewhere
     * (e.g., players with collision-checked movement in PlayState.movePlayer).
     */
    public void blendCorrectionOffset() {
        if (this.correctionOffsetX != 0f || this.correctionOffsetY != 0f) {
            float blendX = this.correctionOffsetX * CORRECTION_BLEND_RATE;
            float blendY = this.correctionOffsetY * CORRECTION_BLEND_RATE;
            this.pos.x += blendX;
            this.pos.y += blendY;
            this.correctionOffsetX -= blendX;
            this.correctionOffsetY -= blendY;

            // Zero out tiny residuals to avoid perpetual micro-corrections
            if (this.correctionOffsetX * this.correctionOffsetX +
                this.correctionOffsetY * this.correctionOffsetY < 0.01f) {
                this.correctionOffsetX = 0f;
                this.correctionOffsetY = 0f;
            }
        }
        this.bounds = new Rectangle(this.pos, this.size, this.size);
    }

    private float lerp(float start, float end, float pct) {
        return (start + ((end - start) * pct));
    }

    public Vector2f getCenteredPosition() {
        return this.pos.clone((this.getSize() / 2), this.getSize() / 2);
    }

    @Override
    public Vector2f clone() {
        Vector2f newVector = new Vector2f(this.pos.x, this.pos.y);
        return newVector;
    }

    public void render(SpriteBatch batch) {
        if (this.spriteSheet == null) {
            GameObject.log.warn("GameObject {} does not have a sprite sheet!");
            return;
        }
        TextureRegion frame = this.spriteSheet.getCurrentFrame();
        if (frame != null) {
            batch.draw(frame, this.pos.getWorldVar().x, this.pos.getWorldVar().y, this.size, this.size);
        }
    }

    @Override
    public String toString() {
        return "$" + this.name;
    }
}
