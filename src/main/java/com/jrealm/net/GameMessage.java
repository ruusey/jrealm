package com.jrealm.net;

import java.io.DataOutputStream;

public interface GameMessage {
    public void readData(byte[] data) throws Exception;

    public int serializeWrite(DataOutputStream stream) throws Exception;

}
