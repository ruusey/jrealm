package com.jrealm.game.realm;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.AABB;

public class Realm {
	private static final SecureRandom RANDOM = new SecureRandom();

	private Map<Long, Player> players;

	private Map<Long, Bullet> bullets;

	private Map<Long, Enemy> enemies;

	private Map<Long, LootContainer> loot;

	public Realm() {
		this.players = new ConcurrentHashMap<>();
		this.bullets = new ConcurrentHashMap<>();
		this.enemies = new ConcurrentHashMap<>();
		this.loot = new ConcurrentHashMap<>();
	}


	public long addPlayer(Player player) {
		long randomId = Realm.RANDOM.nextLong();
		player.setPlayerId(randomId);
		this.players.put(randomId, player);
		return randomId;
	}

	public boolean removePlayer(Player player) {
		Player p = this.players.remove(player.getPlayerId());
		return p != null;
	}

	public long addBullet(Bullet b) {
		long randomId = Realm.RANDOM.nextLong();
		b.setBulletId(randomId);
		this.bullets.put(randomId, b);
		return randomId;
	}

	public boolean removeBullet(Bullet b) {
		Bullet bullet = this.bullets.remove(b.getBulletId());
		return bullet != null;
	}

	public long addEnemy(Enemy enemy) {
		long randomId = Realm.RANDOM.nextLong();
		enemy.setEnemyId(randomId);
		this.enemies.put(randomId, enemy);
		return randomId;
	}

	public boolean removeEnemy(Enemy enemy) {
		Enemy e = this.enemies.remove(enemy.getEnemyId());
		return e != null;
	}

	public long addLootContainer(LootContainer lc) {
		long randomId = Realm.RANDOM.nextLong();
		lc.setLootContainerId(randomId);
		this.loot.put(randomId, lc);
		return randomId;
	}

	public boolean removeLootContainer(LootContainer lc) {
		LootContainer lootContainer = this.loot.remove(lc.getLootContainerId());
		return lootContainer != null;
	}

	public AABB[] getCollisionBoxesInBounds(AABB cam) {
		List<AABB> colBoxes = new ArrayList<>();
		GameObject[] go = this.getGameObjectsInBounds(cam);
		for (GameObject g : go) {
			colBoxes.add(g.getBounds());
		}

		return colBoxes.toArray(new AABB[0]);
	}

	public GameObject[] getGameObjectsInBounds(AABB cam) {
		List<GameObject> objs = new ArrayList<>();
		for (Player p : this.players.values()) {
			if (p.getBounds().intersect(cam)) {
				objs.add(p);
			}
		}

		for (Bullet b : this.bullets.values()) {
			if (b.getBounds().intersect(cam)) {
				objs.add(b);
			}
		}

		for (Enemy e : this.enemies.values()) {
			if (e.getBounds().intersect(cam)) {
				objs.add(e);
			}
		}

		return objs.toArray(new GameObject[0]);
	}

	public LootContainer[] getLootInBounds(AABB cam) {
		List<LootContainer> objs = new ArrayList<>();
		for (LootContainer lc : this.loot.values()) {
			if (cam.inside((int) lc.getPos().x, (int) lc.getPos().y)) {
				objs.add(lc);
			}
		}

		return objs.toArray(new LootContainer[0]);

	}
}
