package com.jrealm.net;

import java.io.DataOutputStream;

import com.jrealm.net.core.PacketId;

@PacketId(packetId=(byte)-1)
public class BlankPacket extends Packet {

    public BlankPacket(byte packetId, byte[] data) {
        super(packetId, data);
    }
    // Overridden to be no-op
    @Override
    public void readData(byte[] data) throws Exception {
        // NO OP
    }

    @Override
    public int serializeWrite(DataOutputStream stream) throws Exception {
    	// NO OP
       return 0;
    }
}
