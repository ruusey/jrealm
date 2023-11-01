package com.jrealm.net;

import java.io.DataOutputStream;

public interface GameMessage {
	public void readData(byte[] data) throws Exception;
	public void serializeWrite(DataOutputStream stream) throws Exception;
		
}
