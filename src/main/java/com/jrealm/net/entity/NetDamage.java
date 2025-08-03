package com.jrealm.net.entity;

import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableShort;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Streamable
@AllArgsConstructor
public class NetDamage extends SerializableFieldType<NetDamage> {
	@SerializableField(order = 0, type = SerializableInt.class)
	private int projectileGroupId;
	@SerializableField(order = 1, type = SerializableShort.class)
	private short min;
	@SerializableField(order = 2, type = SerializableShort.class)
	private short max;
 
	public NetDamage() {
		this.projectileGroupId = -1;
		this.min = -1;
		this.max =-1;
	}
}
