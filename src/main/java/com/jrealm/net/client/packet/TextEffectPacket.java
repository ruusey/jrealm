package com.jrealm.net.client.packet;

import com.jrealm.game.contants.EntityType;
import com.jrealm.game.contants.TextEffect;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.PacketId;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableString;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
@PacketId(packetId = (byte)14)
public class TextEffectPacket extends Packet {
	@SerializableField(order = 0, type = SerializableByte.class)
    private byte textEffectId;
	@SerializableField(order = 1, type = SerializableByte.class)
    private byte entityType;
	@SerializableField(order = 2, type = SerializableLong.class)
    private long targetEntityId;
	@SerializableField(order = 3, type = SerializableString.class)
    private String text;

    public static TextEffectPacket from(EntityType entityType, long targetEntityId, TextEffect effect, String text)
            throws Exception {
    	final TextEffectPacket packet = new TextEffectPacket();
    	packet.textEffectId = (byte) effect.ordinal();
    	packet.entityType = entityType.getEntityTypeId();
    	packet.targetEntityId = targetEntityId;
    	packet.text = text;
        return packet;
    }
}
