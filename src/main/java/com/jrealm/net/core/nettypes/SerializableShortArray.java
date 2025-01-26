package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableShortArray extends SerializableFieldType<Short[]> {

	@Override
	public Short[] read(DataInputStream stream) throws Exception {
		final int size = stream.readInt();
		final Short[] data = new Short[size];
		for (int i = 0; i < size; i++) {
			data[i] = stream.readShort();
		}
		return data;
	}

	@Override
	public void write(Short[] value, DataOutputStream stream) throws Exception {
		stream.writeInt(value.length);
		for (int i = 0; i < value.length; i++) {
			stream.writeShort(value[i]);
		}
	}
}
