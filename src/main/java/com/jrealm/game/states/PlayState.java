package com.jrealm.game.states;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import com.jrealm.game.GamePanel;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.enemy.Monster;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.material.Material;
import com.jrealm.game.entity.material.MaterialManager;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.tiles.TileManager;
import com.jrealm.game.ui.PlayerUI;
import com.jrealm.game.util.AABBTree;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.GameObjectHeap;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;
import com.jrealm.game.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PlayState extends GameState {

	public Player player;
	private GameObjectHeap gameObject;
	private List<LootContainer> loot;
	private AABBTree aabbTree;
	private TileManager tm;
	private MaterialManager mm;
	private Camera cam;
	private PlayerUI pui;
	public static Vector2f map;
	private double heaptime;

	private List<Vector2f> shotDestQueue;
	public long lastShotTick = 0;
	private Semaphore gameObjectLock = new Semaphore(1);
	public PlayState(GameStateManager gsm, Camera cam) {
		super(gsm);

		PlayState.map = new Vector2f();
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
		this.cam = cam;

		// this.tm = new TileManager("tile/tilemap.xml", cam);
		GameDataManager.SPRITE_SHEETS.get("entity/rotmg-classes.png");
		SpriteSheet tileset = GameDataManager.SPRITE_SHEETS.get("tile/overworldOP.png");
		SpriteSheet treeset = GameDataManager.SPRITE_SHEETS.get("material/trees.png");

		this.mm = new MaterialManager(64, 150);
		this.mm.setMaterial(MaterialManager.TYPE.TREE, treeset.getSprite(1, 0), 64);
		this.mm.setMaterial(MaterialManager.TYPE.TREE, treeset.getSprite(3, 0), 64);

		this.tm = new TileManager(tileset, 150, cam, this.mm);
		this.shotDestQueue = new ArrayList<>();
		this.gameObject = new GameObjectHeap();
		//gameObject.addAll(mm.list);
		this.aabbTree = new AABBTree();
		this.loot = new ArrayList<>();
		this.player = new Player(4, cam, GameDataManager.SPRITE_SHEETS.get("entity/rotmg-classes.png"),
				new Vector2f((0 + (GamePanel.width / 2)) - 32, (0 + (GamePanel.height / 2)) - 32), 64, this.tm);
		this.player.setIsInvincible(true);
		this.spawnRandomEnemies();

		this.pui = new PlayerUI(this.player);
		this.aabbTree.insert(this.player);

		cam.target(this.player);
		this.player.setIsInvincible(false);

		this.player.equipSlot(0, GameDataManager.GAME_ITEMS.get(5));
		this.player.equipSlot(2, GameDataManager.GAME_ITEMS.get(19));
		this.player.equipSlot(3, GameDataManager.GAME_ITEMS.get(3));

		this.getPui().setEquipment(this.player.getEquipment());

	}

	private void spawnRandomEnemies() {
		SpriteSheet enemySheet = GameDataManager.SPRITE_SHEETS.get("entity/rotmg-bosses.png");


		Random r = new Random(System.currentTimeMillis());

		for (int i = 0; i < this.tm.getHeight(); i++) {
			for (int j = 0; j < this.tm.getWidth(); j++) {
				GameObject go = null;
				int doSpawn = r.nextInt(200);
				if (doSpawn > 195) {
					switch (r.nextInt(3)) {
					case 0:
						go = new Monster(0, this.cam,
								new SpriteSheet(enemySheet.getSprite(7, 4, 16, 16), "Cube God", 16, 16, 0),
								new Vector2f(j * 64, i * 64),
								64);
						go.setPos(new Vector2f(j * 64, i * 64));
						break;
					case 1:
						go = new Monster(2, this.cam,
								new SpriteSheet(enemySheet.getSprite(5, 4, 16, 16), "Skull Shrine", 16, 16, 0),
								new Vector2f(j * 64, i * 64),
								64);
						go.setPos(new Vector2f(j * 64, i * 64));
						break;
					case 2:
						go = new Monster(1, this.cam,
								new SpriteSheet(enemySheet.getSprite(0, 4, 16, 16), "Ghost God", 16, 16, 0),
								new Vector2f(j * 64, i * 64), 64);
						go.setPos(new Vector2f(j * 64, i * 64));
						break;
					}
					this.getGameObjects().add(go.getBounds().distance(this.getPlayerPos()), go);
					this.getAABBObjects().insert(go);
				}
			}
		}
	}

	public GameObjectHeap getGameObjects() { return this.gameObject; }
	public AABBTree getAABBObjects() { return this.aabbTree; }
	public Vector2f getPlayerPos() { return this.player.getPos(); }

	private boolean canBuildHeap(int offset, int si, double time) {

		if((this.gameObject.size() > 3) && (((this.heaptime / si) + offset) < (time / si)))
			return true;

		return false;
	}

	@Override
	public void update(double time) {
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);

		if(!this.gsm.isStateActive(GameStateManager.PAUSE)) {
			if(!this.gsm.isStateActive(GameStateManager.EDIT)) {

				if(this.player.getDeath()) {
					this.gsm.add(GameStateManager.GAMEOVER);
					this.gsm.pop(GameStateManager.PLAY);
				}
				Runnable playerShootDequeue = () -> {
					for (int i = 0; i < this.shotDestQueue.size(); i++) {
						Vector2f dest = this.shotDestQueue.remove(i);
						dest.addX(PlayState.map.x);
						dest.addY(PlayState.map.y);
						Vector2f source = this.getPlayerPos().clone(this.player.getSize() / 2,
								this.player.getSize() / 2);
						ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(this.getPlayer().getWeaponId());
						float angle = Bullet.getAngle(source, dest);

						for (Projectile p : group.getProjectiles()) {
							short offset = (short) (p.getSize() / (short) 2);
							short rolledDamage = this.player.getEquipment()[0].getDamage().getInRange();
							this.addProjectile(this.player.getWeaponId(), source.clone(-offset, -offset),
									angle + Float.parseFloat(p.getAngle()), p.getSize(), p.getMagnitude(),
									p.getRange(), rolledDamage, false);
						}

					}
				};

				Runnable processGameObjects = () -> {
					this.acquireGameObjectLock();
					for (int i = 0; i < this.gameObject.size(); i++) {
						if (this.gameObject.get(i).go instanceof Enemy) {
							Enemy enemy = ((Enemy) this.gameObject.get(i).go);
							enemy.update(this, time);

							if (this.canBuildHeap(2500, 1000000, time)) {
								this.gameObject.get(i).value = enemy.getBounds().distance(this.player.getPos());
							}

							continue;
						}

						if (this.gameObject.get(i).go instanceof Material) {
							Material mat = ((Material) this.gameObject.get(i).go);
							if (this.player.getHitBounds().collides(mat.getBounds())) {
								this.player.setTargetGameObject(mat);
							}
						}

						if (this.gameObject.get(i).go instanceof Bullet) {
							Bullet bullet = ((Bullet) this.gameObject.get(i).go);
							if (bullet != null) {
								if (bullet.remove()) {
									this.gameObject.remove(bullet);
									this.aabbTree.removeObject(bullet);
								} else {
									bullet.update();
								}
							}
						}
					}
					this.releaseGameObjectLock();

				};

				Runnable render = () -> {
					this.acquireGameObjectLock();

					if (this.canBuildHeap(3, 1000000000, time)) {
						this.heaptime = System.nanoTime();
						this.gameObject.buildHeap();
					}
					this.processBulletHit();

					this.player.update(time);
					this.pui.update(time);
					this.releaseGameObjectLock();

				};


				WorkerThread.submitAndRun(playerShootDequeue, processGameObjects, render);

			}
			this.cam.update();
		}
	}

	public void addProjectile(int projectileGroupId, Vector2f src, Vector2f dest, short size, float magnitude, float range, short damage,
			boolean isEnemy) {
		ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		Bullet b = new Bullet(projectileGroupId, bulletImage,
				src,
				dest, size, magnitude, range, damage, isEnemy);
		this.getGameObjects().add(b.getBounds().distance(this.getPlayerPos()), b);
		this.aabbTree.insert(b);
	}

	public void addProjectile(int projectileGroupId, Vector2f src, float angle, short size, float magnitude,
			float range, short damage,
			boolean isEnemy) {
		ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		Bullet b = new Bullet(projectileGroupId, bulletImage, src, angle, size, magnitude, range, damage, isEnemy);
		this.getGameObjects().add(b.getBounds().distance(this.getPlayerPos()), b);
		this.aabbTree.insert(b);
	}

	public void processBulletHit() {
		List<Bullet> results = this.getBullets();

		for (int i = 0; i < this.gameObject.size(); i++) {
			if (this.gameObject.get(i).go instanceof Enemy) {
				Enemy enemy = ((Enemy) this.gameObject.get(i).go);
				for (Bullet b : results) {
					this.processPlayerHit(b, this.getPlayer());
					this.proccessEnemyHit(b, enemy);
				}
			}

			else if (this.gameObject.get(i).go instanceof Material) {
				Material mat = ((Material) this.gameObject.get(i).go);
				for (Bullet b : results) {
					if (b.getBounds().intersect(mat.getBounds())) {
						this.aabbTree.removeObject(b);
						this.gameObject.remove(b);
					}
				}
			}
		}
	}

	private void processPlayerHit(Bullet b, Player p) {
		if (b.getBounds().collides(0, 0, this.player.getBounds()) && b.isEnemy() && !b.isPlayerHit()) {
			b.setPlayerHit(true);
			this.player.getAnimation().getImage().setEffect(Sprite.effect.REDISH);

			this.player.setHealth(this.player.getHealth() - b.getDamage(), 0, false);
			this.aabbTree.removeObject(b);
			this.gameObject.remove(b);
		}
	}

	private void proccessEnemyHit(Bullet b, Enemy e) {
		if (b.getBounds().collides(0, 0, e.getBounds()) && !b.isEnemy()) {
			e.setHealth(e.getHealth() - b.getDamage(), 0, false);
			this.aabbTree.removeObject(b);
			this.gameObject.remove(b);
			if (e.getDeath()) {
				this.aabbTree.removeObject(e);
				this.gameObject.remove(e);
				this.loot.add(new LootContainer(
						GameDataManager.SPRITE_SHEETS.get("entity/rotmg-projectiles.png").getSprite(8, 0, 8, 8),
						e.getPos()));
			}
		}
	}

	private void processTerrainHit(Bullet b, Material m) {
		if (b.getBounds().intersect(m.getBounds())) {
			this.aabbTree.removeObject(b);
			this.gameObject.remove(b);
		}
	}

	private List<Bullet> getBullets() {
		List<Bullet> results = new ArrayList<>();
		for (int i = 0; i < this.gameObject.size(); i++) {
			if (this.gameObject.get(i).go instanceof Bullet) {
				results.add((Bullet) this.gameObject.get(i).go);
			}
		}
		return results;
	}


	@Override
	public void input(MouseHandler mouse, KeyHandler key) {
		key.escape.tick();
		key.f1.tick();
		key.enter.tick();

		if(!this.gsm.isStateActive(GameStateManager.PAUSE)) {
			if(this.cam.getTarget() == this.player) {
				this.player.input(mouse, key);
			}
			this.cam.input(mouse, key);

			if(key.f1.clicked) {
				if(this.gsm.isStateActive(GameStateManager.EDIT)) {
					this.gsm.pop(GameStateManager.EDIT);
					this.cam.target(this.player);
				} else {
					this.gsm.add(GameStateManager.EDIT);
					this.cam.target(null);
				}
			}

			if(key.enter.clicked) {
				System.out.println(this.aabbTree.toString());
				System.out.println(this.gameObject.toString());
			}

			this.pui.input(mouse, key);
		} else if(this.gsm.isStateActive(GameStateManager.EDIT)) {
			this.gsm.pop(GameStateManager.EDIT);
			this.cam.target(this.player);
		}

		if (key.escape.clicked) {
			if(this.gsm.isStateActive(GameStateManager.PAUSE)) {
				this.gsm.pop(GameStateManager.PAUSE);
			} else {
				this.gsm.add(GameStateManager.PAUSE);
			}
		}
		boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > 100)
				&& !this.gsm.isStateActive(GameStateManager.EDIT);

		if ((mouse.getButton() == 1) && canShoot) {
			this.lastShotTick = System.currentTimeMillis();
			Vector2f dest = new Vector2f(mouse.getX(), mouse.getY());
			this.shotDestQueue.add(dest);
		}
	}

	private void acquireGameObjectLock() {
		try {
			this.gameObjectLock.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void releaseGameObjectLock() {
		try {
			this.gameObjectLock.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render(Graphics2D g) {
		this.tm.render(g);

		this.player.render(g);
		for(int i = 0; i < this.gameObject.size(); i++) {
			if(this.cam.getBounds().collides(this.gameObject.get(i).getBounds())) {
				this.gameObject.get(i).go.render(g);
			}
		}

		LootContainer closeLoot = null;
		for (LootContainer lc : this.loot) {
			if ((this.getPlayer().getBounds().distance(lc.getPos()) < 64)) {
				closeLoot = lc;
			}
			lc.render(g);
		}
		if (this.getPui().isGroundLootEmpty() && (closeLoot != null)) {
			this.getPui().setGroundLoot(closeLoot.getItems());

		} else if ((closeLoot == null) && !this.getPui().isGroundLootEmpty()) {
			this.getPui().setGroundLoot(new GameItem[8]);

		}


		// this.renderCollisionBoxes(g);

		g.setColor(Color.white);

		String fps = GamePanel.oldFrameCount + " FPS";
		g.drawString(fps, 0 + (6 * 32), 32);

		String tps = GamePanel.oldTickCount + " TPS";
		g.drawString(tps, 0 + (6 * 32), 64);

		this.pui.render(g);

		this.cam.render(g);
	}

	private void renderCollisionBoxes(Graphics2D g) {
		for (AABB node : this.aabbTree.getAllNodes()) {
			if (node.getHeight() == 20.0f) {
				System.out.println();
			}
			g.setColor(Color.BLUE);
			Vector2f pos = node.getPos().getWorldVar();
			pos.addX(node.getXOffset());
			pos.addY(node.getYOffset());

			g.drawLine((int) pos.x, (int) pos.y, (int) pos.x + (int) node.getWidth(),
					(int) pos.y);
			// Top Right-> Bottom Right
			g.drawLine((int) pos.x + (int) node.getWidth(), (int) pos.y, (int) pos.x + (int) node.getWidth(),
					(int) pos.y + (int) node.getHeight());
			// Bottom Left -> Bottom Right
			g.drawLine((int) pos.x, (int) pos.y + (int) node.getHeight(), (int) pos.x + (int) node.getWidth(),
					(int) pos.y + (int) node.getHeight());

			g.drawLine((int) pos.x, (int) pos.y, (int) pos.x, (int) pos.y + (int) node.getHeight());
		}
	}
}
