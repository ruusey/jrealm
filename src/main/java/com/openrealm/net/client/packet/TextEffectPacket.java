package com.openrealm.net.client.packet;

import com.openrealm.game.contants.EntityType;
import com.openrealm.game.contants.TextEffect;
import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.nettypes.SerializableByte;
import com.openrealm.net.core.nettypes.SerializableFloat;
import com.openrealm.net.core.nettypes.SerializableLong;
import com.openrealm.net.core.nettypes.SerializableString;

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
	// World-space impact point (bullet center on hit, AoE epicenter on
	// status pulses). 0,0 means "use the target entity's current position"
	// — kept for back-compat with existing callers that don't supply a
	// specific point. The webclient prefers posX/posY when nonzero, which
	// fixes "damage text appears at enemy's NEW position after it moved
	// between server hit and client render".
	@SerializableField(order = 4, type = SerializableFloat.class)
    private float posX;
	@SerializableField(order = 5, type = SerializableFloat.class)
    private float posY;

    public static TextEffectPacket from(EntityType entityType, long targetEntityId, TextEffect effect, String text)
            throws Exception {
        return from(entityType, targetEntityId, effect, text, 0f, 0f);
    }

    public static TextEffectPacket from(EntityType entityType, long targetEntityId, TextEffect effect, String text,
            float posX, float posY) throws Exception {
    	final TextEffectPacket packet = new TextEffectPacket();
    	packet.textEffectId = (byte) effect.ordinal();
    	packet.entityType = entityType.getEntityTypeId();
    	packet.targetEntityId = targetEntityId;
    	packet.text = text;
    	packet.posX = posX;
    	packet.posY = posY;
        return packet;
    }
}
