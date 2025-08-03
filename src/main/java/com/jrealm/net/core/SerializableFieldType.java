package com.jrealm.net.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public abstract class SerializableFieldType<T> implements ISerializableField<T> {
	public void assignData(Object target, Object src) {
    	if(target==null || src==null) return;
		target = src;
	}
	
	public T read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	public int write(T value, DataOutputStream stream) throws Exception {
		return IOService.writeStream(value, stream);
		
	}
	
}
