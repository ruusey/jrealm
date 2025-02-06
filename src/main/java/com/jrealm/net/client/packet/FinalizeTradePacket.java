package com.jrealm.net.client.packet;

import java.io.DataOutputStream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.entity.NetTradeSelection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Streamable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinalizeTradePacket extends Packet {

	@SerializableField(order = 0, type = NetTradeSelection.class)
	private NetTradeSelection selection;

	@Override
	public void readData(byte[] data) throws Exception {
		final FinalizeTradePacket read = IOService.readPacket(getClass(), data);
		this.selection = read.getSelection();
		this.setId(PacketType.FINALIZE_TRADE.getPacketId());
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		IOService.writePacket(this, stream);
	}
}
