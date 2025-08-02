package com.jrealm.net.client.packet;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.PacketId;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.entity.NetTradeSelection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Streamable
@Data
@NoArgsConstructor
@AllArgsConstructor
@PacketId(packetId = (byte)19)
public class UpdateTradePacket extends Packet {

	@SerializableField(order = 0, type = NetTradeSelection.class)
	private NetTradeSelection selections;
}
