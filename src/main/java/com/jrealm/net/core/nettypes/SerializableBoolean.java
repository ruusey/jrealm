package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableBoolean extends SerializableFieldType<Boolean> {

	@Override
	public Boolean read(DataInputStream stream) throws Exception {
		try {
			return stream.readBoolean();
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void write(Boolean value, DataOutputStream stream) throws Exception {
		final Boolean toWrite = value == null ? false : value;
		stream.writeBoolean(toWrite);
	}
}
