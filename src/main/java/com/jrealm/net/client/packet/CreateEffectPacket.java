package com.jrealm.net.client.packet;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.PacketId;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableShort;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
@Builder
@AllArgsConstructor
@NoArgsConstructor
@PacketId(packetId = (byte)21)
public class CreateEffectPacket extends Packet{
	@SerializableField(order = 0, type = SerializableLong.class)
	private long targetEntityId;  
	@SerializableField(order = 0, type = SerializableShort.class)
	private short effectId;
	@SerializableField(order = 0, type = SerializableShort.class)
	private short duration;
}
