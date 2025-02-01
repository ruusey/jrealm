package com.jrealm.net.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableFloat;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Streamable
public class PlayerShootPacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
    private long projectileId;
	@SerializableField(order = 1, type = SerializableLong.class)
    private long entityId;
	@SerializableField(order = 2, type = SerializableInt.class)
    private int projectileGroupId;
	@SerializableField(order = 3, type = SerializableFloat.class)
    private float destX;
	@SerializableField(order = 4, type = SerializableFloat.class)
    private float destY;
	@SerializableField(order = 5, type = SerializableFloat.class)
    private float srcX;
	@SerializableField(order = 6, type = SerializableFloat.class)
    private float srcY;

    public PlayerShootPacket() {

    }

    public PlayerShootPacket(final byte id, final byte[] data) {
        super(id, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            log.error("Failed to parse ObjectMove packet, Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);
        if (dis == null || dis.available() < 5)
            throw new IllegalStateException("No Packet data available to read from DataInputStream");
        this.projectileId = dis.readLong();
        this.entityId = dis.readLong();
        this.projectileGroupId = dis.readInt();
        this.destX = dis.readFloat();
        this.destY = dis.readFloat();
        this.srcX = dis.readFloat();
        this.srcY = dis.readFloat();
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");
        this.addHeader(stream);
        stream.writeLong(this.projectileId);
        stream.writeLong(this.entityId);
        stream.writeInt(this.projectileGroupId);
        stream.writeFloat(this.destX);
        stream.writeFloat(this.destY);
        stream.writeFloat(this.srcX);
        stream.writeFloat(this.srcY);
    }

    public static PlayerShootPacket from(long newProjectileId, Player p, Vector2f dest) throws Exception {
    	final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	final DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(newProjectileId);
        dos.writeLong(p.getId());
        dos.writeInt(p.getWeaponId());
        if (dest != null) {
            dos.writeFloat(dest.x);
            dos.writeFloat(dest.y);
        } else {
            dos.writeFloat(-1);
            dos.writeFloat(-1);
        }

        dos.writeFloat(p.getPos().x);
        dos.writeFloat(p.getPos().y);
        return new PlayerShootPacket(PacketType.PLAYER_SHOOT.getPacketId(), baos.toByteArray());
    }
}
