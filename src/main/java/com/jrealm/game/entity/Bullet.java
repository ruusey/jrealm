package com.jrealm.game.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.contants.ProjectileEffectType;
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

    private List<Short> flags;

    private boolean invert = false;

    private long timeStep = 0;
    private short amplitude = 4;
    private short frequency = 25;

    private long createdTime;
    
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
    
    public boolean hasFlag(ProjectileEffectType flag) {
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
    // Update for regular non Parametric bullets
    public void update() {
        // if is flagged to be rendered as a parametric projectile
        if (this.hasFlag(ProjectileEffectType.PARAMETRIC_PROJECTILE)) {
            this.updateParametric();
        } else {
            // Regular straight line projectile
            final Vector2f vel = new Vector2f((float) (Math.sin(this.angle) * this.magnitude),
                    (float) (Math.cos(this.angle) * this.magnitude));
            final double dist = Math.sqrt((vel.x * vel.x) + (vel.y * vel.y));
            this.range -= dist;
            this.pos.addX(vel.x);
            this.pos.addY(vel.y);
            this.dx = vel.x;
            this.dy = vel.y;
        }
    }

    public void updateParametric() {
        this.timeStep = (this.timeStep + this.frequency) % 360;

        final Vector2f vel = new Vector2f((float) (Math.sin(this.angle) * this.magnitude),
                (float) (Math.cos(this.angle) * this.magnitude));
        final double dist = Math.sqrt((vel.x * vel.x) + (vel.y * vel.y));
        this.range -= dist;
        // 'invert'
        if (this.hasFlag(ProjectileEffectType.INVERTED_PARAMETRIC_PROJECTILE)) {
            double shift = -this.amplitude * Math.sin(Math.toRadians(this.timeStep));
            double shift2 = this.amplitude * Math.cos(Math.toRadians(this.timeStep));
            float velX = (float) (vel.x + shift2);
            float velY = (float) (vel.y + shift);
            this.pos.addX(velX);
            this.pos.addY(velY);
            this.dx = velX;
            this.dy = velY;
        } else {
            double shift = this.amplitude * Math.sin(Math.toRadians(this.timeStep));
            double shift2 = this.amplitude * Math.cos(Math.toRadians(this.timeStep));
            float velX = (float) (vel.x + shift2);
            float velY = (float) (vel.y + shift);
            this.pos.addX(velX);
            this.pos.addY(velY);
            this.dx = velX;
            this.dy = velY;
        }
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
