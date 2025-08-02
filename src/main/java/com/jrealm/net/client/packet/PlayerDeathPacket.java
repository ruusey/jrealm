package com.jrealm.net.client.packet;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Slf4j
@Streamable
public class PlayerDeathPacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
	private long playerId;
	
    public PlayerDeathPacket(long playerId) {
       this.playerId = playerId;
    }

    public static PlayerDeathPacket from(long playerId) throws Exception {
        return new PlayerDeathPacket(playerId);
    }

	@Override
	public byte getPacketId() {
		// TODO Auto-generated method stub
		return (byte)15;
	}
}
