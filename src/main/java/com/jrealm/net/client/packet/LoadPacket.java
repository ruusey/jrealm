package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import com.jrealm.net.entity.NetTile;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Streamable
public class LoadPacket extends Packet {
	@SerializableField(order = 0, type = NetPlayer.class)
    private NetPlayer[] players;
	@SerializableField(order = 1, typeList = NetEnemy[].class)
    private NetEnemy[] enemies;
	@SerializableField(order = 2, typeList = NetBullet[].class)
    private NetBullet[] bullets;
	@SerializableField(order = 3, typeList = NetLootContainer[].class)
    private NetLootContainer[] containers;
	@SerializableField(order = 4, typeList = NetPortal[].class)
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
        
        LoadPacket packet = IOService.read(getClass(), dis);
        this.players = packet.getPlayers();
        this.enemies = packet.getEnemies();
        this.bullets = packet.getBullets();
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if ((this.getId() < 1) || (this.getData() == null) || (this.getData().length < 5))
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");

       IOService.write(this, stream);
    }

    public static LoadPacket from(Player[] players, LootContainer[] loot, Bullet[] bullets, Enemy[] enemies,
            Portal[] portals) throws Exception {
    	final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    	final DataOutputStream stream = new DataOutputStream(byteStream);
        stream.writeInt(players.length);
        for (Player p : players) {
            p.write(stream);
        }

        stream.writeInt(loot.length);
        for (LootContainer l : loot) {
            //l.write(l, stream);
        }

        stream.writeInt(bullets.length);
        for (Bullet b : bullets) {
            //b.write(stream);
        }

        stream.writeInt(enemies.length);
        for (Enemy e : enemies) {
            e.write(stream);
        }

        stream.writeInt(portals.length);
        for (Portal p : portals) {
            p.write(stream);
        }
        return new LoadPacket(PacketType.LOAD.getPacketId(), byteStream.toByteArray());
    }

    public boolean equals(LoadPacket other) {
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

        for (final LootContainer c : this.containers) {
            if (c.getContentsChanged()) {
                containersEq = false;
                break;
            }
        }

        for (final LootContainer c : other.getContainers()) {
            if (c.getContentsChanged()) {
                containersEq = false;
                break;
            }
        }

        for (final LootContainer c : this.getContainers()) {
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
        final List<Long> playerIdsThis = Stream.of(this.players).map(Player::getId).collect(Collectors.toList());
        final List<Long> lootIdsThis = Stream.of(this.containers).map(LootContainer::getLootContainerId)
                .collect(Collectors.toList());
        final List<Long> enemyIdsThis = Stream.of(this.enemies).map(Enemy::getId).collect(Collectors.toList());
        final List<Long> bulletIdsThis = Stream.of(this.bullets).map(Bullet::getId).collect(Collectors.toList());
        final List<Long> portalIdsThis = Stream.of(this.portals).map(Portal::getId).collect(Collectors.toList());

        final List<Bullet> bullets = Arrays.asList(other.getBullets());
        final List<Player> players = Arrays.asList(other.getPlayers());
        final List<LootContainer> loot = Arrays.asList(other.getContainers());
        final List<Enemy> enemies = Arrays.asList(other.getEnemies());
        final List<Portal> portals = Arrays.asList(other.getPortals());

        final List<Bullet> bulletsDiff = new ArrayList<>();
        for (final Bullet b : bullets) {
            if (!bulletIdsThis.contains(b.getId())) {
                bulletsDiff.add(b);
            }
        }

        final List<Player> playersDiff = new ArrayList<>();
        for (final Player p : players) {
            if (!playerIdsThis.contains(p.getId())) {
                playersDiff.add(p);
            }
        }

        final List<LootContainer> lootDiff = new ArrayList<>();
        for (final LootContainer p : loot) {
            if (!lootIdsThis.contains(p.getLootContainerId()) || p.getContentsChanged()) {
                lootDiff.add(p);
            }
        }

        final List<Enemy> enemyDiff = new ArrayList<>();
        for (final Enemy e : enemies) {
            if (!enemyIdsThis.contains(e.getId())) {
                enemyDiff.add(e);
            }
        }

        final List<Portal> portalDiff = new ArrayList<>();
        for (final Portal p : portals) {
            if (!portalIdsThis.contains(p.getId())) {
                portalDiff.add(p);
            }
        }
        return LoadPacket.from(playersDiff.toArray(new Player[0]), lootDiff.toArray(new LootContainer[0]),
                bulletsDiff.toArray(new Bullet[0]), enemyDiff.toArray(new Enemy[0]), portalDiff.toArray(new Portal[0]));
    }

    public UnloadPacket difference(LoadPacket other) throws Exception {
        final List<Long> playerIdsOther = Stream.of(other.getPlayers()).map(Player::getId).collect(Collectors.toList());
        final List<Long> lootIdsOther = Stream.of(other.getContainers()).map(LootContainer::getLootContainerId)
                .collect(Collectors.toList());
        final List<Long> bulletIdsOther = Stream.of(other.getBullets()).map(Bullet::getId).collect(Collectors.toList());
        final List<Long> enemyIdsOther = Stream.of(other.getEnemies()).map(Enemy::getId).collect(Collectors.toList());
        final List<Long> portalIdsOther = Stream.of(other.getPortals()).map(Portal::getId).collect(Collectors.toList());

        final List<Player> players = Arrays.asList(this.getPlayers());
        final List<LootContainer> loot = Arrays.asList(this.getContainers());
        final List<Bullet> bullets = Arrays.asList(this.getBullets());
        final List<Enemy> enemies = Arrays.asList(this.getEnemies());
        final List<Portal> portals = Arrays.asList(this.getPortals());

        final List<Long> bulletsDiff = new ArrayList<>();
        for (final Bullet b : bullets) {
            if (!bulletIdsOther.contains(b.getId())) {
                bulletsDiff.add(b.getId());
            }
        }

        final List<Long> portalsDiff = new ArrayList<>();
        for (final Portal p : portals) {
            if (!portalIdsOther.contains(p.getId())) {
                portalsDiff.add(p.getId());
            }
        }

        final List<Long> playersDiff = new ArrayList<>();
        for (final Player p : players) {
            if (!playerIdsOther.contains(p.getId())) {
                playersDiff.add(p.getId());
            }
        }

        final List<Long> lootDiff = new ArrayList<>();
        for (final LootContainer p : loot) {
            if (!lootIdsOther.contains(p.getLootContainerId())) {
                lootDiff.add(p.getLootContainerId());
            }
        }

        final List<Long> enemyDiff = new ArrayList<>();
        for (final Enemy e : enemies) {
            if (!enemyIdsOther.contains(e.getId())) {
                enemyDiff.add(e.getId());
            }
        }

        return UnloadPacket.from(playersDiff.toArray(new Long[0]), lootDiff.toArray(new Long[0]),
                bulletsDiff.toArray(new Long[0]), enemyDiff.toArray(new Long[0]), portalsDiff.toArray(new Long[0]));
    }

    public boolean containsPlayer(final Long player) {
        for (final Player p : this.players) {
            if (p.getId() == player)
                return true;
        }

        return false;
    }

    public boolean containsEnemy(final Long enemy) {
        for (final Enemy e : this.enemies) {
            if (e.getId() == enemy)
                return true;
        }

        return false;
    }

    public boolean containsBullet(final Long bullet) {
        for (final Bullet b : this.bullets) {
            if (b.getId() == bullet)
                return true;
        }

        return false;
    }

    public boolean containsLootContainer(final Long container) {
        for (final LootContainer lc : this.containers) {
            if (lc.getLootContainerId() == container)
                return true;
        }

        return false;
    }

    public boolean containsPortal(final Long portal) {
        for (final Portal p : this.portals) {
            if (p.getId() == portal)
                return true;
        }
        return false;
    }
}
