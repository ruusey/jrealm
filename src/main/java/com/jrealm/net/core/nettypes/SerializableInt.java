package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
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
			if(log.isDebugEnabled()) {
				log.debug("SerializableInt failed to read stream. Reason: {}", e);
			}
		}
		return result;
	}

	@Override
	public int write(Integer value, DataOutputStream stream) throws Exception {
		if(stream==null)throw new Exception("SerializableInt Error: target stream cannot be null");
		if (value != null) {
			stream.writeInt(value);
		} else {
			stream.writeInt(0);
		}
		return NetConstants.INT32_LENGTH;
	}
}
