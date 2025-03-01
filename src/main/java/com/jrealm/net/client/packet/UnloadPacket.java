package com.jrealm.net.client.packet;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
@Builder
@AllArgsConstructor
public class UnloadPacket extends Packet {
	
	@SerializableField(order = 0, type = SerializableLong.class, isCollection=true)
    private Long[] players;
	@SerializableField(order = 1, type = SerializableLong.class, isCollection=true)
    private Long[] bullets;
	@SerializableField(order = 2, type = SerializableLong.class, isCollection=true)
    private Long[] enemies;
	@SerializableField(order = 3, type = SerializableLong.class, isCollection=true)
    private Long[] containers;
	@SerializableField(order = 4, type = SerializableLong.class, isCollection=true)
    private Long[] portals;

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
    	UnloadPacket read = IOService.readPacket(getClass(), data);
    	this.players = read.getPlayers();
    	this.bullets = read.getBullets();
    	this.enemies = read.getEnemies();
    	this.containers = read.getContainers();
    	this.portals = read.getPortals();
    	this.setId(PacketType.UNLOAD.getPacketId());
    }

    @Override
    public int serializeWrite(DataOutputStream stream) throws Exception {
		return IOService.writePacket(this, stream).length;
    }

    public static UnloadPacket from(Long[] players, Long[] bullets, Long[] enemies, Long[] containers, Long[] portals)
            throws Exception {
    	final UnloadPacket packet = UnloadPacket.builder().players(players).containers(containers).bullets(bullets).enemies(enemies).portals(portals).build();
    	packet.setId(PacketType.UNLOAD.getPacketId());
    	return packet;
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
