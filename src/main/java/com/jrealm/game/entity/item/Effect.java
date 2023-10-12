package com.jrealm.game.entity.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.EffectType;
import com.jrealm.net.Streamable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Effect implements Streamable<Effect> {
	private boolean self;
	private EffectType effectId;
	private long duration;
	private short mpCost;

	@Override
	public Effect read(DataInputStream stream) throws Exception {
		boolean self = stream.readBoolean();
		short effectId = stream.readShort();
		long duration = stream.readLong();
		short mpCost = stream.readShort();

		return new Effect(self, EffectType.valueOf(effectId), duration, mpCost);
	}

	@Override
	public void write(DataOutputStream stream) throws Exception {
		stream.writeBoolean(this.self);
		stream.writeShort(this.effectId.effectId);
		stream.writeLong(this.duration);
		stream.writeShort(this.mpCost);
		
	}

}
