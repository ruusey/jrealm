package com.jrealm.net.client.packet;

import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableString;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitTradeRequestPacket extends Packet{
	@SerializableField(order = 0, type = SerializableString.class)
	private String playerName;
	
	@Override
	public void readData(byte[] data) throws Exception {
		InitTradeRequestPacket read = IOService.readPacket(getClass(), data);
		this.playerName = read.getPlayerName();
		this.setId(PacketType.INIT_TRADE_REQUEST.getPacketId());
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		IOService.writePacket(this, stream);
	}

}
