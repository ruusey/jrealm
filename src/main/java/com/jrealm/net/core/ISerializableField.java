package com.jrealm.net.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface ISerializableField<T> {
	public T read(DataInputStream stream) throws Exception;
	public void write(T value, DataOutputStream stream) throws Exception;

}
