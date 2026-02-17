package com.jrealm.game.entity;

import java.time.Instant;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.jrealm.game.contants.ProjectileEffectType;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class Entity extends GameObject {
    protected boolean up = false;
    protected boolean down = false;
    protected boolean right = false;
    protected boolean left = false;
    protected boolean attack = false;
    protected String lastAnimSet = "idle_side";

    public boolean xCol = false;
    public boolean yCol = false;

    protected int attackSpeed = 1050;
    protected int attackDuration = 650;
    protected double attacktime;
    protected boolean canAttack = true;
    protected boolean attacking = false;

    public int health = 100;
    public int mana = 100;
    public float healthpercent = 1;
    public float manapercent = 1;

    protected Rectangle hitBounds;

    private Short[] effectIds;
    private Long[] effectTimes;

    public Entity(long id, Vector2f origin, int size) {
        super(id, origin, size);
        this.hitBounds = new Rectangle(origin, size, size);
        this.resetEffects();
    }

    public void removeEffect(short effectId) {
        for (int i = 0; i < this.effectIds.length; i++) {
            if (this.effectIds[i] == effectId) {
                this.effectIds[i] = (short) -1;
                this.effectTimes[i] = (long) -1;
            }
        }
    }

    public void removeExpiredEffects() {
        for (int i = 0; i < this.effectIds.length; i++) {
            if (this.effectIds[i] != -1) {
                if (Instant.now().toEpochMilli() > this.effectTimes[i]) {
                    this.effectIds[i] = (short) -1;
                    this.effectTimes[i] = (long) -1;
                }
            }
        }
    }

    public boolean hasEffect(ProjectileEffectType effect) {
        if (this.effectIds == null)
            return false;
        for (int i = 0; i < this.effectIds.length; i++) {
            if (this.effectIds[i] == effect.effectId)
                return true;
        }
        return false;
    }

    public boolean hasNoEffects() {
        for (int i = 0; i < this.effectIds.length; i++) {
            if (this.effectIds[i] > -1)
                return false;
        }
        return true;
    }

    public void resetEffects() {
        this.effectIds = new Short[] { -1, -1, -1, -1, -1, -1, -1, -1 };
        this.effectTimes = new Long[] { -1l, -1l, -1l, -1l, -1l, -1l, -1l, -1l };
    }

    public void addEffect(ProjectileEffectType effect, long duration) {
        for (int i = 0; i < this.effectIds.length; i++) {
            if (this.effectIds[i] == -1) {
                this.effectIds[i] = effect.effectId;
                this.effectTimes[i] = (Instant.now().toEpochMilli() + duration);
                return;
            }
        }
    }

    public boolean getDeath() {
        return this.health <= 0;
    }

    public int getDirection() {
        if ((this.isUp()) || (this.isLeft()))
            return 1;
        return -1;
    }

    public void update(double time) {
        if (this.getSpriteSheet() != null) {
            this.getSpriteSheet().animate();
        }
    }

    public void updateAnimation() {
        if (this.dx > 0) {
            this.right = true;
        } else if (this.dx < 0) {
            this.left = true;
        } else {
            this.right = false;
            this.left = false;
        }

        if (this.dy > 0) {
            this.down = true;
        } else if (this.dy < 0) {
            this.up = true;
        } else {
            this.down = false;
            this.up = false;
        }

        // Select animation set based on movement state
        if (this.getSpriteSheet() != null && this.getSpriteSheet().hasAnimSets()) {
            String targetAnim;
            if (this.attacking) {
                targetAnim = (this.up || this.down) ? "attack_front" : "attack_side";
            } else if (this.left || this.right) {
                targetAnim = "walk_side";
            } else if (this.up || this.down) {
                targetAnim = "walk_front";
            } else {
                // Idle: keep facing the same direction as last movement
                if (this.lastAnimSet != null && this.lastAnimSet.contains("front")) {
                    targetAnim = "idle_front";
                } else {
                    targetAnim = "idle_side";
                }
            }
            this.lastAnimSet = targetAnim;
            this.getSpriteSheet().setAnimSet(targetAnim);
        }
    }

    @Override
    public abstract void render(SpriteBatch batch);
}
