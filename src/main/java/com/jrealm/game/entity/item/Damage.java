package com.jrealm.game.entity.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jrealm.net.packet.client.temp.Streamable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Damage implements Streamable<Damage>{
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

	@Override
	public Damage read(DataInputStream stream) throws Exception {
		int projectileGroupId = stream.readInt();
		short min = stream.readShort();
		short max = stream.readShort();
		
		return new Damage(projectileGroupId, min, max);
	}

	@Override
	public void write(DataOutputStream stream) throws Exception {
		stream.writeInt(this.projectileGroupId);
		stream.writeShort(this.min);
		stream.writeShort(this.max);
		
	}
}
