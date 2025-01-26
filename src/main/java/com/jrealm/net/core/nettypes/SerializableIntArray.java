package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableIntArray extends SerializableFieldType<Integer[]> {

	@Override
	public Integer[] read(DataInputStream stream) throws Exception {
		final int size = stream.readInt();
		final Integer[] data = new Integer[size];
		for (int i = 0; i < size; i++) {
			data[i] = stream.readInt();
		}
		return data;
	}

	@Override
	public void write(Integer[] value, DataOutputStream stream) throws Exception {
		stream.writeInt(value.length);
		for (int i = 0; i < value.length; i++) {
			stream.writeInt(value[i]);
		}
	}
}
