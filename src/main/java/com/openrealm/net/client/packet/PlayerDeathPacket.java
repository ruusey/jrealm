package com.openrealm.net.client.packet;

import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.nettypes.SerializableLong;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Slf4j
@Streamable
@PacketId(packetId = (byte)15)
public class PlayerDeathPacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
	private long playerId;
	
    public PlayerDeathPacket(long playerId) {
       this.playerId = playerId;
    }

    public static PlayerDeathPacket from(long playerId) throws Exception {
        return new PlayerDeathPacket(playerId);
    }
}
