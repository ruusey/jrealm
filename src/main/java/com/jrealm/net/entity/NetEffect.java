package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.EffectType;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableShort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Streamable
@AllArgsConstructor
@NoArgsConstructor
public class NetEffect extends SerializableFieldType<NetEffect> {
	@SerializableField(order = 0, type = SerializableBoolean.class)
	private boolean self;
	@SerializableField(order = 1, type = SerializableShort.class)
	private short effectId;
	@SerializableField(order = 2, type = SerializableLong.class)
	private long duration;
	@SerializableField(order = 3, type = SerializableLong.class)
	private long cooldownDuration;
	@SerializableField(order = 4, type = SerializableShort.class)
	private short mpCost;

	@Override
	public NetEffect read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetEffect value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}

	public Effect asEffect() {
		return Effect.builder().self(this.self).effectId(EffectType.valueOf(this.effectId)).duration(this.duration)
				.cooldownDuration(this.cooldownDuration).mpCost(this.mpCost).build();
	}
}
