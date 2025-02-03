package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.model.TileModel;
import com.jrealm.game.tile.Tile;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableShort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Streamable
public class NetTile extends SerializableFieldType<NetTile> {
	@SerializableField(order = 0, type = SerializableShort.class)
    private Short tileId;
	@SerializableField(order = 1, type = SerializableByte.class)
    private Byte layer;
	@SerializableField(order = 2, type = SerializableInt.class)
    private Integer xIndex;
	@SerializableField(order = 3, type = SerializableInt.class)
    private Integer yIndex;

	public NetTile() {
		this.tileId = -1;
		this.layer = -1;
		this.xIndex = -1;
		this.yIndex = -1;
	}
	
	@Override
	public NetTile read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetTile value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}

    public static NetTile from(Tile tile, TileModel model, byte layer, int xIndex, int yIndex) {
        return new NetTile(tile.getTileId(), layer, xIndex, yIndex);
    }

    public boolean equals(NetTile other) {
        return (this.tileId == other.getTileId()) && (this.getLayer() == other.getLayer())
                && (this.getXIndex() == other.getXIndex()) && (this.getYIndex() == other.getYIndex());
    }

	
}
