package com.jrealm.net.packet.client.temp;

import java.io.DataInputStream;

public interface StreamReadable <T> {
	T read(DataInputStream stream) throws Exception;
}
