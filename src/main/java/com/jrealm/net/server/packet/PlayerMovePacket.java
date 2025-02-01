package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.Player;
import com.jrealm.game.util.Cardinality;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
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
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);
        if ((dis == null) || (dis.available() < 5))
            throw new IllegalStateException("No Packet data available to read from DataInputStream");

        this.entityId = dis.readLong();
        this.dir = dis.readByte();
        this.move = dis.readBoolean();
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if ((this.getId() < 1) || (this.getData() == null) || (this.getData().length < 5))
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");
        this.addHeader(stream);
        stream.writeLong(this.entityId);
        stream.writeByte(this.dir);
        stream.writeBoolean(this.move);
    }

    public static PlayerMovePacket from(Player player, Cardinality direction, boolean move) throws Exception {
    	final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	final DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(player.getId());
        dos.writeByte(direction.cardinalityId);
        dos.writeBoolean(move);
        return new PlayerMovePacket(PacketType.PLAYER_MOVE.getPacketId(), baos.toByteArray());
    }

    public Cardinality getDirection() {
        return Cardinality.valueOf(this.dir);
    }
}
