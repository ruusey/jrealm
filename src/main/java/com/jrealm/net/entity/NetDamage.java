package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableShort;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Streamable
@AllArgsConstructor
public class NetDamage extends SerializableFieldType<NetDamage> {
	@SerializableField(order = 0, type = SerializableInt.class)
	private Integer projectileGroupId;
	@SerializableField(order = 1, type = SerializableShort.class)
	private Short min;
	@SerializableField(order = 2, type = SerializableShort.class)
	private Short max;
 
	public NetDamage() {
		this.projectileGroupId = -1;
		this.min = -1;
		this.max =-1;
	}
	@Override
	public NetDamage read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetDamage value, DataOutputStream stream) throws Exception {
		NetDamage toWrite = value == null ? new NetDamage() : value;
		IOService.writeStream(toWrite, stream);
	}
}
