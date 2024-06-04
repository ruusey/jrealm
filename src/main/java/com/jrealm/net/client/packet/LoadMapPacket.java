package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.tile.NetTile;
import com.jrealm.net.Packet;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class LoadMapPacket extends Packet {
    private long realmId;
    private short mapId;
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
        final long realmId = dis.readLong();
        final short mapId = dis.readShort();
        this.realmId = realmId;
        this.mapId = mapId;
        short tilesSize = dis.readShort();
        if (tilesSize > 0) {
            this.tiles = new NetTile[tilesSize];
            for (int i = 0; i < tilesSize; i++) {
                this.tiles[i] = new NetTile().read(dis);
            }
        }
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if ((this.getId() < 1) || (this.getData() == null) || (this.getData().length < 5))
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");

        this.addHeader(stream);
        stream.writeLong(this.realmId);
        stream.writeShort(this.mapId);
        stream.writeShort(this.tiles.length);
        for (NetTile tile : this.tiles) {
            tile.write(stream);
        }
    }

    public static LoadMapPacket from(long realmId, short mapId, List<NetTile> tiles) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        stream.writeLong(realmId);
        stream.writeShort(mapId);
        stream.writeShort(tiles.size());
        for (NetTile tile : tiles) {
            tile.write(stream);
        }

        return new LoadMapPacket(PacketType.LOAD_MAP.getPacketId(), byteStream.toByteArray());
    }

    public static LoadMapPacket from(long realmId, short mapId, NetTile[] tiles) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        stream.writeLong(realmId);
        stream.writeShort(mapId);
        stream.writeShort(tiles.length);
        for (NetTile tile : tiles) {
            tile.write(stream);
        }

        return new LoadMapPacket(PacketType.LOAD_MAP.getPacketId(), byteStream.toByteArray());
    }

    public LoadMapPacket difference(LoadMapPacket other) throws Exception {
        List<NetTile> diff = new ArrayList<>();
        // If the player is changing realms, force the new tiles to be sent
        if (this.realmId != other.getRealmId())
            return other;
        for (NetTile tileOther : other.getTiles()) {
            if (!LoadMapPacket.tilesContains(tileOther, this.getTiles())) {
                diff.add(tileOther);
            }
        }
        if (diff.size() == 0)
            return null;
        return LoadMapPacket.from(other.getRealmId(), other.getMapId(), diff);
    }

    public boolean equals(LoadMapPacket other) {
        NetTile[] myTiles = this.getTiles();
        NetTile[] otherTiles = other.getTiles();
        if (myTiles.length != otherTiles.length)
            return false;

        if (this.getRealmId() != other.getRealmId())
            return false;

        for (int i = 0; i < myTiles.length; i++) {
            NetTile myTile = myTiles[i];
            NetTile otherTile = otherTiles[i];
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
}
