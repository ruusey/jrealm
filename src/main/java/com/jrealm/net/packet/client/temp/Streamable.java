package com.jrealm.net.packet.client.temp;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface Streamable <T> {
	void write(DataOutputStream stream) throws Exception;
	T read(DataInputStream stream) throws Exception;

}
