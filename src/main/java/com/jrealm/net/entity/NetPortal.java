package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableFloat;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableShort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Streamable
public class NetPortal extends SerializableFieldType<NetPortal> {
	@SerializableField(order = 0, type = SerializableLong.class)
	private long id;
	@SerializableField(order = 1, type = SerializableShort.class)
	private short portalId;
	@SerializableField(order = 2, type = SerializableLong.class)
	private long fromRealmId;
	@SerializableField(order = 3, type = SerializableLong.class)
	private long toRealmId;
	@SerializableField(order = 4, type = SerializableLong.class)
	private long expires;
	@SerializableField(order = 5, type = SerializableFloat.class)
	private float posX;
	@SerializableField(order = 6, type = SerializableFloat.class)
	private float posY;

	@Override
	public NetPortal read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetPortal value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}

}
