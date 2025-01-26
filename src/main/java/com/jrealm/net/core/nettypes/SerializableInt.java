package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableInt extends SerializableFieldType<Integer> {

	@Override
	public Integer read(DataInputStream stream) throws Exception {
		return stream.readInt();
	}

	@Override
	public void write(Integer value, DataOutputStream stream) throws Exception {
		stream.writeInt(value);
	}
}
