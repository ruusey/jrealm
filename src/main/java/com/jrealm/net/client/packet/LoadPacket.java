package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.entity.NetBullet;
import com.jrealm.net.entity.NetEnemy;
import com.jrealm.net.entity.NetLootContainer;
import com.jrealm.net.entity.NetPlayer;
import com.jrealm.net.entity.NetPortal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
@AllArgsConstructor
public class LoadPacket extends Packet {
	@SerializableField(order = 0, type = NetPlayer.class, isCollection=true)
    private NetPlayer[] players;
	@SerializableField(order = 1, type = NetEnemy.class, isCollection=true)
    private NetEnemy[] enemies;
	@SerializableField(order = 2, type = NetBullet.class, isCollection=true)
    private NetBullet[] bullets;
	@SerializableField(order = 3, type = NetLootContainer.class, isCollection=true)
    private NetLootContainer[] containers;
	@SerializableField(order = 4, type = NetPortal.class, isCollection=true)
    private NetPortal[] portals;

    public LoadPacket() {

    }

    public LoadPacket(final byte id, final byte[] data) {
        super(id, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            LoadPacket.log.error("Failed to parse LoadPacket packet, Reason: {}", e);
        }
    }

    @Override
    public void readData(byte[] data) throws Exception {
    	final ByteArrayInputStream bis = new ByteArrayInputStream(data);
    	final DataInputStream dis = new DataInputStream(bis);
        if ((dis == null) || (dis.available() < 5))
            throw new IllegalStateException("No Packet data available to read from DataInputStream");	
        
        LoadPacket packet = IOService.readPacket(getClass(), dis);
        this.players = packet.getPlayers();
        this.enemies = packet.getEnemies();
        this.bullets = packet.getBullets();
        this.containers = packet.getContainers();
        this.portals = packet.getPortals();
    	this.setId(PacketType.LOAD.getPacketId());

    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
       IOService.writePacket(this, stream);
    }

    public static LoadPacket from(Player[] players, LootContainer[] loot, Bullet[] bullets, Enemy[] enemies,
            Portal[] portals) throws Exception {
    	 LoadPacket load = null;
    	try {
            final NetPlayer[] mappedPlayers = IOService.mapModel(players, NetPlayer[].class);
            final NetEnemy[] mappedEnemies = IOService.mapModel(enemies, NetEnemy[].class);
            final NetBullet[] mappedBullets = IOService.mapModel(bullets, NetBullet[].class);
            final NetLootContainer[] mappedLoot = IOService.mapModel(loot, NetLootContainer[].class);
            final NetPortal[] mappedPortals = IOService.mapModel(portals, NetPortal[].class);
            load = new LoadPacket(mappedPlayers, mappedEnemies, mappedBullets, mappedLoot, mappedPortals);
        	load.setId(PacketType.LOAD.getPacketId());
    	}catch(Exception e) {
    		log.error("Failed to build load packet from mapped game data. Reason {}", e);
    	}


        return load;
    }

    public boolean equals(LoadPacket other) {
    	if(other == null) {
    		return false;
    	}
        final List<Long> playerIdsThis = Stream.of(this.players).map(NetPlayer::getId).collect(Collectors.toList());
        final List<Long> playerIdsOther = Stream.of(other.getPlayers()).map(NetPlayer::getId).collect(Collectors.toList());

        final List<Long> lootIdsThis = Stream.of(this.containers).map(NetLootContainer::getLootContainerId)
                .collect(Collectors.toList());
        final List<Long> lootIdsOther = Stream.of(other.getContainers()).map(NetLootContainer::getLootContainerId)
                .collect(Collectors.toList());

        final List<Long> enemyIdsThis = Stream.of(this.enemies).map(NetEnemy::getId).collect(Collectors.toList());
        final List<Long> enemyIdsOther = Stream.of(other.getEnemies()).map(NetEnemy::getId).collect(Collectors.toList());

        final List<Long> bulletIdsThis = Stream.of(this.bullets).map(NetBullet::getId).collect(Collectors.toList());
        final List<Long> bulletIdsOther = Stream.of(other.getBullets()).map(NetBullet::getId).collect(Collectors.toList());

        final List<Long> portalIdsThis = Stream.of(this.portals).map(NetPortal::getId).collect(Collectors.toList());
        final List<Long> portalIdsOther = Stream.of(other.getPortals()).map(NetPortal::getId).collect(Collectors.toList());

        boolean containersEq = true;
        if (this.containers.length != other.getContainers().length) {
            containersEq = false;
        }

        if (containersEq) {
            for (int i = 0; i < this.containers.length; i++) {
                if (!this.containers[i].equals(other.getContainers()[i])) {
                    containersEq = false;
                    break;
                }
            }
        }

        for (final NetLootContainer c : this.containers) {
            if (c.getContentsChanged()) {
                containersEq = false;
                break;
            }
        }


        return (playerIdsThis.equals(playerIdsOther) && lootIdsThis.equals(lootIdsOther)
                && enemyIdsThis.equals(enemyIdsOther) && bulletIdsThis.equals(bulletIdsOther) && containersEq
                && portalIdsThis.equals(portalIdsOther));

    }

