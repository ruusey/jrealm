package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.item.Stats;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableShort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Streamable
@AllArgsConstructor
@NoArgsConstructor
public class NetStats extends SerializableFieldType<NetStats> {
	@SerializableField(order = 0, type = SerializableShort.class)
	private short hp;
	@SerializableField(order = 1, type = SerializableShort.class)
	private short mp;
	@SerializableField(order = 2, type = SerializableShort.class)
	private short def;
	@SerializableField(order = 3, type = SerializableShort.class)
	private short att;
	@SerializableField(order = 4, type = SerializableShort.class)
	private short spd;
	@SerializableField(order = 5, type = SerializableShort.class)
	private short dex;
	@SerializableField(order = 6, type = SerializableShort.class)
	private short vit;
	@SerializableField(order = 7, type = SerializableShort.class)
	private short wis;

	@Override
	public NetStats read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetStats value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}
}
