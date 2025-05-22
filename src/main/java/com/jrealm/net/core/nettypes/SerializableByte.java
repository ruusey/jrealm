package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
import com.jrealm.net.core.SerializableFieldType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializableByte extends SerializableFieldType<Byte> {

	@Override
	public Byte read(DataInputStream stream) throws Exception {
		Byte res = 0;
		try {
			res = stream.readByte();
		} catch (Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("SerializableByte failed to read stream. Reason: {}", e);
			}
		}
		return res;
	}

	@Override
	public int write(Byte value, DataOutputStream stream) throws Exception {
		if(stream==null)throw new Exception("SerializableByte Error: target stream cannot be null");
		stream.writeByte(value == null ? 0 : value);
		return NetConstants.BYTE_LENGTH;
	}
}
