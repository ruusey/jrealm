package com.jrealm.net.client.packet;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableShort;
import com.jrealm.net.entity.NetTile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@Streamable
@AllArgsConstructor
public class LoadMapPacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
    private long realmId;
	@SerializableField(order = 1, type = SerializableShort.class)
    private short mapId;
	@SerializableField(order = 2, type = SerializableShort.class)
    private short mapWidth;
	@SerializableField(order = 3, type = SerializableShort.class)
    private short mapHeight;
	@SerializableField(order = 4, type = NetTile.class, isCollection=true)
    private NetTile[] tiles;

    public LoadMapPacket() {

    }

    @Override
    public void readData(byte[] data) throws Exception {
    	final LoadMapPacket readPacket = IOService.readPacket(getClass(), data);
        this.assignData(data, readPacket);
    }

    @Override
    public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
    }

    public static LoadMapPacket from(long realmId, short mapId, short mapWidth, short mapHeight, List<NetTile> tiles) throws Exception {
    	return new LoadMapPacket(realmId, mapId, mapWidth, mapHeight, tiles.toArray(new NetTile[0]));
    }

    public static LoadMapPacket from(long realmId, short mapId, short mapWidth, short mapHeight,  NetTile[] tiles) throws Exception {
    	return new LoadMapPacket(realmId, mapId, mapWidth, mapHeight, tiles);
    }

    public LoadMapPacket difference(LoadMapPacket other) throws Exception {
        List<NetTile> diff = new ArrayList<>();
        // If the player is changing realms, force the new tiles to be sent
        if (this.realmId != other.getRealmId())
            return other;
        for (final NetTile tileOther : other.getTiles()) {
            if (!LoadMapPacket.tilesContains(tileOther, this.getTiles())) {
                diff.add(tileOther);
            }
        }
        if (diff.size() == 0)
            return null;
        return LoadMapPacket.from(other.getRealmId(), other.getMapId(), other.getMapWidth(), other.getMapHeight(), diff);
    }

    public boolean equals(LoadMapPacket other) {
    	final NetTile[] myTiles = this.getTiles();
    	final NetTile[] otherTiles = other.getTiles();
        if (myTiles.length != otherTiles.length)
            return false;

        if (this.getRealmId() != other.getRealmId())
            return false;
        
        if(this.getMapId()!=other.getMapId()) {
        	return false;
        }

        if(this.getMapHeight()!=other.getMapHeight() || this.getMapWidth()!=other.getMapWidth()) {
            return false;
        }
        
        // Calculate per-tile difference
        for (int i = 0; i < myTiles.length; i++) {
        	final NetTile myTile = myTiles[i];
        	final NetTile otherTile = otherTiles[i];
            if (!myTile.equals(otherTile))
                return false;
        }
        return true;
    }

    public static boolean tilesContains(NetTile tile, NetTile[] array) {
        for (NetTile netTile : array) {
            if (tile.equals(netTile))
                return true;
        }
        return false;
    }

	@Override
	public byte getPacketId() {
		return (byte) 8;
	}
}
