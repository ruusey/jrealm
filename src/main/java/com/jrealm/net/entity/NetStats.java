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

@Data
@Streamable
@AllArgsConstructor
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
	
	public NetStats() {
		this.hp = 0;
		this.mp = 0;
		this.def = 0;
		this.att = 0;
		this.spd = 0;
		this.dex = 0;
		this.vit = 0;
		this.wis = 0;
	}
	
	@Override
	public NetStats read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public int write(NetStats value, DataOutputStream stream) throws Exception {
		final NetStats toWrite = value == null ? new NetStats() : value;
		return IOService.writeStream(toWrite, stream);
	}

	public Stats asStats() {
		return Stats.builder().hp(hp).mp(mp).def(def).att(att).spd(spd).dex(dex).vit(vit).wis(wis).build();
	}

}
