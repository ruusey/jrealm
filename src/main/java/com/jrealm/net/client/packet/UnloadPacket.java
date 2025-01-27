package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class UnloadPacket extends Packet {
	@SerializableField(order = 0, type = SerializableLongArray.class)
    private long[] players;
	@SerializableField(order = 1, type = SerializableLongArray.class)
    private long[] bullets;
	@SerializableField(order = 2, type = SerializableLongArray.class)
    private long[] enemies;
	@SerializableField(order = 3, type = SerializableLongArray.class)
    private long[] containers;
	@SerializableField(order = 4, type = SerializableLongArray.class)
    private long[] portals;

    public UnloadPacket() {

    }

    public UnloadPacket(final byte id, final byte[] data) {
        super(id, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            UnloadPacket.log.error("Failed to parse LoadPacket packet, Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
    	final ByteArrayInputStream bis = new ByteArrayInputStream(data);
    	final DataInputStream dis = new DataInputStream(bis);
        if ((dis == null) || (dis.available() < 5))
            throw new IllegalStateException("No Packet data available to read from DataInputStream");
        int playersSize = dis.readInt();
        this.players = new long[playersSize];
        for (int i = 0; i < playersSize; i++) {
            this.players[i] = dis.readLong();
        }
 
        int bulletsSize = dis.readInt();
        this.bullets = new long[bulletsSize];
        for (int i = 0; i < bulletsSize; i++) {
            this.bullets[i] = dis.readLong();
        }

        int enemiesSize = dis.readInt();
        this.enemies = new long[enemiesSize];
        for (int i = 0; i < enemiesSize; i++) {
            this.enemies[i] = dis.readLong();
        }
        
        int containersSize = dis.readInt();
        this.containers = new long[containersSize];
        for (int i = 0; i < containersSize; i++) {
            this.containers[i] = dis.readLong();
        }

        int portalsSize = dis.readInt();
        this.portals = new long[portalsSize];
        for (int i = 0; i < portalsSize; i++) {
            this.portals[i] = dis.readLong();
        }

    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if ((this.getId() < 1) || (this.getData() == null) || (this.getData().length < 5))
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");

        this.addHeader(stream);
        stream.writeInt(this.players.length);
        for (long p : this.players) {
            stream.writeLong(p);
        }
        
        stream.writeInt(this.bullets.length);
        for (long b : this.bullets) {
            stream.writeLong(b);
        }
        
        stream.writeInt(this.enemies.length);
        for (long e : this.enemies) {
            stream.writeLong(e);
        }

        stream.writeInt(this.containers.length);
        for (long l : this.containers) {
            stream.writeLong(l);
        }

        stream.writeInt(this.portals.length);
        for (long p : this.portals) {
            stream.writeLong(p);
        }
    }

    public static UnloadPacket from(Long[] players, Long[] containers, Long[] bullets, Long[] enemies, Long[] portals)
            throws Exception {
    	final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    	final DataOutputStream stream = new DataOutputStream(byteStream);
        stream.writeInt(players.length);
        for (long p : players) {
            stream.writeLong(p);
        }
        
        stream.writeInt(bullets.length);
        for (long b : bullets) {
            stream.writeLong(b);
        }

        stream.writeInt(enemies.length);
        for (long e : enemies) {
            stream.writeLong(e);
        }
        
        stream.writeInt(containers.length);
        for (long l : containers) {
            stream.writeLong(l);
        }

        stream.writeInt(portals.length);
        for (long p : portals) {
            stream.writeLong(p);
        }
        return new UnloadPacket(PacketType.UNLOAD.getPacketId(), byteStream.toByteArray());
    }

    public boolean isNotEmpty() {
        return (this.getEnemies().length > 0) || (this.getContainers().length > 0) || (this.getPlayers().length > 0)
                || (this.getPortals().length > 0) || (this.bullets.length > 0);
    }

    public boolean equals(UnloadPacket other) {
        final boolean players = Arrays.equals(this.players, other.getPlayers());
        final boolean enemies = Arrays.equals(this.enemies, other.getEnemies());
        final boolean loot = Arrays.equals(this.containers, other.getContainers());
        final boolean bullets = Arrays.equals(this.bullets, other.getBullets());
        final boolean portals = Arrays.equals(this.portals, other.getPortals());
        return players && enemies && loot && bullets && portals;
    }

    // Removes entities that are not in the provided load state
    public UnloadPacket cullFromLoadPacket(LoadPacket loadState) throws Exception {
        final List<Long> players = new ArrayList<>();
        for (final Long playerId : this.players) {
            if (loadState.containsPlayer(playerId)) {
                players.add(playerId);
            }
        }

        final List<Long> enemies = new ArrayList<>();
        for (final Long enemyId : this.enemies) {
            if (loadState.containsEnemy(enemyId)) {
                enemies.add(enemyId);
            }
        }

        final List<Long> loot = new ArrayList<>();
        for (final Long lootId : this.containers) {
            if (loadState.containsLootContainer(lootId)) {
                loot.add(lootId);
            }
        }

        final List<Long> bullets = new ArrayList<>();
        for (final Long bulletId : this.bullets) {
            if (loadState.containsBullet(bulletId)) {
                bullets.add(bulletId);
            }
        }

        final List<Long> portals = new ArrayList<>();
        for (final Long portalId : this.portals) {
            if (loadState.containsPortal(portalId)) {
                portals.add(portalId);
            }
        }

        return UnloadPacket.from(players.toArray(new Long[0]), loot.toArray(new Long[0]), bullets.toArray(new Long[0]),
                enemies.toArray(new Long[0]), portals.toArray(new Long[0]));

    }
}
