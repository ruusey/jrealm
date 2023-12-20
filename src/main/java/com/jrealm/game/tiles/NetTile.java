package com.jrealm.game.tiles;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.model.TileModel;
import com.jrealm.game.tiles.blocks.Tile;
import com.jrealm.game.tiles.blocks.TileData;
import com.jrealm.net.Streamable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetTile implements Streamable<NetTile> {
	private short tileId;
	private short size;
	private byte layer;
	private int xIndex;
	private int yIndex;
	private TileData data;

	@Override
	public void write(DataOutputStream stream) throws Exception {
		stream.writeShort(this.tileId);
		stream.writeShort(this.size);
		stream.writeByte(this.layer);
		stream.writeInt(this.xIndex);
		stream.writeInt(this.yIndex);
		this.data.write(stream);
	}

	@Override
	public NetTile read(DataInputStream stream) throws Exception {
		final short tileId = stream.readShort();
		final short size = stream.readShort();
		final byte layer = stream.readByte();
		final int xIndex = stream.readInt();
		final int yIndex = stream.readInt();
		final TileData data = new TileData().read(stream);
		return new NetTile(tileId, size, layer, xIndex, yIndex, data);
	}

	public static NetTile from(Tile tile, TileModel model, byte layer, int xIndex, int yIndex) {
		return new NetTile(tile.getTileId(), model.getSize(), layer, xIndex, yIndex, model.getData());
	}


	public boolean equals(NetTile other) {
		return (this.tileId == other.getTileId()) && (this.getLayer() == other.getLayer())
				&& (this.getXIndex() == other.getXIndex()) && (this.getYIndex() == other.getYIndex());
	}
}
