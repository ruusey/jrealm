package com.jrealm.game.entity;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.net.Streamable;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Bullet extends GameObject  {
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
        if (this.hasFlag((short) 12)) {
            this.update(1);
        } else {
            // Regular straight line projectile
            Vector2f vel = new Vector2f((float) (Math.sin(this.angle) * this.magnitude),
                    (float) (Math.cos(this.angle) * this.magnitude));
            double dist = Math.sqrt((vel.x * vel.x) + (vel.y * vel.y));
            this.range -= dist;
            this.pos.addX(vel.x);
            this.pos.addY(vel.y);
            this.dx = vel.x;
            this.dy = vel.y;
        }
    }

    public void update(int i) {
        this.timeStep = (this.timeStep + this.frequency) % 360;

        Vector2f vel = new Vector2f((float) (Math.sin(this.angle) * this.magnitude),
                (float) (Math.cos(this.angle) * this.magnitude));
        double dist = Math.sqrt((vel.x * vel.x) + (vel.y * vel.y));
        this.range -= dist;
        // 'invert'
        if (this.hasFlag((short) 13)) {
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
    public void render(Graphics2D g) {
        AffineTransform original = g.getTransform();
        AffineTransform t = new AffineTransform();
        ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(this.getProjectileId());
        float angleOffset = Float.parseFloat(group.getAngleOffset());
        if (angleOffset > 0.0f) {
            t.rotate(-this.getAngle() + (this.tfAngle + angleOffset), this.pos.getWorldVar().x + (this.size / 2),
                    this.pos.getWorldVar().y + (this.size / 2));
        } else {
            t.rotate(-this.getAngle() + this.tfAngle, this.pos.getWorldVar().x + (this.size / 2),
                    this.pos.getWorldVar().y + (this.size / 2));
        }

        g.setTransform(t);
        g.drawImage(this.getSpriteSheet().getCurrentFrame(), (int) (this.pos.getWorldVar().x),
                (int) (this.pos.getWorldVar().y), this.size, this.size, null);
        g.setTransform(original);
    }

//    @Override
//    public void write(DataOutputStream stream) throws Exception {
//        stream.writeLong(this.id);
//        stream.writeInt(this.projectileId);
//        stream.writeInt(this.size);
//        stream.writeFloat(this.pos.x);
//        stream.writeFloat(this.pos.y);
//        stream.writeFloat(this.dx);
//        stream.writeFloat(this.dy);
//        stream.writeFloat(this.angle);
//        stream.writeFloat(this.magnitude);
//        stream.writeFloat(this.range);
//        stream.writeShort(this.damage);
//        stream.writeBoolean(this.isEnemy);
//        stream.writeBoolean(this.playerHit);
//        stream.writeBoolean(this.enemyHit);
//        stream.writeBoolean(this.invert);
//        stream.writeInt(this.flags.size());
//        for (short s : this.flags) {
//            stream.writeShort(s);
//        }
//        stream.writeLong(this.timeStep);
//        stream.writeShort(this.amplitude);
//        stream.writeShort(this.frequency);
//    }
//
//    @Override
//    public Bullet read(DataInputStream stream) throws Exception {
//        final long id = stream.readLong();
//        final int bulletId = stream.readInt();
//        final int size = stream.readInt();
//        final float posX = stream.readFloat();
//        final float posY = stream.readFloat();
//        final float dY = stream.readFloat();
//        final float dX = stream.readFloat();
//        final float angle = stream.readFloat();
//        final float magnitude = stream.readFloat();
//        final float range = stream.readFloat();
//        final short damage = stream.readShort();
//        final boolean isEnemy = stream.readBoolean();
//        final boolean playerHit = stream.readBoolean();
//        final boolean enemyHit = stream.readBoolean();
//        final boolean invert = stream.readBoolean();
//        final int flagsSize = stream.readInt();
//        final short[] flags = new short[flagsSize];
//        for (int i = 0; i < flagsSize; i++) {
//            flags[i] = stream.readShort();
//        }
//        final long timeStep = stream.readLong();
//        final short amplitude = stream.readShort();
//        final short frequency = stream.readShort();
//        List<Short> flagsList = new ArrayList<>();
//        for (short s : flags) {
//            flagsList.add(s);
//        }
//        Bullet newBullet = new Bullet(id, bulletId, new Vector2f(posX, posY), size, angle, magnitude, range, damage,
//                isEnemy, playerHit, enemyHit, flagsList, invert, timeStep, amplitude, frequency);
//        newBullet.setDx(dX);
//        newBullet.setDy(dY);
//        return newBullet;
//    }

    public static Bullet fromStream(DataInputStream stream) throws Exception {
        final long id = stream.readLong();
        final int bulletId = stream.readInt();
        final int size = stream.readInt();
        final float posX = stream.readFloat();
        final float posY = stream.readFloat();
        final float dY = stream.readFloat();
        final float dX = stream.readFloat();
        final float angle = stream.readFloat();
        final float magnitude = stream.readFloat();
        final float range = stream.readFloat();
        final short damage = stream.readShort();
        final boolean isEnemy = stream.readBoolean();
        final boolean playerHit = stream.readBoolean();
        final boolean enemyHit = stream.readBoolean();
        final boolean invert = stream.readBoolean();
        final int flagsSize = stream.readInt();
        final short[] flags = new short[flagsSize];
        for (int i = 0; i < flagsSize; i++) {
            flags[i] = stream.readShort();
        }
        final long timeStep = stream.readLong();
        final short amplitude = stream.readShort();
        final short frequency = stream.readShort();
        List<Short> flagsList = new ArrayList<>();
        for (short s : flags) {
            flagsList.add(s);
        }
        Bullet newBullet = new Bullet(id, bulletId, new Vector2f(posX, posY), size, angle, magnitude, range, damage,
                isEnemy, playerHit, enemyHit, flagsList, invert, timeStep, amplitude, frequency);
        newBullet.setDx(dX);
        newBullet.setDy(dY);
        return newBullet;
    }
}
