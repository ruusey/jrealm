package com.jrealm.net.core;

public abstract class SerializableFieldType<T> implements ISerializableField<T> {
	public void assignData(Object target, Object src) {
    	if(target==null || src==null) return;
		target = src;
	}
}
