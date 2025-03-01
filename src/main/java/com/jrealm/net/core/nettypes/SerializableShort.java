package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
import com.jrealm.net.core.SerializableFieldType;

public class SerializableShort extends SerializableFieldType<Short> {

	@Override
	public Short read(DataInputStream stream) throws Exception {
		try {
			final Short res = stream.readShort();
			return res;
		} catch (Exception e) {
			return (short) 0;
		}
	}

	@Override
	public int write(Short value, DataOutputStream stream) throws Exception {
		stream.writeShort(value == null ? 0 : value);
		return NetConstants.INT16_LENGTH;
	}
}
