package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializableString extends SerializableFieldType<String> {

	@Override
	public String read(DataInputStream stream) throws Exception {
		try {
			return stream.readUTF();
		}catch(Exception e) {
			e.printStackTrace();
			//.println("ERROR Failed to read String from streaam. Reason "+e.getMessage());
			return "";
		}
	}

	@Override
	public void write(String value, DataOutputStream stream) throws Exception {
		stream.writeUTF(value==null?"":value);
	}
}
