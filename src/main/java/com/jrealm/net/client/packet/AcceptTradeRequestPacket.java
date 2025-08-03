package com.jrealm.net.client.packet;

import com.jrealm.game.entity.Player;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.PacketId;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.entity.NetGameItem;
import com.jrealm.net.entity.NetPlayer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Streamable
@PacketId(packetId=(byte)17)
public class AcceptTradeRequestPacket extends Packet{
	
	@SerializableField(order = 0, type = SerializableBoolean.class)
	private boolean accepted;
	@SerializableField(order = 1, type = NetPlayer.class)
	private NetPlayer player0;
	@SerializableField(order = 2, type = NetPlayer.class)
	private NetPlayer player1;
	@SerializableField(order = 3, type = NetGameItem.class, isCollection = true)
	private NetGameItem[] player0Inv;
	@SerializableField(order = 4, type = NetGameItem.class, isCollection = true)
	private NetGameItem[] player1Inv;
	
	public AcceptTradeRequestPacket(boolean accepted, Player p0, Player p1) {
		this.accepted = accepted;
		this.player0 = (IOService.mapModel(p0, NetPlayer.class));
		this.player1 = (IOService.mapModel(p1, NetPlayer.class));
		this.player0Inv = (IOService.mapModel(p0.getInventory(), NetGameItem[].class));
		this.player1Inv = (IOService.mapModel(p1.getInventory(), NetGameItem[].class));
	}
}
