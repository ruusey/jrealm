package com.jrealm.game.entity;

import java.awt.Graphics2D;

import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.client.packet.ObjectMovement;

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

    public void applyMovementLerp(ObjectMovement packet, float pct) {
        final float lerpX = this.lerp(this.pos.x, packet.getPosX(), pct);
        final float lerpY = this.lerp(this.pos.y, packet.getPosY(), pct);

        this.pos = new Vector2f(lerpX, lerpY);
        this.bounds = new Rectangle(this.pos, this.size, this.size);
        this.dx = packet.getVelX();
        this.dy = packet.getVelY();
    }

    public void applyMovementLerp(ObjectMovement packet) {
        final float lerpX = this.lerp(this.pos.x, packet.getPosX(), 0.65f);
        final float lerpY = this.lerp(this.pos.y, packet.getPosY(), 0.65f);

        this.pos = new Vector2f(lerpX, lerpY);
        this.bounds = new Rectangle(this.pos, this.size, this.size);
        this.dx = packet.getVelX();
        this.dy = packet.getVelY();
    }

    public void applyMovement(ObjectMovement packet) {
        this.pos = new Vector2f(packet.getPosX(), packet.getPosY());
        this.bounds = new Rectangle(this.pos, this.size, this.size);
        this.dx = packet.getVelX();
        this.dy = packet.getVelY();
    }

    private float lerp(float start, float end, float pct) {
        return (start + ((end - start) * pct));
    }

    // Returns the players position adjusted to the center of its
    // on-screen sprite image
    public Vector2f getCenteredPosition() {
        return this.pos.clone((this.getSize() / 2), this.getSize() / 2);
    }

    @Override
    public Vector2f clone() {
        Vector2f newVector = new Vector2f(this.pos.x, this.pos.y);
        return newVector;
    }

    public void render(Graphics2D g) {
        if (this.spriteSheet == null) {
            GameObject.log.warn("GameObject {} does not have a sprite sheet!");
            return;
        }
        g.drawImage(this.spriteSheet.getCurrentFrame(), (int) (this.pos.getWorldVar().x),
                (int) (this.pos.getWorldVar().y), this.size, this.size, null);
    }

    @Override
    public String toString() {
        return "$" + this.name;
    }

}