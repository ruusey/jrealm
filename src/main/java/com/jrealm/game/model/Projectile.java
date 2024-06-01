package com.jrealm.game.model;

import java.util.List;

import com.jrealm.game.contants.ProjectilePositionMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projectile {
    private int projectileId;
    private ProjectilePositionMode positionMode;
    private String angle;
    private short range;
    private float magnitude;
    private short size;
    private short damage;

    private short amplitude;
    private short frequency;

    private List<Short> flags;

    public boolean hasFlag(short flag) {
	return (this.flags != null) && this.flags.contains(flag);
    }

}
