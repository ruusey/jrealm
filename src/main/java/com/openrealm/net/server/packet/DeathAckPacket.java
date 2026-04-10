package com.openrealm.net.server.packet;

import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Streamable
@AllArgsConstructor
@NoArgsConstructor
@PacketId(packetId=(byte)20)
public class DeathAckPacket extends Packet{
	@SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;

    public static DeathAckPacket from(long playerId) throws Exception {
        return new DeathAckPacket(playerId);
    }
}
