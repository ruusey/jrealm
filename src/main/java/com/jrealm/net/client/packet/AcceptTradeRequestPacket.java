package com.jrealm.net.client.packet;

import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
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
		this.setId(PacketType.ACCEPT_TRADE_REQUEST.getPacketId());
	}
	
	@Override
	public void readData(byte[] data) throws Exception {
		AcceptTradeRequestPacket read = IOService.readPacket(getClass(), data);
		this.accepted = read.isAccepted();
		this.setId(PacketType.ACCEPT_TRADE_REQUEST.getPacketId());
	}

	@Override
	public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
	}
}
