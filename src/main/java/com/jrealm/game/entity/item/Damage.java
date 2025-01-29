package com.jrealm.game.entity.item;

import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jrealm.net.core.nettypes.game.SerializableDamage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Damage extends SerializableDamage {
    private int projectileGroupId;
    private short min;
    private short max;

    @JsonIgnore
    public short getInRange() {
        Random r = new Random(System.nanoTime());
        return (short) (r.nextInt(this.max - this.min) + this.min);
    }

    public static short getInRange(Damage d) {
        Random r = new Random(System.nanoTime());
        return (short) (r.nextInt(d.getMax() - d.getMin()) + d.getMin());
    }

    @Override
    public Damage clone() {
        return Damage.builder().projectileGroupId(this.projectileGroupId).min(this.min).max(this.max).build();
    }
}
