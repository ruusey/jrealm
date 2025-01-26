package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableShort extends SerializableFieldType<Short> {

	@Override
	public Short read(DataInputStream stream) throws Exception {
		return stream.readShort();
	}

	@Override
	public void write(Short value, DataOutputStream stream) throws Exception {
		stream.writeShort(value);
	}
}
