package com.openrealm.net.client.packet;

import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.nettypes.SerializableString;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Streamable
@PacketId(packetId = (byte)16)
public class RequestTradePacket extends Packet{
	@SerializableField(order = 0, type = SerializableString.class)
	private String requestingPlayerName;
	
	public RequestTradePacket(String requestingPlayerName) {
		this.requestingPlayerName = requestingPlayerName;
	}
}
