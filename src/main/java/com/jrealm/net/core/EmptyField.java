package com.jrealm.net.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Streamable;

@Streamable
// Empty fields will always be read as null and not write
// data to the byte stream (no-op)
public class EmptyField extends SerializableFieldType<EmptyField>{

	@Override
	public EmptyField read(DataInputStream stream) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int write(EmptyField value, DataOutputStream stream) throws Exception {
		return 0;
		
	}

}
