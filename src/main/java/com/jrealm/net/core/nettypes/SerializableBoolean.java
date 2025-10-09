package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
import com.jrealm.net.core.SerializableFieldType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializableBoolean extends SerializableFieldType<Boolean> {

	@Override
	public Boolean read(DataInputStream stream) throws Exception {
		Boolean res = null;
		
		try {
			res = stream.readBoolean();
		} catch (Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("SerializableBoolean failed to read stream. Reason: {}", e);
			}
		}
		return res;
	}

	@Override
	public int write(Boolean value, DataOutputStream stream) throws Exception {
		if(stream==null) throw new Exception("SerializableBoolean Error: target stream cannot be null");
		stream.writeBoolean(value == null ? false : value);
		return NetConstants.BOOLEAN_LENGTH;
	}
}
