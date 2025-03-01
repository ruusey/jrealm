package com.jrealm.net.client.packet;

import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
public class PlayerDeathPacket extends Packet {

    public PlayerDeathPacket() {
        // TODO Auto-generated constructor stub
    }

    public PlayerDeathPacket(final byte id, final byte[] data) {
        super(id, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            PlayerDeathPacket.log.error("Failed to parse PlayerDeath packet, Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {

    }

    @Override
    public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;

    }

    public static PlayerDeathPacket from() throws Exception {
        return new PlayerDeathPacket(PacketType.PLAYER_DEATH.getPacketId(), new byte[0]);
    }
}
