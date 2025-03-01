package com.jrealm.net.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Streamable;

@Streamable
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
