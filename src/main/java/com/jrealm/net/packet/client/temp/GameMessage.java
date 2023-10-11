package com.jrealm.net.packet.client.temp;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface GameMessage {
	public void readData(DataInputStream stream) throws Exception;
	public void serializeWrite(DataOutputStream stream) throws Exception;
}
