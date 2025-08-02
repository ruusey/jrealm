package com.jrealm.net.server.packet;

import com.jrealm.game.entity.Player;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.util.Cardinality;

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
public class PlayerMovePacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
    private long entityId;
	@SerializableField(order = 1, type = SerializableByte.class)
    private byte dir;
	@SerializableField(order = 2, type = SerializableBoolean.class)
    private boolean move;

    public static PlayerMovePacket from(Player player, Cardinality direction, boolean move) throws Exception {
    	final PlayerMovePacket read = new PlayerMovePacket(player.getId(), direction.cardinalityId, move);
        return read;
    }

    public Cardinality getDirection() {
        return Cardinality.valueOf(this.dir);
    }

	@Override
	public byte getPacketId() {
		// TODO Auto-generated method stub
		return (byte) 1;
	}
}
