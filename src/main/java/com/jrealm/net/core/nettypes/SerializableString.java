package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
import com.jrealm.net.core.SerializableFieldType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializableString extends SerializableFieldType<String> {

	@Override
	public String read(DataInputStream stream) throws Exception {
		try {
			return stream.readUTF();
		} catch (Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("SerializableString failed to read stream. Reason: {}", e);
			}
			return "";
		}
	}

	@Override
	public int write(String value, DataOutputStream stream) throws Exception {
		if(stream==null)throw new Exception("SerializableString Error: target stream cannot be null");
		final String toUse = value == null ? "" : value;
		stream.writeUTF(toUse);
		// UTF net encoding
		return NetConstants.INT16_LENGTH +  value.getBytes().length;
	}
}
