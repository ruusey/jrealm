package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
import com.jrealm.net.core.SerializableFieldType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializableShort extends SerializableFieldType<Short> {

	@Override
	public Short read(DataInputStream stream) throws Exception {
		Short res = 0;
		try {
			res = stream.readShort();
		} catch (Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("SerializableShort failed to read stream. Reason: {}", e);
			}
		}
		return res;
	}

	@Override
	public int write(Short value, DataOutputStream stream) throws Exception {
		if(stream==null)throw new Exception("SerializableShort Error: target stream cannot be null");
		stream.writeShort(value == null ? 0 : value);
		return NetConstants.INT16_LENGTH;
	}
}
