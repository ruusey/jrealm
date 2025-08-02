package com.jrealm.net.client.packet;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.PacketId;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableBoolean;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Streamable
@PacketId(packetId=(byte)17)
public class AcceptTradeRequestPacket extends Packet{
	@SerializableField(order = 0, type = SerializableBoolean.class)
	private boolean accepted;
	
	public AcceptTradeRequestPacket(boolean accepted) {
		this.accepted = accepted;
	}
}
