package com.jrealm.game.entity.item;

import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Damage {
	private int projectileGroupId;
	private short min;
	private short max;

	@JsonIgnore
	public short getInRange() {
		Random r = new Random(System.currentTimeMillis());
		return (short) (r.nextInt(this.max - this.min) + this.min);
	}

	public static short getInRange(Damage d) {
		Random r = new Random(System.currentTimeMillis());
		return (short) (r.nextInt(d.getMax() - d.getMin()) + d.getMin());
	}
}
