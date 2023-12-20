package com.jrealm.game.tiles;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.model.TileModel;
import com.jrealm.game.tiles.blocks.Tile;
import com.jrealm.net.Streamable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetTile implements Streamable<NetTile> {
	private short tileId;
	private byte layer;
	private int xIndex;
	private int yIndex;

	@Override
	public void write(DataOutputStream stream) throws Exception {
		stream.writeShort(this.tileId);
		stream.writeByte(this.layer);
		stream.writeInt(this.xIndex);
		stream.writeInt(this.yIndex);
	}

	@Override
	public NetTile read(DataInputStream stream) throws Exception {
		final short tileId = stream.readShort();
		final byte layer = stream.readByte();
		final int xIndex = stream.readInt();
		final int yIndex = stream.readInt();
		return new NetTile(tileId, layer, xIndex, yIndex);
	}

	public static NetTile from(Tile tile, TileModel model, byte layer, int xIndex, int yIndex) {
		return new NetTile(tile.getTileId(), layer, xIndex, yIndex);
	}


	public boolean equals(NetTile other) {
		return (this.tileId == other.getTileId()) && (this.getLayer() == other.getLayer())
				&& (this.getXIndex() == other.getXIndex()) && (this.getYIndex() == other.getYIndex());
	}
}
