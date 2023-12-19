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

import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class LoadPacket extends Packet {

	private Player[] players;
	private Enemy[] enemies;
	private Bullet[] bullets;
	private LootContainer[] containers;
	
	public LoadPacket() {

	}

	public LoadPacket(final byte id, final byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			log.error("Failed to parse LoadPacket packet, Reason: {}", e);
		}
	}

	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if (dis == null || dis.available() < 5)
			throw new IllegalStateException("No Packet data available to read from DataInputStream");
		int playersSize = dis.readInt();
		this.players = new Player[playersSize];
		for(int i = 0; i< playersSize; i++) {
			this.players[i] = Player.fromStream(dis);
		}
		
		int containersSize = dis.readInt();
		this.containers = new LootContainer[containersSize];
		for(int i = 0; i< containersSize; i++) {
			this.containers[i] = new LootContainer().read(dis);
		}
		
		int bulletsSize = dis.readInt();
		this.bullets = new Bullet[bulletsSize];
		for(int i = 0; i < bulletsSize ; i++) {
			this.bullets[i] = Bullet.fromStream(dis);
		}
		
		int enemiesSize = dis.readInt();
		this.enemies = new Enemy[enemiesSize];
		for(int i = 0; i < enemiesSize; i++) {
			this.enemies[i] = Enemy.fromStream(dis);
		}
	}

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");

		this.addHeader(stream);
		stream.writeInt(this.players.length);
		for(Player p : this.players) {
			p.write(stream);
		}
		
		stream.writeInt(this.containers.length);
		for(LootContainer l : this.containers) {
			l.write(stream);
		}
		
		stream.writeInt(this.bullets.length);
		for(Bullet b : this.bullets) {
			b.write(stream);
		}
		
		stream.writeInt(enemies.length);
		for(Enemy e: enemies) {
			e.write(stream);
		}
	}
	
	public static LoadPacket from(Player[] players, LootContainer[] loot, Bullet[] bullets, Enemy[] enemies) throws Exception{
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream stream = new DataOutputStream(byteStream);
		stream.writeInt(players.length);
		for(Player p : players) {
			p.write(stream);
		}
		
		stream.writeInt(loot.length);
		for(LootContainer l : loot) {
			l.write(stream);
		}
		
		stream.writeInt(bullets.length);
		for(Bullet b : bullets) {
			b.write(stream);
		}
		
		stream.writeInt(enemies.length);
		for(Enemy e: enemies) {
			e.write(stream);
		}
		return new LoadPacket(PacketType.LOAD.getPacketId(), byteStream.toByteArray());
	}
	
	public boolean equals(LoadPacket other) {
		List<Long> playerIdsThis = Stream.of(this.players).map(player->player.getId()).collect(Collectors.toList());
		List<Long> playerIdsOther = Stream.of(other.getPlayers()).map(player->player.getId()).collect(Collectors.toList());
		
		List<Long> lootIdsThis =Stream.of(this.containers).map(container->container.getLootContainerId()).collect(Collectors.toList());
		List<Long> lootIdsOther =Stream.of(other.getContainers()).map(container->container.getLootContainerId()).collect(Collectors.toList());
		
		List<Long> enemyIdsThis =Stream.of(this.enemies).map(enemy->enemy.getId()).collect(Collectors.toList());
		List<Long> enemyIdsOther =Stream.of(other.getEnemies()).map(enemy->enemy.getId()).collect(Collectors.toList());

		List<Long> bulletIdsThis =Stream.of(this.bullets).map(bullet->bullet.getId()).collect(Collectors.toList());
		List<Long> bulletIdsOther =Stream.of(other.getBullets()).map(bullet->bullet.getId()).collect(Collectors.toList());
		
		
		boolean containersEq = true;
		if(this.containers.length!=other.getContainers().length) {
			containersEq = false;
		}
		
		if(containersEq) {
			for(int i = 0; i<this.containers.length; i++) {
				if(!this.containers[i].equals(other.getContainers()[i])) {
					containersEq = false;
					break;
				}
			}
		}

//		for(LootContainer c : this.containers) {
//			if(c.getContentsChanged()) {
//				return false;
//			}
//		}
//		
//		for(LootContainer c : other.getContainers()) {
//			if(c.getContentsChanged()) {
//				return false;
//			}
//		}
		return (playerIdsThis.equals(playerIdsOther) && lootIdsThis.equals(lootIdsOther) && enemyIdsThis.equals(enemyIdsOther) && bulletIdsThis.equals(bulletIdsOther) && containersEq);

	}
	
	public LoadPacket combine(LoadPacket other) throws Exception {
		List<Long> playerIdsThis = Stream.of(this.players).map(player->player.getId()).collect(Collectors.toList());		
		List<Long> lootIdsThis =Stream.of(this.containers).map(container->container.getLootContainerId()).collect(Collectors.toList());
		List<Long> enemyIdsThis =Stream.of(this.enemies).map(enemy->enemy.getId()).collect(Collectors.toList());
		List<Long> bulletIdsThis =Stream.of(this.bullets).map(bullet->bullet.getId()).collect(Collectors.toList());
		
		List<Bullet> bullets = Arrays.asList(other.getBullets());
		List<Player> players = Arrays.asList(other.getPlayers());
		List<LootContainer> loot = Arrays.asList(other.getContainers());
		List<Enemy> enemies = Arrays.asList(other.getEnemies());

		List<Bullet> bulletsDiff = new ArrayList<>();
		for(Bullet b : bullets) {
			if(!bulletIdsThis.contains(b.getId())) {
				bulletsDiff.add(b);
			}
		}

		List<Player> playersDiff = new ArrayList<>();
		for(Player p : players) {
			if(!playerIdsThis.contains(p.getId())) {
				playersDiff.add(p);
			}
		}
		
		List<LootContainer> lootDiff = new ArrayList<>();
		for(LootContainer p : loot) {
			if(!lootIdsThis.contains(p.getLootContainerId()) || p.getContentsChanged()) {
				lootDiff.add(p);
			}
		}
		
		List<Enemy> enemyDiff = new ArrayList<>();
		for(Enemy e : enemies) {
			if(!enemyIdsThis.contains(e.getId())) {
				enemyDiff.add(e);
			}
		}
		
		return LoadPacket.from(playersDiff.toArray(new Player[0]), lootDiff.toArray(new LootContainer[0]), bulletsDiff.toArray(new Bullet[0]), enemyDiff.toArray(new Enemy[0]));
	}
	
	public UnloadPacket difference(LoadPacket other) throws Exception {
		List<Long> playerIdsOther = Stream.of(other.getPlayers()).map(player->player.getId()).collect(Collectors.toList());
		List<Long> lootIdsOther = Stream.of(other.getContainers()).map(container->container.getLootContainerId()).collect(Collectors.toList());
		List<Long> enemyIdsOther = Stream.of(other.getEnemies()).map(enemy->enemy.getId()).collect(Collectors.toList());
		List<Long> bulletIdsOther = Stream.of(other.getBullets()).map(bullet->bullet.getId()).collect(Collectors.toList());
		
		List<Bullet> bullets = Arrays.asList(this.getBullets());
		List<Player> players = Arrays.asList(this.getPlayers());
		List<LootContainer> loot = Arrays.asList(this.getContainers());
		List<Enemy> enemies = Arrays.asList(this.getEnemies());

		
		List<Long> bulletsDiff = new ArrayList<>();
		// Dont diff the bullets as this could cause bad player
		// experience when bullets randomly despawn
		// (bullets will despawn when the owner enemy is unloaded)
//		for(Bullet b : bullets) {
//			if(!!bulletIdsOther.contains(b.getId())) {
//				bulletsDiff.add(b.getId());
//			}
//		}
		

		List<Long> playersDiff = new ArrayList<>();
		for(Player p : players) {
			if(!playerIdsOther.contains(p.getId())) {
				playersDiff.add(p.getId());
			}
		}
		
		List<Long> lootDiff = new ArrayList<>();
		for(LootContainer p : loot) {
			if(!lootIdsOther.contains(p.getLootContainerId())) {
				lootDiff.add(p.getLootContainerId());
			}
		}
		
		List<Long> enemyDiff = new ArrayList<>();
		for(Enemy e : enemies) {
			if(!enemyIdsOther.contains(e.getId())) {
				enemyDiff.add(e.getId());
			}
		}
		
		return UnloadPacket.from(playersDiff.toArray(new Long[0]), lootDiff.toArray(new Long[0]), bulletsDiff.toArray(new Long[0]), enemyDiff.toArray(new Long[0]));
	}
}
