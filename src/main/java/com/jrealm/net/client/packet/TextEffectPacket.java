package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.EntityType;
import com.jrealm.game.contants.PacketType;
import com.jrealm.game.contants.TextEffect;
import com.jrealm.net.Packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class TextEffectPacket extends Packet {
    private byte textEffectId;
    private byte entityType;
    private long targetEntityId;
    private String text;

    public TextEffectPacket() {

    }

    public TextEffectPacket(final byte id, final byte[] data) {
        super(id, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            TextEffectPacket.log.error("Failed to parse TextEffect packet, Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream stream = new DataInputStream(bis);
        if ((stream == null) || (stream.available() < 5))
            throw new IllegalStateException("No Packet data available to read from DataInputStream");

        this.textEffectId = stream.readByte();
        this.entityType = stream.readByte();
        this.targetEntityId = stream.readLong();
        this.text = stream.readUTF();
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if ((this.getId() < 1) || (this.getData() == null) || (this.getData().length < 5))
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");
        this.addHeader(stream);
        stream.writeByte(this.textEffectId);
        stream.writeByte(this.entityType);
        stream.writeLong(this.targetEntityId);
        stream.writeUTF(this.text);
    }

    public static TextEffectPacket from(EntityType entityType, long targetEntityId, TextEffect effect, String text)
            throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        stream.writeByte(effect.ordinal());
        stream.writeByte(entityType.getEntityTypeId());
        stream.writeLong(targetEntityId);
        stream.writeUTF(text);
        return new TextEffectPacket(PacketType.TEXT_EFFECT.getPacketId(), byteStream.toByteArray());
    }
}
