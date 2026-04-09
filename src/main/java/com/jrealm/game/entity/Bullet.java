package com.jrealm.game.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.contants.ProjectileFlag;
import com.jrealm.game.contants.StatusEffectType;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.ProjectileGroup;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Bullet extends GameObject  {
	private long srcEntityId;
    private int projectileId;
    private float angle;
    private float magnitude;
    private float range;
    private short damage;
    private boolean isEnemy;
    private boolean playerHit;
    private boolean enemyHit;
    private float tfAngle = (float) (Math.PI / 2);

    /**
     * Projectile behavior flags (ProjectileFlag IDs): PLAYER_PROJECTILE(10),
     * PARAMETRIC(12), INVERTED_PARAMETRIC(13), ORBITAL(20).
     * Controls movement — NOT on-hit effects.
     */
    private List<Short> flags;
    /**
     * On-hit status effects (StatusEffect IDs + durations).
     * Applied to the target entity when this bullet hits.
     * NOT behavior flags — those go in {@link #flags}.
     */
    private List<com.jrealm.game.model.ProjectileEffect> effects;

    private boolean invert = false;

    private long timeStep = 0;
    private short amplitude = 4;
    private short frequency = 25;

    // Orbital projectile: orbits around a fixed center point
    private float orbitCenterX;
    private float orbitCenterY;
    private float orbitRadius;
    private float orbitPhase; // starting angle in radians for this projectile

    private long createdTime;
    private long lastUpdateNanos = System.nanoTime();

    public Bullet() {
    	super(0l,null,0);
    }
    public Bullet(long id, int bulletId, Vector2f origin, int size) {
        super(id, origin, size);
        this.flags = new ArrayList<>();
        this.createdTime = Instant.now().toEpochMilli();
    }

    public Bullet(long id, int projectileId, Vector2f origin, int size, float angle, float magnitude, float range,
            short damage, boolean isEnemy, boolean playerHit, boolean enemyHit, List<Short> flags, boolean invert,
            long timeStep, short amplitude, short frequency) {
        super(id, origin, size);
        this.projectileId = projectileId;
        this.angle = angle;
        this.magnitude = magnitude;
        this.range = range;
        this.damage = damage;
        this.isEnemy = isEnemy;
        this.playerHit = playerHit;
        this.enemyHit = enemyHit;
        this.flags = flags;
        this.invert = invert;
        this.timeStep = timeStep;
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.createdTime = Instant.now().toEpochMilli();
    }

    public Bullet(long id, int projectileId, Vector2f origin, Vector2f dest, short size, float magnitude, float range,
            short damage, boolean isEnemy) {
        super(id, origin, size);
        this.projectileId = projectileId;
        this.magnitude = magnitude;
        this.range = range;
        this.damage = damage;
        this.angle = -Bullet.getAngle(origin, dest);
        this.isEnemy = isEnemy;
        this.flags = new ArrayList<>();
        this.createdTime = Instant.now().toEpochMilli();
    }

    public Bullet(long id, int projectileId, Vector2f origin, Vector2f dest, short size, float magnitude, float range,
            short damage, short amplitude, short frequency, boolean isEnemy) {
        super(id, origin, size);
        this.projectileId = projectileId;
        this.magnitude = magnitude;
        this.range = range;
        this.damage = damage;
        this.angle = -Bullet.getAngle(origin, dest);
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.isEnemy = isEnemy;
        this.flags = new ArrayList<>();
        this.createdTime = Instant.now().toEpochMilli();
    }

    public Bullet(long id, int projectileId, Vector2f origin, float angle, short size, float magnitude, float range,
            short damage, boolean isEnemy) {
        super(id, origin, size);
        this.projectileId = projectileId;
        this.magnitude = magnitude;
        this.range = range;
        this.damage = damage;
        this.angle = -angle;
        this.isEnemy = isEnemy;
        this.flags = new ArrayList<>();
        this.createdTime = Instant.now().toEpochMilli();
    }

    public static float getAngle(Vector2f source, Vector2f target) {
        double angle = (Math.atan2(target.y - source.y, target.x - source.x));
        angle -= Math.PI / 2;
        return (float) angle;
    }

    public boolean hasFlag(short flag) {
        return (this.flags != null) && (this.flags.contains(flag));
    }
    
    public boolean hasFlag(ProjectileFlag flag) {
        return (this.flags != null) && (this.flags.contains(flag.flagId));
    }

    public boolean hasFlag(StatusEffectType flag) {
        return (this.flags != null) && (this.flags.contains(flag.effectId));
    }

    public boolean isEnemy() {
        return this.isEnemy;
    }

    public float getAngle() {
        return this.angle;
    }

    public float getMagnitude() {
        return this.magnitude;
    }

    // TODO: Remove static 10s lifetime
    public boolean remove() {
        final boolean timeExp = ((Instant.now().toEpochMilli()) - this.createdTime) > 10000;
        return ((this.range <= 0.0) || timeExp);
    }

    public short getDamage() {
        return this.damage;
    }

    @Override
    // Update for regular non Parametric bullets.
    // Uses delta-time scaling (dt * 64) to match the web client's frame-rate
    // independent approach. This ensures consistent bullet speed regardless of
    // whether the 64Hz tick loop drifts due to OS timer resolution.
    public void update() {
        final long now = System.nanoTime();
        final float dt = Math.min((now - this.lastUpdateNanos) / 1_000_000_000.0f, 0.1f);
        this.lastUpdateNanos = now;
        final float bulletScale = dt * 64.0f;

        if (this.hasFlag(ProjectileFlag.ORBITAL)) {
            this.updateOrbital(bulletScale);
        } else if (this.hasFlag(ProjectileFlag.PARAMETRIC)
                || this.hasFlag(ProjectileFlag.INVERTED_PARAMETRIC)) {
            this.updateParametric(bulletScale);
        } else {
            // Regular straight line projectile
            final float velX = (float) (Math.sin(this.angle) * this.magnitude * bulletScale);
            final float velY = (float) (Math.cos(this.angle) * this.magnitude * bulletScale);
            final double dist = Math.sqrt((velX * velX) + (velY * velY));
            this.range -= dist;
            this.pos.addX(velX);
            this.pos.addY(velY);
            this.dx = velX;
            this.dy = velY;
        }
    }

    /**
     * Parametric projectile update - applies sinusoidal oscillation perpendicular
     * to the direction of travel, creating wavy projectile patterns (e.g. RotMG staff shots).
     *
     * The oscillation is computed as a position offset along the perpendicular axis,
     * so each tick we apply the CHANGE in offset (delta) rather than a raw velocity.
     * Negative amplitude naturally inverts the wave (no special flag needed).
     *
     * Perpendicular axis to forward (sin(a), cos(a)) is (cos(a), -sin(a)).
     */
    public void updateParametric(float bulletScale) {
        // Compute perpendicular offset BEFORE advancing timeStep
        float prevOffset = (float) (this.amplitude * Math.sin(Math.toRadians(this.timeStep)));

        this.timeStep = (long) ((this.timeStep + this.frequency * bulletScale) % 360);

        float currOffset = (float) (this.amplitude * Math.sin(Math.toRadians(this.timeStep)));
        float perpDelta = (currOffset - prevOffset) * (this.invert ? -1 : 1);

        // Forward velocity along the travel direction
        float forwardX = (float) (Math.sin(this.angle) * this.magnitude * bulletScale);
        float forwardY = (float) (Math.cos(this.angle) * this.magnitude * bulletScale);

        // Perpendicular direction (90 degrees from forward)
        float perpX = (float) Math.cos(this.angle);
        float perpY = (float) -Math.sin(this.angle);

        // Combine forward motion + perpendicular oscillation
        float velX = forwardX + perpX * perpDelta;
        float velY = forwardY + perpY * perpDelta;

        // Decrease range by forward distance only (not oscillation)
        this.range -= this.magnitude * bulletScale;

        this.pos.addX(velX);
        this.pos.addY(velY);
        this.dx = velX;
        this.dy = velY;
    }

    /**
     * Orbital projectile update — positions the bullet on a circle around orbitCenter.
     * Uses frequency as angular speed (degrees/tick) and amplitude as orbit radius.
     * The initial angle for each projectile in the ring is set via orbitPhase.
     */
    public void updateOrbital(float bulletScale) {
        this.orbitPhase += (float) Math.toRadians(this.frequency * bulletScale);
        float newX = this.orbitCenterX + this.orbitRadius * (float) Math.cos(this.orbitPhase);
        float newY = this.orbitCenterY + this.orbitRadius * (float) Math.sin(this.orbitPhase);
        this.dx = newX - this.pos.x;
        this.dy = newY - this.pos.y;
        this.pos.x = newX;
        this.pos.y = newY;
        // Decrease range by arc length traveled per tick
        this.range -= this.orbitRadius * Math.abs(Math.toRadians(this.frequency * bulletScale));
    }

    /**
     * Configure this bullet as an orbital projectile.
     * @param centerX orbit center X
     * @param centerY orbit center Y
     * @param radius orbit radius in pixels
     * @param startPhase starting angle in radians (evenly spaced for ring patterns)
     */
    public void setupOrbital(float centerX, float centerY, float radius, float startPhase) {
        this.orbitCenterX = centerX;
        this.orbitCenterY = centerY;
        this.orbitRadius = radius;
        this.orbitPhase = startPhase;
        // Set initial position on the orbit
        this.pos.x = centerX + radius * (float) Math.cos(startPhase);
        this.pos.y = centerY + radius * (float) Math.sin(startPhase);
    }

    @Override
    public void render(SpriteBatch batch) {
        if (this.getSpriteSheet() == null) return;
        TextureRegion frame = this.getSpriteSheet().getCurrentFrame();
        if (frame == null) return;

        final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(this.getProjectileId());
        final float angleOffset = Float.parseFloat(group.getAngleOffset());

        // Convert angle to degrees for LibGDX (counter-clockwise positive)
        float rotationDeg;
        if (angleOffset > 0.0f) {
            rotationDeg = (float) Math.toDegrees(-this.getAngle() + (this.tfAngle + angleOffset));
        } else {
            rotationDeg = (float) Math.toDegrees(-this.getAngle() + this.tfAngle);
        }

        float wx = this.pos.getWorldVar().x;
        float wy = this.pos.getWorldVar().y;
        float halfSize = this.size / 2f;

        // draw with rotation around center
        batch.draw(frame, wx, wy, halfSize, halfSize, this.size, this.size, 1f, 1f, rotationDeg);
    }
}
