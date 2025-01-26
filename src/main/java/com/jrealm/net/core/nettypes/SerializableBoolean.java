package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableBoolean extends SerializableFieldType<Boolean> {

	@Override
	public Boolean read(DataInputStream stream) throws Exception {
		return stream.readBoolean();
	}

	@Override
	public void write(Boolean value, DataOutputStream stream) throws Exception {
		stream.writeBoolean(value);
	}
}
