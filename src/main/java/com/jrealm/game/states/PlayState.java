package com.jrealm.game.states;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.jrealm.game.GamePanel;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.enemy.TinyMon;
import com.jrealm.game.entity.material.Material;
import com.jrealm.game.entity.material.MaterialManager;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.tiles.TileManager;
import com.jrealm.game.ui.PlayerUI;
import com.jrealm.game.util.AABBTree;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.GameObjectHeap;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

import lombok.Data;

@Data
public class PlayState extends GameState {

	public Player player;
	private GameObjectHeap gameObject;
	private AABBTree aabbTree;
	private TileManager tm;
	private MaterialManager mm;
	private Camera cam;
	private PlayerUI pui;
	public static Vector2f map;
	private double heaptime;

	private List<Vector2f> shotDestQueue;
	public long lastShotTick = 0;

	public PlayState(GameStateManager gsm, Camera cam) {
		super(gsm);

		PlayState.map = new Vector2f();
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
		this.cam = cam;

		this.tm = new TileManager("tile/tilemap.xml", cam);

		//		SpriteSheet tileset = new SpriteSheet("tile/overworldOP.png", 32, 32);
		//		SpriteSheet treeset = new SpriteSheet("material/trees.png", 64, 96);
		//
		//		this.mm = new MaterialManager(64, 150);
		//		this.mm.setMaterial(MaterialManager.TYPE.TREE, treeset.getSprite(1, 0), 64);
		//		this.mm.setMaterial(MaterialManager.TYPE.TREE, treeset.getSprite(3, 0), 64);
		//
		//		this.tm = new TileManager(tileset, 150, cam, this.mm);
		this.shotDestQueue = new ArrayList<>();
		this.gameObject = new GameObjectHeap();
		//gameObject.addAll(mm.list);
		this.aabbTree = new AABBTree();

		this.player = new Player(cam, new SpriteSheet("entity/rotmg-classes.png", 8, 8, 4),
				new Vector2f((0 + (GamePanel.width / 2)) - 32, (0 + (GamePanel.height / 2)) - 32), 64, this.tm);
		this.pui = new PlayerUI(this.player);
		this.aabbTree.insert(this.player);

		cam.target(this.player);

		SpriteSheet enemySheet = new SpriteSheet("entity/enemy/minimonsters.png", 16, 16, 4);


		Random r = new Random(System.currentTimeMillis());

		for (int i = 0; i < this.tm.getHeight(); i++) {
			for (int j = 0; j < this.tm.getWidth(); j++) {
				GameObject go = null;
				int doSpawn = r.nextInt(50);
				if (doSpawn > 46) {
					switch (r.nextInt(2)) {
					case 1:
						go = new TinyMon(cam,
								new SpriteSheet(enemySheet.getSprite(0, 1, 128, 32), "tiny boar", 16, 16, 0),
								new Vector2f(((GamePanel.width / 2) - 32) + 150,
										((0 + (GamePanel.height / 2)) - 32) + 150),
								32);
						go.setPos(new Vector2f(j * 64, i * 64));

						break;
					case 0:
						go = new TinyMon(cam,
								new SpriteSheet(enemySheet.getSprite(0, 0, 128, 32), "tiny monster", 16, 16, 0),
								new Vector2f(((GamePanel.width / 2) - 32) + 150,
										((0 + (GamePanel.height / 2)) - 32) + 150),
								32);
						go.setPos(new Vector2f(j * 64, i * 64));

						break;
					}
					this.getGameObjects().add(0.5f, go);
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


				//aabbTree.update(player);

				if(this.player.getDeath()) {
					this.gsm.add(GameStateManager.GAMEOVER);
					this.gsm.pop(GameStateManager.PLAY);
				}

				// System.out.println("Player shooting, adding " + this.shotDestinations.size()
				// + " bullets.");
				for (int i = 0; i < this.shotDestQueue.size(); i++) {
					Vector2f dest = this.shotDestQueue.remove(i);
					dest.addX(PlayState.map.x);
					dest.addY(PlayState.map.y);
					Vector2f source = this.getPlayerPos().clone(this.player.getSize() / 2, this.player.getSize() / 2);
					float angle = Bullet.getAngle(source, dest);
					this.addProjectile(0, source, angle + ((float) (Math.PI / 4)), (short) 8, 4.0f, 256.0f, (short) 30,
							false);
					this.addProjectile(1, source, angle - ((float) (Math.PI / 4)), (short) 8, 4.0f, 256.0f, (short) 30,
							false);

				}


				for(int i = 0; i < this.gameObject.size(); i++) {
					if(this.gameObject.get(i).go instanceof Enemy) {
						Enemy enemy = ((Enemy) this.gameObject.get(i).go);
						if(this.player.getHitBounds().collides(enemy.getBounds())) {
							this.player.setTargetEnemy(enemy);
						}

						if(enemy.getDeath()) {
							this.gameObject.remove(enemy);
						} else {
							enemy.update(this, time);
						}

						if(this.canBuildHeap(2500, 1000000, time)) {
							this.gameObject.get(i).value = enemy.getBounds().distance(this.player.getPos());
						}

						continue;
					}

					if(this.gameObject.get(i).go instanceof Material) {
						Material mat = ((Material) this.gameObject.get(i).go);
						if(this.player.getHitBounds().collides(mat.getBounds())) {
							this.player.setTargetGameObject(mat);
						}
					}

					if(this.gameObject.get(i).go instanceof Bullet) {
						Bullet bullet = ((Bullet) this.gameObject.get(i).go);
						if (bullet.remove()) {
							this.gameObject.remove(bullet);
							this.aabbTree.removeObject(bullet);

						} else {
							bullet.update();
						}
					}
				}

				if(this.canBuildHeap(3, 1000000000, time)) {
					this.heaptime = System.nanoTime();
					this.gameObject.buildHeap();
					//System.out.println(gameObject);
				}

				this.player.update(time);
				this.pui.update(time);
				this.processBulletHit();
			}
			this.cam.update();
		}
	}

	public void addProjectile(int index, Vector2f src, Vector2f dest, short size, float magnitude, float range, short damage,
			boolean isEnemy) {
		Bullet b = new Bullet(null,
				src,
				dest, size, magnitude, range, damage, isEnemy);
		this.getGameObjects().add(b.getBounds().distance(this.getPlayerPos()) + index, b);
		this.aabbTree.insert(b);
	}

	public void addProjectile(int index, Vector2f src, float angle, short size, float magnitude, float range,
			short damage,
			boolean isEnemy) {
		Bullet b = new Bullet(null, src, angle, size, magnitude, range, damage, isEnemy);
		this.getGameObjects().add(b.getBounds().distance(this.getPlayerPos()) + index, b);
		this.aabbTree.insert(b);
	}

	public void processBulletHit() {
		List<Bullet> results = new ArrayList<>();

		for (int i = 0; i < this.gameObject.size(); i++) {
			if (this.gameObject.get(i).go instanceof Bullet) {

				results.add((Bullet) this.gameObject.get(i).go);
			}
		}
		for (int i = 0; i < this.gameObject.size(); i++) {
			if (this.gameObject.get(i).go instanceof Enemy) {
				Enemy enemy = ((Enemy) this.gameObject.get(i).go);
				for (Bullet b : results) {
					if (b.getBounds().collides(0, 0, enemy.getHitBounds()) && !b.isEnemy()) {
						enemy.setHealth(enemy.getHealth() - b.getDamage(), 0, false);
						this.aabbTree.removeObject(b);
						this.gameObject.remove(b);
						if (enemy.getDeath()) {
							this.aabbTree.removeObject(enemy);
							this.gameObject.remove(enemy);
						}
					}
					if (b.getBounds().collides(0, 0, this.player.getBounds()) && b.isEnemy()) {
						this.player.getAnimation().getImage().setEffect(Sprite.effect.REDISH);

						this.player.setHealth(this.player.getHealth() - b.getDamage(), 0, false);
						this.aabbTree.removeObject(b);
						this.gameObject.remove(b);
					}
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
			PointerInfo a = MouseInfo.getPointerInfo();
			Point b = a.getLocation();
			int x = (int) b.getX();
			int y = (int) b.getY();
			Vector2f dest = new Vector2f(mouse.getX(), mouse.getY());
			Vector2f source = new Vector2f(x, y);
			this.shotDestQueue.add(dest);


		}
	}

	@Override
	public void render(Graphics2D g) {
		this.tm.render(g);

		this.player.render(g);
		for(int i = 0; i < this.gameObject.size(); i++) {
			if(this.cam.getBounds().collides(this.gameObject.get(i).getBounds())) {
				this.gameObject.get(i).go.render(g);
			} else if (this.gameObject.get(i).go instanceof Bullet) {
				Bullet bullet = ((Bullet) this.gameObject.get(i).go);
				this.gameObject.remove(bullet);
			}
		}

		for (AABB node : this.aabbTree.getAllNodes()) {
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

		g.setColor(Color.white);

		String fps = GamePanel.oldFrameCount + " FPS";
		g.drawString(fps, GamePanel.width - (6 * 32), 32);

		String tps = GamePanel.oldTickCount + " TPS";
		g.drawString(tps, GamePanel.width - (6 * 32), 64);

		this.pui.render(g);

		this.cam.render(g);
	}
}