    public LoadPacket combine(final LoadPacket other) throws Exception {
    	if(other==null) {
    		return this;
    	}
        final List<Long> playerIdsThis = Stream.of(this.players).map(NetPlayer::getId).collect(Collectors.toList());
        final List<Long> lootIdsThis = Stream.of(this.containers).map(NetLootContainer::getLootContainerId)
                .collect(Collectors.toList());
        final List<Long> enemyIdsThis = Stream.of(this.enemies).map(NetEnemy::getId).collect(Collectors.toList());
        final List<Long> bulletIdsThis = Stream.of(this.bullets).map(NetBullet::getId).collect(Collectors.toList());
        final List<Long> portalIdsThis = Stream.of(this.portals).map(NetPortal::getId).collect(Collectors.toList());

        final List<NetBullet> bullets = Arrays.asList(other.getBullets());
        final List<NetPlayer> players = Arrays.asList(other.getPlayers());
        final List<NetLootContainer> loot = Arrays.asList(other.getContainers());
        final List<NetEnemy> enemies = Arrays.asList(other.getEnemies());
        final List<NetPortal> portals = Arrays.asList(other.getPortals());

        final List<NetBullet> bulletsDiff = new ArrayList<>();
        for (final NetBullet b : bullets) {
            if (!bulletIdsThis.contains(b.getId())) {
                bulletsDiff.add(b);
            }
        }

        final List<NetPlayer> playersDiff = new ArrayList<>();
        for (final NetPlayer p : players) {
            if (!playerIdsThis.contains(p.getId())) {
                playersDiff.add(p);
            }
        }

        final List<NetLootContainer> lootDiff = new ArrayList<>();
        for (final NetLootContainer p : loot) {
            if (!lootIdsThis.contains(p.getLootContainerId()) || p.getContentsChanged()) {
                lootDiff.add(p);
            }
        }

        final List<NetEnemy> enemyDiff = new ArrayList<>();
        for (final NetEnemy e : enemies) {
            if (!enemyIdsThis.contains(e.getId())) {
                enemyDiff.add(e);
            }
        }

        final List<NetPortal> portalDiff = new ArrayList<>();
        for (final NetPortal p : portals) {
            if (!portalIdsThis.contains(p.getId())) {
                portalDiff.add(p);
            }
        }
        return new LoadPacket(playersDiff.toArray(new NetPlayer[0]), enemyDiff.toArray(new NetEnemy[0]),
                bulletsDiff.toArray(new NetBullet[0]), lootDiff.toArray(new NetLootContainer[0]), portalDiff.toArray(new NetPortal[0]));
    }

    public UnloadPacket difference(LoadPacket other) throws Exception {
    	//if(other==null)
        final List<Long> playerIdsOther = Stream.of(other.getPlayers()).map(NetPlayer::getId).collect(Collectors.toList());
        final List<Long> lootIdsOther = Stream.of(other.getContainers()).map(NetLootContainer::getLootContainerId)
                .collect(Collectors.toList());
        final List<Long> bulletIdsOther = Stream.of(other.getBullets()).map(NetBullet::getId).collect(Collectors.toList());
        final List<Long> enemyIdsOther = Stream.of(other.getEnemies()).map(NetEnemy::getId).collect(Collectors.toList());
        final List<Long> portalIdsOther = Stream.of(other.getPortals()).map(NetPortal::getId).collect(Collectors.toList());

        final List<NetPlayer> players = Arrays.asList(this.getPlayers());
        final List<NetLootContainer> loot = Arrays.asList(this.getContainers());
        final List<NetBullet> bullets = Arrays.asList(this.getBullets());
        final List<NetEnemy> enemies = Arrays.asList(this.getEnemies());
        final List<NetPortal> portals = Arrays.asList(this.getPortals());

        final List<Long> bulletsDiff = new ArrayList<>();
        for (final NetBullet b : bullets) {
            if (!bulletIdsOther.contains(b.getId())) {
                bulletsDiff.add(b.getId());
            }
        }

        final List<Long> portalsDiff = new ArrayList<>();
        for (final NetPortal p : portals) {
            if (!portalIdsOther.contains(p.getId())) {
                portalsDiff.add(p.getId());
            }
        }

        final List<Long> playersDiff = new ArrayList<>();
        for (final NetPlayer p : players) {
            if (!playerIdsOther.contains(p.getId())) {
                playersDiff.add(p.getId());
            }
        }

        final List<Long> lootDiff = new ArrayList<>();
        for (final NetLootContainer p : loot) {
            if (!lootIdsOther.contains(p.getLootContainerId())) {
                lootDiff.add(p.getLootContainerId());
            }
        }

        final List<Long> enemyDiff = new ArrayList<>();
        for (final NetEnemy e : enemies) {
            if (!enemyIdsOther.contains(e.getId())) {
                enemyDiff.add(e.getId());
            }
        }

		return UnloadPacket.from(playersDiff.toArray(new Long[0]), bulletsDiff.toArray(new Long[0]),
				enemyDiff.toArray(new Long[0]), lootDiff.toArray(new Long[0]), portalsDiff.toArray(new Long[0]));
    }

    public boolean containsPlayer(final Long player) {
        for (final NetPlayer p : this.players) {
            if (p.getId() == player)
                return true;
        }

        return false;
    }

    public boolean containsEnemy(final Long enemy) {
        for (final NetEnemy e : this.enemies) {
            if (e.getId() == enemy)
                return true;
        }

        return false;
    }

    public boolean containsBullet(final Long bullet) {
        for (final NetBullet b : this.bullets) {
            if (b.getId() == bullet)
                return true;
        }

        return false;
    }

    public boolean containsLootContainer(final Long container) {
        for (final NetLootContainer lc : this.containers) {
            if (lc.getLootContainerId() == container)
                return true;
        }

        return false;
    }

    public boolean containsPortal(final Long portal) {
        for (final NetPortal p : this.portals) {
            if (p.getId() == portal)
                return true;
        }
        return false;
    }
}
