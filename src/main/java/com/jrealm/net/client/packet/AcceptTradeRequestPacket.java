package com.jrealm.net.client.packet;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableBoolean;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Streamable
public class AcceptTradeRequestPacket extends Packet{
	@SerializableField(order = 0, type = SerializableBoolean.class)
	private boolean accepted;
	
	public AcceptTradeRequestPacket(boolean accepted) {
		this.accepted = accepted;
	}

	@Override
	public byte getPacketId() {
		return (byte) 17;
	}
}
