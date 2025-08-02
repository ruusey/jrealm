package com.jrealm.net.server.packet;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Streamable
@AllArgsConstructor
@NoArgsConstructor
public class DeathAckPacket extends Packet{
	@SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;

    public static DeathAckPacket from(long playerId) throws Exception {
        return new DeathAckPacket(playerId);
    }

	@Override
	public byte getPacketId() {
		// TODO Auto-generated method stub
		return (byte) 20;
	}
}
