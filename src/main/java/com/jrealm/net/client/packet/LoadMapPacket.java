package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;

import com.jrealm.game.tiles.NetTile;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class LoadMapPacket extends Packet {

	private NetTile[] tiles;


	public LoadMapPacket() {

	}

	public LoadMapPacket(final byte id, final byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			LoadMapPacket.log.error("Failed to parse ObjectMove packet, Reason: {}", e);
		}
	}

	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if ((dis == null) || (dis.available() < 5))
			throw new IllegalStateException("No Packet data available to read from DataInputStream");

		short tilesSize = dis.readShort();
		if (tilesSize > 0) {
			this.tiles = new NetTile[tilesSize];
			for(int i = 0; i< tilesSize; i ++) {
				this.tiles[i] = new NetTile().read(dis);
			}
		}
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if ((this.getId() < 1) || (this.getData() == null) || (this.getData().length < 5))
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");

		this.addHeader(stream);
		stream.writeShort(this.tiles.length);
		for (NetTile tile : this.tiles) {
			tile.write(stream);
		}
	}

	public static LoadMapPacket from(List<NetTile> tiles) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream stream = new DataOutputStream(byteStream);

		stream.writeShort(tiles.size());
		for (NetTile tile : tiles) {
			tile.write(stream);
		}

		return new LoadMapPacket(PacketType.LOAD_MAP.getPacketId(), byteStream.toByteArray());
	}

	public static LoadMapPacket from(NetTile[] tiles) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream stream = new DataOutputStream(byteStream);

		stream.writeShort(tiles.length);
		for (NetTile tile : tiles) {
			tile.write(stream);
		}

		return new LoadMapPacket(PacketType.LOAD_MAP.getPacketId(), byteStream.toByteArray());
	}

	public boolean equals(LoadMapPacket other) {
		NetTile[] myTiles = this.getTiles();
		NetTile[] otherTiles = other.getTiles();
		if (myTiles.length != otherTiles.length)
			return false;

		for (int i = 0; i < myTiles.length; i++) {
			NetTile myTile = myTiles[i];
			NetTile otherTile = otherTiles[i];
			if (!myTile.equals(otherTile))
				return false;
		}

		return true;
	}
}
