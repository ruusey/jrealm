package com.openrealm.game.ui;

import com.openrealm.game.math.Vector2f;
import com.openrealm.net.client.packet.CreateEffectPacket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client-side visual effect spawned by CreateEffectPacket.
 * Each effect has a type, position, duration, and tracks its own lifecycle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveVisualEffect {
    private short effectType;
    private float posX;
    private float posY;
    private float radius;
    private float targetPosX;
    private float targetPosY;
    /** Total duration in milliseconds */
    private short duration;
    /** Time elapsed in milliseconds */
    @Builder.Default
    private float elapsed = 0f;
    @Builder.Default
    private boolean remove = false;

    public static ActiveVisualEffect from(CreateEffectPacket packet) {
        return ActiveVisualEffect.builder()
                .effectType(packet.getEffectType())
                .posX(packet.getPosX())
                .posY(packet.getPosY())
                .radius(packet.getRadius())
                .targetPosX(packet.getTargetPosX())
                .targetPosY(packet.getTargetPosY())
                .duration(packet.getDuration())
                .build();
    }

    /**
     * @return normalized progress [0..1] where 0 = just started, 1 = finished
     */
    public float getProgress() {
        if (this.duration <= 0) return 1f;
        return Math.min(this.elapsed / this.duration, 1f);
    }

    public void update(float deltaMs) {
        this.elapsed += deltaMs;
        if (this.elapsed >= this.duration) {
            this.remove = true;
        }
    }

    public boolean isAoe() {
        // Line effects have radius=0 and target positions set
        if (this.radius == 0 && (this.targetPosX != 0 || this.targetPosY != 0)) return false;
        return true;
    }

    public boolean getRemove() {
        return this.remove;
    }
}
