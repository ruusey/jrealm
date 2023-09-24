package com.jrealm.game.realm;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.jrealm.game.GamePanel;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.enemy.Monster;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.material.Material;
import com.jrealm.game.entity.material.MaterialManager;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.tiles.TileManager;
import com.jrealm.game.util.GameObjectKey;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Realm {
	private static final SecureRandom RANDOM = new SecureRandom();

	private Map<Long, Player> players;

	private Map<Long, Bullet> bullets;

	private Map<Long, Enemy> enemies;

	private Map<Long, LootContainer> loot;

	private Map<Long, Material> materials;

	private TileManager tm;
	private MaterialManager mm;

	public Realm() {
		this.players = new ConcurrentHashMap<>();
		this.bullets = new ConcurrentHashMap<>();
		this.enemies = new ConcurrentHashMap<>();
		this.loot = new ConcurrentHashMap<>();
		this.materials = new ConcurrentHashMap<>();

		SpriteSheet tileset = GameDataManager.SPRITE_SHEETS.get("tile/overworldOP.png");
		SpriteSheet treeset = GameDataManager.SPRITE_SHEETS.get("material/trees.png");
		SpriteSheet rockset = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-items-1.png");

		this.mm = new MaterialManager(64, 150);
		this.mm.setMaterial(MaterialManager.TYPE.TREE, treeset.getSprite(1, 0), 64);
		this.mm.setMaterial(MaterialManager.TYPE.TREE, treeset.getSprite(3, 0), 64);
		this.mm.setMaterial(MaterialManager.TYPE.TREE, rockset.getSprite(10, 5), 32);

		this.tm = new TileManager(tileset, 150, this.mm);
		for (GameObjectKey m : this.mm.list) {
			if (m.go instanceof Material) {
				this.addMaterial((Material) m.go);
			}
		}
		this.spawnRandomEnemies();
	}

	public long addMaterial(Material m) {
		long randomId = Realm.RANDOM.nextLong();
		m.setMaterialId(randomId);
		this.materials.put(randomId, m);

		return randomId;

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

	public Player getPlayer(long playerId) {
		return this.players.get(playerId);
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

	public Enemy getEnemy(long enemyId) {
		return this.enemies.get(enemyId);
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

		for (Material e : this.materials.values()) {
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

	public void spawnRandomEnemies() {
		Vector2f v = new Vector2f((0 + (GamePanel.width / 2)) - 32, (0 + (GamePanel.height / 2)) - 32);
		SpriteSheet enemySheet = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-bosses.png");

		SpriteSheet enemySheet1 = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-bosses-1.png");

		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < this.tm.getHeight(); i++) {
			for (int j = 0; j < this.tm.getWidth(); j++) {
				Enemy go = null;
				int doSpawn = r.nextInt(200);
				if ((doSpawn > 195) && (i > 0) && (j > 0)) {
					Vector2f spawnPos = new Vector2f(j * 64, i * 64);
					AABB bounds = new AABB(spawnPos, 64, 64);
					if (bounds.distance(v) < 512) {
						continue;
					}
					switch (r.nextInt(5)) {
					case 0:
						go = new Monster(0,
								new SpriteSheet(enemySheet.getSprite(7, 4, 16, 16), "Cube God", 16, 16, 0), spawnPos,
								64);
						go.setPos(spawnPos);
						break;
					case 1:
						go = new Monster(2,
								new SpriteSheet(enemySheet.getSprite(5, 4, 16, 16), "Skull Shrine", 16, 16, 0),
								spawnPos, 64);
						go.setPos(spawnPos);
						break;
					case 2:
						go = new Monster(1,
								new SpriteSheet(enemySheet.getSprite(0, 4, 16, 16), "Ghost God", 16, 16, 0),
								new Vector2f(j * 64, i * 64), 64);
						go.setPos(new Vector2f(j * 64, i * 64));
						break;
					case 3:
						go = new Monster(7,
								new SpriteSheet(enemySheet.getSprite(2, 1, 16, 16), "Medusa", 16, 16, 0),
								new Vector2f(j * 64, i * 64), 64);
						go.setPos(new Vector2f(j * 64, i * 64));
						break;
					case 4:
						go = new Monster(8,
								new SpriteSheet(enemySheet.getSprite(1, 0, 16, 16), "Red Demon", 16, 16, 0),
								new Vector2f(j * 64, i * 64), 64);
						go.setPos(new Vector2f(j * 64, i * 64));
						break;

					}
					this.addEnemy(go);
				}
			}
		}
	}
}
