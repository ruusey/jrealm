package com.jrealm.net.server.packet;

import com.jrealm.game.entity.Player;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.PacketId;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
@AllArgsConstructor
@NoArgsConstructor
@PacketId(packetId = (byte)1)
public class PlayerMovePacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
    private long entityId;
	@SerializableField(order = 1, type = com.jrealm.net.core.nettypes.SerializableInt.class)
    private int seq;
	@SerializableField(order = 2, type = SerializableByte.class)
    private byte dirFlags;

    public static PlayerMovePacket from(Player player, int seq, byte dirFlags) throws Exception {
    	final PlayerMovePacket read = new PlayerMovePacket(player.getId(), seq, dirFlags);
        return read;
    }
}
