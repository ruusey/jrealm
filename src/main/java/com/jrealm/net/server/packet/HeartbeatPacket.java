package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class HeartbeatPacket extends Packet {

    private long playerId;
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
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);
        if (dis == null || dis.available() < 5)
            throw new IllegalStateException("No Packet data available to read from DataInputStream");
        this.playerId = dis.readLong();
        this.timestamp = dis.readLong();
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");
        this.addHeader(stream);
        stream.writeLong(this.playerId);
        stream.writeLong(this.timestamp);
    }

    public static HeartbeatPacket from(long playerId, long timestamp) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(playerId);
        dos.writeLong(timestamp);

        return new HeartbeatPacket(PacketType.HEARTBEAT.getPacketId(), baos.toByteArray());
    }
}
