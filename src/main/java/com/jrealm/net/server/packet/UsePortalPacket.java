package com.jrealm.net.server.packet;

import java.io.DataOutputStream;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Streamable
@AllArgsConstructor
@NoArgsConstructor
public class UsePortalPacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)	
    private long portalId;
	@SerializableField(order = 1, type = SerializableLong.class)
    private long fromRealmId;
	@SerializableField(order = 2, type = SerializableLong.class)
    private long playerId;
	@SerializableField(order = 3, type = SerializableByte.class)
    private byte toVault;
	@SerializableField(order = 4, type = SerializableByte.class)
    private byte toNexus;

    @Override
    public void readData(byte[] data) throws Exception {
    	final UsePortalPacket packet = IOService.readStream(this.getClass(), data);
    	this.assignData(data, packet);
    }

    @Override
    public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
    }
    
    @Override
	public byte getPacketId() {
		return (byte) 13;
	}

    public static UsePortalPacket from(long portalId, long fromRealmId, long playerId) throws Exception {
    	final UsePortalPacket packet = new UsePortalPacket(portalId, fromRealmId, playerId, (byte)-1, (byte)-1);
        return packet;
    }

    public static UsePortalPacket toNexus(long fromRealmId, long playerId) throws Exception {
    	final UsePortalPacket packet = new UsePortalPacket(-1, fromRealmId, playerId, (byte)-1, (byte)1);
        return packet;
    }

    public static UsePortalPacket toVault(long fromRealmId, long playerId) throws Exception {
    	final UsePortalPacket packet = new UsePortalPacket(-1, fromRealmId, playerId, (byte)1, (byte)-1);
        return packet;
    }
    
    public boolean isToNexus() {
        return this.toNexus != (byte) -1;
    }

    public boolean isToVault() {
        return this.toVault != (byte) -1;
    }
}
