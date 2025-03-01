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
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@Streamable
@AllArgsConstructor
public class HeartbeatPacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;
	@SerializableField(order = 1, type = SerializableLong.class)
    private long timestamp;

    public HeartbeatPacket() {

    }

    public HeartbeatPacket(byte packetId, byte[] data) {
        super(packetId, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            log.error("Failed to create Text Packet. Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
    	final HeartbeatPacket read = IOService.readPacket(getClass(), data);
    	this.playerId = read.getPlayerId();
    	this.timestamp = read.getTimestamp();
    	this.setId(PacketType.HEARTBEAT.getPacketId());
    }

    @Override
    public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
    }

    public static HeartbeatPacket from(long playerId, long timestamp) throws Exception {
        return new HeartbeatPacket(playerId, timestamp);
    }
}
