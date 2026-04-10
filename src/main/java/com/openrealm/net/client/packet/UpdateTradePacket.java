package com.openrealm.net.client.packet;

import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.entity.NetTradeSelection;

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
