package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.math.Vector2f;
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
	private Long id;
	@SerializableField(order = 1, type = SerializableShort.class)
	private Short portalId;
	@SerializableField(order = 2, type = SerializableLong.class)
	private Long fromRealmId;
	@SerializableField(order = 3, type = SerializableLong.class)
	private Long toRealmId;
	@SerializableField(order = 4, type = SerializableLong.class)
	private Long expires;
	@SerializableField(order = 5, type = Vector2f.class)
	private Vector2f pos;

	@Override
	public NetPortal read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetPortal value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}

}
