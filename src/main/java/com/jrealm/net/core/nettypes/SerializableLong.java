package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
import com.jrealm.net.core.SerializableFieldType;

public class SerializableLong extends SerializableFieldType<Long> {

	@Override
	public Long read(DataInputStream stream) throws Exception {
		try {
			final Long res = stream.readLong();
			return res;
		} catch (Exception e) {
			return 0l;
		}
	}

	@Override
	public int write(Long value, DataOutputStream stream) throws Exception {
		stream.writeLong(value == null ? 0l : value);
		return NetConstants.INT64_LENGTH;
	}
}
