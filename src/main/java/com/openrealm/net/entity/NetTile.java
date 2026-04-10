package com.openrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.openrealm.game.model.TileModel;
import com.openrealm.game.tile.Tile;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.IOService;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.SerializableFieldType;
import com.openrealm.net.core.nettypes.SerializableByte;
import com.openrealm.net.core.nettypes.SerializableInt;
import com.openrealm.net.core.nettypes.SerializableShort;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Streamable
public class NetTile extends SerializableFieldType<NetTile> {
	@SerializableField(order = 0, type = SerializableShort.class)
    private short tileId;
	@SerializableField(order = 1, type = SerializableByte.class)
    private byte layer;
	@SerializableField(order = 2, type = SerializableInt.class)
    private int xIndex;
	@SerializableField(order = 3, type = SerializableInt.class)
    private int yIndex;

	public NetTile() {
		this.tileId = -1;
		this.layer = -1;
		this.xIndex = -1;
		this.yIndex = -1;
	}

    public static NetTile from(Tile tile, TileModel model, byte layer, int xIndex, int yIndex) {
        return new NetTile(tile.getTileId(), layer, xIndex, yIndex);
    }

    public boolean equals(NetTile other) {
        return (this.tileId == other.getTileId()) && (this.getLayer() == other.getLayer())
                && (this.getXIndex() == other.getXIndex()) && (this.getYIndex() == other.getYIndex());
    }

	
}
