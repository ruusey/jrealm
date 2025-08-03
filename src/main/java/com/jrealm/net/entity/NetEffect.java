package com.jrealm.net.entity;

import com.jrealm.game.contants.EffectType;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableShort;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Streamable
@AllArgsConstructor
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

	public NetEffect() {
		this.self = false;
		this.effectId = -1;
		this.duration = -1l;
		this.cooldownDuration = -1l;
		this.mpCost = -1;
	}

	public Effect asEffect() {
		return Effect.builder().self(this.self).effectId(EffectType.valueOf(this.effectId)).duration(this.duration)
				.cooldownDuration(this.cooldownDuration).mpCost(this.mpCost).build();
	}

}
