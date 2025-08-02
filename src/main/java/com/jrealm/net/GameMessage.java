package com.jrealm.net;

import java.io.DataOutputStream;

/** 
 * GameMessage interface to enforce implementation
 * of required functionality
 */
public interface GameMessage {
    public void readData(byte[] data) throws Exception;
    public int serializeWrite(DataOutputStream stream) throws Exception;
}
