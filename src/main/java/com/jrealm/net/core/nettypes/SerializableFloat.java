package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
import com.jrealm.net.core.SerializableFieldType;

public class SerializableFloat extends SerializableFieldType<Float> {

	@Override
	public Float read(DataInputStream stream) throws Exception {
		return stream.readFloat();
	}

	@Override
	public int write(Float value, DataOutputStream stream) throws Exception {
		stream.writeFloat(value);
		return NetConstants.FLOAT_LENGTH;
	}
}
