package com.jrealm.net.client.packet;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableString;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Streamable
public class RequestTradePacket extends Packet{
	@SerializableField(order = 0, type = SerializableString.class)
	private String requestingPlayerName;
	
	public RequestTradePacket(String requestingPlayerName) {
		this.requestingPlayerName = requestingPlayerName;
	}

	@Override
	public byte getPacketId() {
		// TODO Auto-generated method stub
		return (byte)16;
	}
}
