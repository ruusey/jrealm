package com.jrealm.net.server.packet;

import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Streamable
@AllArgsConstructor
public class DeathAckPacket extends Packet{
	@SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;
	
	public DeathAckPacket(){
		
	}
	
	public DeathAckPacket(byte packetId, byte[] data) {
        super(packetId, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            log.error("Failed to create DeathAck Packet. Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
    	final DeathAckPacket read = IOService.readPacket(getClass(), data);
    	this.playerId = read.getPlayerId();
    	this.setId(PacketType.DEATH_ACK.getPacketId());
    }

    @Override
    public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
    }

    public static DeathAckPacket from(long playerId) throws Exception {
        return new DeathAckPacket(playerId);
    }
}
