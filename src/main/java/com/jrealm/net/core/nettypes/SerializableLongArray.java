package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableLongArray extends SerializableFieldType<Long[]>{

	@Override
	public Long[] read(DataInputStream stream) throws Exception {
		final int size = stream.readInt();
		final Long[] data = new Long[size];
		for (int i = 0; i < size; i++) {
			data[i] = stream.readLong();
		}
		return data;
	}

	@Override
	public void write(Long[] value, DataOutputStream stream) throws Exception {
		stream.writeInt(value.length);
		for (int i = 0; i < value.length; i++) {
			stream.writeLong(value[i]);
		}
	}
}
