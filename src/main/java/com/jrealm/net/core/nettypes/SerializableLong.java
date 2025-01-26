package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableLong extends SerializableFieldType<Long>{

	@Override
	public Long read(DataInputStream stream) throws Exception {
		return stream.readLong();
	}

	@Override
	public void write(Long value, DataOutputStream stream) throws Exception{
		stream.writeLong(value);
	}
}
