package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
import com.jrealm.net.core.SerializableFieldType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializableLong extends SerializableFieldType<Long> {

	@Override
	public Long read(DataInputStream stream) throws Exception {
		Long res = 0l;
		try {
			res = stream.readLong();
		} catch (Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("SerializableLong failed to read stream. Reason: {}", e);
			}
		}
		return res;
	}

	@Override
	public int write(Long value, DataOutputStream stream) throws Exception {
		if(stream==null)throw new Exception("SerializableLong Error: target stream cannot be null");
		stream.writeLong(value == null ? 0l : value);
		return NetConstants.INT64_LENGTH;
	}
}
