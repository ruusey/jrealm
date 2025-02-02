package com.jrealm.net.core.nettypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.core.SerializableFieldType;

public class SerializableString extends SerializableFieldType<String> {

	@Override
	public String read(DataInputStream stream) throws Exception {
		return stream.readUTF();
	}

	@Override
	public void write(String value, DataOutputStream stream) throws Exception {
		stream.writeUTF(value==null?"":value);
	}
}
