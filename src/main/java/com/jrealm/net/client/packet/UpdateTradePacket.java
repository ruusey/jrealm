package com.jrealm.net.client.packet;

import java.io.DataOutputStream;

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
public class UpdateTradePacket extends Packet {

	@SerializableField(order = 0, type = NetTradeSelection.class)
	private NetTradeSelection selections;

	@Override
	public void readData(byte[] data) throws Exception {
		final UpdateTradePacket read = IOService.readPacket(getClass(), data);
		this.assignData(this, read);
	}

	@Override
	public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
	}

	@Override
	public byte getPacketId() {
		// TODO Auto-generated method stub
		return (byte) 19;
	}
}
