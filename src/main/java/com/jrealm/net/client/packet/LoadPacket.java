package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class LoadPacket extends Packet {

	private Player[] players;
	private Enemy[] enemies;
	private Bullet[] bullets;
	private LootContainer[] containers;
	
	public LoadPacket() {

	}

	public LoadPacket(final byte id, final byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			log.error("Failed to parse LoadPacket packet, Reason: {}", e);
		}
	}

	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if (dis == null || dis.available() < 5)
			throw new IllegalStateException("No Packet data available to read from DataInputStream");
		int playersSize = dis.readInt();
		this.players = new Player[playersSize];
		for(int i = 0; i< playersSize; i++) {
			this.players[i] = Player.fromStream(dis);
		}
		
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");

		this.addHeader(stream);
		stream.writeInt(this.players.length);
		for(Player p : this.players) {
			p.write(stream);
		}
	}
	
	public static LoadPacket from(Player[] players) throws Exception{
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		DataOutputStream stream = new DataOutputStream(byteStream);
		stream.writeInt(players.length);
		for(Player p : players) {
			p.write(stream);
		}
		
		return new LoadPacket(PacketType.LOAD.getPacketId(), byteStream.toByteArray());
	}
}
