package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.Player;
import com.jrealm.game.tiles.TileMap;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class LoadMapPacket extends Packet {

	private long playerId;
	private String mapKey;
//	private short tileSize;
//	private short width;
//	private short height;
//	private Tile[] tiles;


	public LoadMapPacket() {

	}

	public LoadMapPacket(final byte id, final byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			log.error("Failed to parse ObjectMove packet, Reason: {}", e);
		}
	}

	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if (dis == null || dis.available() < 5)
			throw new IllegalStateException("No Packet data available to read from DataInputStream");
		
		this.playerId = dis.readLong();
		this.mapKey = dis.readUTF();
//		this.tileSize = dis.readShort();
//		this.width = dis.readShort();
//		this.height = dis.readShort();
//		
//		short tilesSize = dis.readShort();
//		this.tiles = new Tile[tilesSize];
//		
//		for(int i = 0; i < tilesSize; i++) {
//			this.tiles[i] = new Tile().read(dis);
//		}

	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");

		this.addHeader(stream);
		stream.writeLong(this.playerId);
		stream.writeUTF(this.mapKey);
//		stream.writeShort(this.tileSize);
//		stream.writeShort(this.width);
//		stream.writeShort(this.height);
//		
//		short tilesSize = this.tiles != null ?  (short) this.tiles.length : (short) 0;
//		stream.writeShort(tilesSize);
//		
//		for(Tile tile : this.tiles) {
//			tile.write(stream);
//		}
	}

	public static LoadMapPacket from(Player player, String mapKey) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		DataOutputStream stream = new DataOutputStream(byteStream);

		stream.writeLong(player.getId());
		stream.writeUTF(mapKey);

		return new LoadMapPacket(PacketType.LOAD_MAP.getPacketId(), byteStream.toByteArray());
	}
	
	public static LoadMapPacket from(Player player, String mapKey, TileMap map) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		DataOutputStream stream = new DataOutputStream(byteStream);

		stream.writeLong(player.getId());
		stream.writeUTF(mapKey);
		//stream.writeShort(map.getTileSize());

		return new LoadMapPacket(PacketType.LOAD_MAP.getPacketId(), byteStream.toByteArray());
	}
}
