package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableFloat extends SerializableFieldType<Float> {

	@Override
	public Float read(DataInputStream stream) throws Exception {
		return stream.readFloat();
	}

	@Override
	public void write(Float value, DataOutputStream stream) throws Exception {
		stream.writeFloat(value);
	}
}
