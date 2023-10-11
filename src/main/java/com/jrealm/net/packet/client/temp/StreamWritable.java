package com.jrealm.net.packet.client.temp;

import java.io.DataOutputStream;

public interface StreamWritable <T> {
	void write(DataOutputStream stream) throws Exception;
}
