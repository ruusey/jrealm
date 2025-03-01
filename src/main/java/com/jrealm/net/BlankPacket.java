package com.jrealm.net;

import java.io.DataOutputStream;

public class BlankPacket extends Packet {

    public BlankPacket(byte packetId, byte[] data) {
        super(packetId, data);
    }

    @Override
    public void readData(byte[] data) throws Exception {
        // NO OP
    }

    @Override
    public int serializeWrite(DataOutputStream stream) throws Exception {
       return 0;
    }
}
