package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.math.Vector2f;
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
	private Long id;
	@SerializableField(order = 1, type = SerializableInt.class)
	private Integer projectileId;
	@SerializableField(order = 2, type = SerializableShort.class)
	private Short size;
	@SerializableField(order = 3, type = Vector2f.class)
	private Vector2f pos;
	@SerializableField(order = 4, type = SerializableFloat.class)
	private Float dX;
	@SerializableField(order = 5, type = SerializableFloat.class)
	private Float dY;
	@SerializableField(order = 6, type = SerializableFloat.class)
	private Float angle;
	@SerializableField(order = 7, type = SerializableFloat.class)
	private Float magnitude;
	@SerializableField(order = 8, type = SerializableFloat.class)
	private Float range;
	@SerializableField(order = 9, type = SerializableShort.class)
	private Short damage;
	@SerializableField(order = 10, type = SerializableShort.class, isCollection=true)
	private Short[] flags;
	
	@Override
	public NetBullet read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetBullet value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}
}
