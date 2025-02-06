package com.jrealm.net.server.packet;

import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.Player;
import com.jrealm.game.util.Cardinality;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
@AllArgsConstructor
public class PlayerMovePacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
    private long entityId;
	@SerializableField(order = 1, type = SerializableByte.class)
    private byte dir;
	@SerializableField(order = 2, type = SerializableBoolean.class)
    private boolean move;

    public PlayerMovePacket() {

    }

    public PlayerMovePacket(final byte id, final byte[] data) {
        super(id, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            PlayerMovePacket.log.error("Failed to parse PlayerMove packet, Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
        PlayerMovePacket read = IOService.readPacket(getClass(), data);
        this.entityId = read.getEntityId();
        this.dir = read.getDir();
        this.move = read.isMove();
        this.setId(PacketType.PLAYER_MOVE.getPacketId());
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
    	IOService.writePacket(this, stream);
    }

    public static PlayerMovePacket from(Player player, Cardinality direction, boolean move) throws Exception {
    	PlayerMovePacket read = new PlayerMovePacket(player.getId(), direction.cardinalityId, move);
    	read.setId(PacketType.PLAYER_MOVE.getPacketId());
        return read;
    }

    public Cardinality getDirection() {
        return Cardinality.valueOf(this.dir);
    }
}
