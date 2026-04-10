package com.openrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.openrealm.net.Streamable;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.SerializableFieldType;
import com.openrealm.net.core.nettypes.SerializableInt;
import com.openrealm.net.core.nettypes.SerializableShort;

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

	@Override
	public int write(NetDamage value, DataOutputStream stream) throws Exception {
		final NetDamage v = value == null ? new NetDamage() : value;
		stream.writeInt(v.projectileGroupId);
		stream.writeShort(v.min);
		stream.writeShort(v.max);
		return 8;
	}

	@Override
	public NetDamage read(DataInputStream stream) throws Exception {
		return new NetDamage(stream.readInt(), stream.readShort(), stream.readShort());
	}
}
