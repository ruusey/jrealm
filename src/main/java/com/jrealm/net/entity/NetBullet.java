package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.*;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Streamable
public class NetBullet extends SerializableFieldType<NetBullet> {
	@SerializableField(order = 0, type = SerializableLong.class)
	private long id;
	@SerializableField(order = 1, type = SerializableLong.class)
	private int projectileId;
	@SerializableField(order = 2, type = SerializableShort.class)
	private short size;
	@SerializableField(order = 3, type = SerializableFloat.class)
	private float posX;
	@SerializableField(order = 4, type = SerializableFloat.class)
	private float posY;
	@SerializableField(order = 5, type = SerializableFloat.class)
	private float dX;
	@SerializableField(order = 6, type = SerializableFloat.class)
	private float dY;
	@SerializableField(order = 7, type = SerializableFloat.class)
	private float angle;
	@SerializableField(order = 8, type = SerializableFloat.class)
	private float magnitude;
	@SerializableField(order = 9, type = SerializableFloat.class)
	private float range;
	@SerializableField(order = 10, type = SerializableShort.class)
	private short damage;
	@SerializableField(order = 11, type = SerializableShortArray.class)
	private short[] flags;
	
	@Override
	public NetBullet read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetBullet value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}
}
