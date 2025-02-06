package com.jrealm.net.client.packet;

import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableString;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Streamable
public class RequestTradePacket extends Packet{
	@SerializableField(order = 0, type = SerializableString.class)
	private String requestingPlayerName;
	
	@Override
	public void readData(byte[] data) throws Exception {
		RequestTradePacket read = IOService.readPacket(getClass(), data);
		this.requestingPlayerName = read.getRequestingPlayerName();
		this.setId(PacketType.INIT_TRADE_REQUEST.getPacketId());
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		IOService.writePacket(this, stream);
	}
	
	public RequestTradePacket(String requestingPlayerName) {
		this.requestingPlayerName = requestingPlayerName;
		this.setId(PacketType.INIT_TRADE_REQUEST.getPacketId());

	}

}
