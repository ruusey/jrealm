package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableObject extends SerializableFieldType<Byte[]> {

	@Override
	public Byte[] read(DataInputStream stream) throws Exception {
		final int size = stream.readInt();
		final Byte[] data = new Byte[size];
		for (int i = 0; i < size; i++) {
			data[i] = stream.readByte();
		}
		return data;
	}

	@Override
	public void write(Byte[] value, DataOutputStream stream) throws Exception {
		stream.writeInt(value.length);
		for (int i = 0; i < value.length; i++) {
			stream.writeByte(value[i]);
		}
	}
}
