package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableByte extends SerializableFieldType<Byte> {

	@Override
	public Byte read(DataInputStream stream) throws Exception {
		return stream.readByte();
	}

	@Override
	public void write(Byte value, DataOutputStream stream) throws Exception {
		stream.writeByte(value);
	}
}
