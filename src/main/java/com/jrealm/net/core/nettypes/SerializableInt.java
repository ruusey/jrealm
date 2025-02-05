package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializableInt extends SerializableFieldType<Integer> {

	@Override
	public Integer read(DataInputStream stream) throws Exception {
		Integer result = 0;
		try {
			result = stream.readInt();
		} catch (Exception e) {
			// log.error("Failed to read integerr. Reason: {}", e.getMessage());
		}
		return result;
	}

	@Override
	public void write(Integer value, DataOutputStream stream) throws Exception {
		if (value != null) {
			stream.writeInt(value);
		} else {
			stream.writeInt(0);
		}
	}
}
