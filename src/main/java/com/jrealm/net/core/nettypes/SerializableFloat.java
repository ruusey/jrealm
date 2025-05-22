package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
import com.jrealm.net.core.SerializableFieldType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializableFloat extends SerializableFieldType<Float> {

	@Override
	public Float read(DataInputStream stream) throws Exception {
		Float result = 0f;
		try {
			result = stream.readFloat();
		} catch (Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("SerializableFloat failed to read stream. Reason: {}", e);
			}
		}
		return result;
	}

	@Override
	public int write(Float value, DataOutputStream stream) throws Exception {
		if(stream==null)throw new Exception("SerializableFloat Error: target stream cannot be null");
		stream.writeFloat(value == null ? 0f : value);
		return NetConstants.FLOAT_LENGTH;
	}
}
