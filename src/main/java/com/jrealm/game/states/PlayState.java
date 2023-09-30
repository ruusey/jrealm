package com.jrealm.game.states;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.Chest;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.entity.material.Material;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.ui.DamageText;
import com.jrealm.game.ui.PlayerUI;
import com.jrealm.game.ui.TextEffect;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;
import com.jrealm.game.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PlayState extends GameState {
	public Realm realm;

	private List<DamageText> damageText;

	private Camera cam;
	private PlayerUI pui;
	public static Vector2f map;
	private List<Vector2f> shotDestQueue;
	public long lastShotTick = 0;
	public long lastAbilityTick = 0;

	public long playerId;

	public PlayState(GameStateManager gsm, Camera cam) {
		super(gsm);
		PlayState.map = new Vector2f();
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
		this.cam = cam;
		this.realm = new Realm(this.cam);


		this.shotDestQueue = new ArrayList<>();
		this.damageText = new ArrayList<>();
		this.loadClass(CharacterClass.WIZARD);
		Vector2f chestLoc = new Vector2f((0 + (GamePanel.width / 2)) - 32, (0 + (GamePanel.height / 2)) - 32);
		this.await(100);
		this.realm.addLootContainer(new Chest(chestLoc));
		this.await(100);
		this.realm.addLootContainer(new Chest(chestLoc.clone(128, 0)));
		this.await(100);
		this.realm.addLootContainer(new Chest(chestLoc.clone(256, 0)));

	}

	private void loadClass(CharacterClass cls) {
		if (this.realm.getPlayer(this.playerId) == null) {
			Player player = new Player(cls.classId, this.cam, GameDataManager.loadClassSprites(cls.classId),
					new Vector2f((0 + (GamePanel.width / 2)) - 32, (0 + (GamePanel.height / 2)) - 32), 32,
					this.realm.getTileManager());
			player.equipSlots(this.getStartingEquipment(player.getId()));
			player.setIsInvincible(true);

			this.cam.target(player);
			player.setIsInvincible(false);

			this.playerId = this.realm.addPlayer(player);
			this.pui = new PlayerUI(this);

			this.getPui().setEquipment(player.getInventory());
		} else {
			this.realm.removePlayer(this.playerId);
			Player player = new Player(cls.classId, this.cam, GameDataManager.loadClassSprites(cls.classId),
					new Vector2f((0 + (GamePanel.width / 2)) - 32, (0 + (GamePanel.height / 2)) - 32), 32,
					this.realm.getTileManager());
			player.equipSlots(this.getStartingEquipment(player.getId()));
			player.setIsInvincible(true);

			this.cam.target(player);
			player.setIsInvincible(false);

			this.playerId = this.realm.addPlayer(player);
			this.pui = new PlayerUI(this);

			this.getPui().setEquipment(player.getInventory());
		}

	}

	public Map<Integer, GameItem> getStartingEquipment(int characterClass) {
		Map<Integer, GameItem> result = new HashMap<>();

		switch(characterClass) {
		case 0:
			result.put(0, GameDataManager.GAME_ITEMS.get(91));
			result.put(2, GameDataManager.GAME_ITEMS.get(32));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(5, GameDataManager.GAME_ITEMS.get(2));
			break;
		case 1:
			result.put(0, GameDataManager.GAME_ITEMS.get(17));
			result.put(2, GameDataManager.GAME_ITEMS.get(32));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(5, GameDataManager.GAME_ITEMS.get(0));

			break;
		case 2:
			result.put(0, GameDataManager.GAME_ITEMS.get(121));
			result.put(1, GameDataManager.GAME_ITEMS.get(136));
			result.put(2, GameDataManager.GAME_ITEMS.get(106));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
			// priest
		case 3:
			result.put(0, GameDataManager.GAME_ITEMS.get(137));
			result.put(2, GameDataManager.GAME_ITEMS.get(106));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
			// warr
		case 4:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
			// knight
		case 5:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		case 6:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			// result.put(4, GameDataManager.GAME_ITEMS.get(75));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		}

		return result;
	}

	public long getPlayerId() {
		return this.playerId;
	}

	public Vector2f getPlayerPos() {
		return this.realm.getPlayers().get(this.playerId).getPos();
	}

	@Override
	public void update(double time) {
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
		Player player = this.realm.getPlayer(this.playerId);
		if (player == null)
			return;
		if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
			if (!this.gsm.isStateActive(GameStateManager.EDIT)) {

				if (player.getDeath()) {
					this.gsm.add(GameStateManager.GAMEOVER);
					this.gsm.pop(GameStateManager.PLAY);
				}
				Runnable monitorDamageText = () ->{
					List<DamageText> toRemove = new ArrayList<>();

					for (DamageText text : this.getDamageText()) {
						text.update();
						if (text.getRemove()) {
							toRemove.add(text);
						}
					}
					this.damageText.removeAll(toRemove);
				};
				Runnable playerShootDequeue = () -> {
					for (int i = 0; i < this.shotDestQueue.size(); i++) {
						Vector2f dest = this.shotDestQueue.remove(i);
						dest.addX(PlayState.map.x);
						dest.addY(PlayState.map.y);
						Vector2f source = this.getPlayerPos().clone(player.getSize() / 2, player.getSize() / 2);
						ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(player.getWeaponId());
						float angle = Bullet.getAngle(source, dest);
						for (Projectile p : group.getProjectiles()) {


							short offset = (short) (p.getSize() / (short) 2);
							short rolledDamage = player.getInventory()[0].getDamage().getInRange();
							rolledDamage += player.getComputedStats().getAtt();
							this.addProjectile(player.getWeaponId(), source.clone(-offset, -offset),
									angle + Float.parseFloat(p.getAngle()), p.getSize(), p.getMagnitude(), p.getRange(),
									rolledDamage, false, p.getFlags(), p.getAmplitude(), p.getFrequency());
						}
					}

				};

				Runnable processGameObjects = () -> {
					GameObject[] gameObject = this.realm.getGameObjectsInBounds(this.cam.getBounds());
					for (int i = 0; i < gameObject.length; i++) {
						if (gameObject[i] instanceof Enemy) {
							Enemy enemy = ((Enemy) gameObject[i]);
							enemy.update(this, time);

						}

						if (gameObject[i] instanceof Material) {
							Material mat = ((Material) gameObject[i]);
							if (!mat.isDiscovered()
									&& this.realm.getTileManager().getRenderViewPort().inside(
											(int) mat.getPos().getWorldVar().x,
											(int) mat.getPos().getWorldVar().y)) {
								mat.getImage().restoreDefault();
								mat.setDiscovered(true);
							}

						}

						if (gameObject[i] instanceof Bullet) {
							Bullet bullet = ((Bullet) gameObject[i]);
							if (bullet != null) {
								if (bullet.remove()) {
									this.realm.removeBullet(bullet);
								} else {
									bullet.update();

								}
							}
						}
					}
					this.processBulletHit();
				};

				Runnable updatePlayerAndUi = () -> {
					player.update(time);
					this.pui.update(time);
				};
				WorkerThread.submitAndRun(playerShootDequeue, processGameObjects, updatePlayerAndUi, monitorDamageText);
			}
			this.cam.target(player);
			this.cam.update();
		}
	}

	public synchronized void addProjectile(int projectileGroupId, Vector2f src, Vector2f dest, short size,
			float magnitude,
			float range, short damage, boolean isEnemy, List<Short> flags) {
		Player player = this.realm.getPlayer(this.playerId);
		if (player == null)
			return;
		ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		if (pg.getAngleOffset() != null) {
			bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
		}
		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}
		Bullet b = new Bullet(projectileGroupId, bulletImage, src, dest, size, magnitude, range, damage, isEnemy);
		b.setFlags(flags);

		this.realm.addBullet(b);
	}

	public synchronized void addProjectile(int projectileGroupId, Vector2f src, float angle, short size,
			float magnitude,
			float range, short damage, boolean isEnemy, List<Short> flags, short amplitude, short frequency) {
		Player player = this.realm.getPlayer(this.playerId);
		if (player == null)
			return;
		ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		if (pg.getAngleOffset() != null) {
			bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
		}
		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}
		Bullet b = new Bullet(projectileGroupId, bulletImage, src, angle, size, magnitude, range, damage, isEnemy);
		b.setAmplitude(amplitude);
		b.setFrequency(frequency);

		b.setFlags(flags);
		this.realm.addBullet(b);
	}

	public synchronized void processBulletHit() {
		List<Bullet> results = this.getBullets();
		GameObject[] gameObject = this.realm.getGameObjectsInBounds(this.cam.getBounds());
		Player player = this.realm.getPlayer(this.playerId);

		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Enemy) {
				Enemy enemy = ((Enemy) gameObject[i]);
				for (Bullet b : results) {
					this.processPlayerHit(b, player);
					this.proccessEnemyHit(b, enemy);
				}
			}

			else if (gameObject[i] instanceof Material) {
				Material mat = ((Material) gameObject[i]);
				for (Bullet b : results) {
					if (b.getBounds().collides(0, 0, mat.getBounds())) {
						this.realm.removeBullet(b);
					}
				}
			}
		}
	}

	private synchronized void processPlayerHit(Bullet b, Player p) {
		Player player = this.realm.getPlayer(this.playerId);
		if (player == null)
			return;
		if (b.getBounds().collides(0, 0, player.getBounds()) && b.isEnemy() && !b.isPlayerHit()) {
			Stats stats = player.getComputedStats();
			Vector2f sourcePos = p.getPos();
			int computedDamage = b.getDamage() - p.getComputedStats().getDef();
			DamageText hitText = DamageText.builder().damage(computedDamage + "")
					.effect(TextEffect.DAMAGE)
					.sourcePos(sourcePos).build();
			this.damageText.add(hitText);
			b.setPlayerHit(true);
			// player.getAnimation().getImage().setEffect(Sprite.effect.REDISH);
			short dmgToInflict = (short) (b.getDamage() - stats.getDef());
			player.setHealth(player.getHealth() - dmgToInflict, 0, false);
			this.realm.removeBullet(b);
		}
	}

	private synchronized void proccessEnemyHit(Bullet b, Enemy e) {
		if (b.getBounds().collides(0, 0, e.getBounds()) && !b.isEnemy()) {
			e.setHealth(e.getHealth() - b.getDamage(), 0, false);
			Vector2f sourcePos = e.getPos();
			DamageText hitText = DamageText.builder().damage("" + b.getDamage()).effect(TextEffect.DAMAGE)
					.sourcePos(sourcePos).build();
			this.damageText.add(hitText);
			if (b.hasFlag((short) 10) && !b.isEnemyHit()) {
				b.setEnemyHit(true);
			} else {
				this.realm.removeBullet(b);

			}

			if (e.getDeath()) {
				this.realm.removeEnemy(e);
				this.realm.addLootContainer(new LootContainer(
						GameDataManager.SPRITE_SHEETS.get("entity/rotmg-items-1.png").getSprite(6, 7, 8, 8),
						e.getPos()));
			}
		}
	}

	private List<Bullet> getBullets() {

		GameObject[] gameObject = this.realm.getGameObjectsInBounds(this.cam.getBounds());

		List<Bullet> results = new ArrayList<>();
		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Bullet) {
				results.add((Bullet) gameObject[i]);
			}
		}
		return results;
	}

	@Override
	public void input(MouseHandler mouse, KeyHandler key) {
		key.escape.tick();
		key.f1.tick();
		key.enter.tick();
		Player player = this.realm.getPlayer(this.playerId);

		if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
			if (this.cam.getTarget() == player) {
				player.input(mouse, key);
			}
			this.cam.input(mouse, key);

			if (key.f1.clicked) {
				if (this.gsm.isStateActive(GameStateManager.EDIT)) {
					this.gsm.pop(GameStateManager.EDIT);
					this.cam.target(player);
				} else {
					this.gsm.add(GameStateManager.EDIT);
					this.cam.target(null);
				}
			}

			this.pui.input(mouse, key);

			if (key.one.down) {
				this.loadClass(CharacterClass.ARCHER);
			}
			if (key.zero.down) {
				this.loadClass(CharacterClass.ROGUE);
			}
			if (key.two.down) {
				this.loadClass(CharacterClass.WIZARD);
			}
			if (key.three.down) {
				this.loadClass(CharacterClass.PRIEST);
			}
			if (key.four.down) {
				this.loadClass(CharacterClass.WARRIOR);
			}
			if (key.five.down) {
				this.loadClass(CharacterClass.KNIGHT);
			}
			if (key.six.down) {
				this.loadClass(CharacterClass.PALLADIN);
			}


			if (key.q.down && ((System.currentTimeMillis() - this.lastAbilityTick) > 1000)) {
				if (this.getPlayer().getMana() > 25) {
					this.useAbility(mouse);
					this.getPlayer().setMana(this.getPlayer().getMana() - 25);

				}
			}
		} else if (this.gsm.isStateActive(GameStateManager.EDIT)) {
			this.gsm.pop(GameStateManager.EDIT);
			this.cam.target(player);
		}

		if (key.escape.clicked) {
			if (this.gsm.isStateActive(GameStateManager.PAUSE)) {
				this.gsm.pop(GameStateManager.PAUSE);
			} else {
				this.gsm.add(GameStateManager.PAUSE);
			}
		}
		Stats stats = player.getComputedStats();
		boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > (400 - (stats.getDex() * 12)))
				&& !this.gsm.isStateActive(GameStateManager.EDIT);

		if ((mouse.getButton() == 1) && canShoot) {
			this.lastShotTick = System.currentTimeMillis();
			Vector2f dest = new Vector2f(mouse.getX(), mouse.getY());
			this.shotDestQueue.add(dest);
		}

	}

	private void useAbility(MouseHandler mouse) {
		GameItem abilityItem = this.getPlayer().getAbility();
		ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(15);
		Player player = this.realm.getPlayer(this.playerId);
		Vector2f dest = new Vector2f(mouse.getX(), mouse.getY());
		dest.addX(PlayState.map.x);
		dest.addY(PlayState.map.y);
		for (Projectile p : group.getProjectiles()) {

			short offset = (short) (p.getSize() / (short) 2);
			short rolledDamage = player.getInventory()[0].getDamage().getInRange();
			rolledDamage += player.getComputedStats().getAtt();
			this.addProjectile(15, dest.clone(-offset, -offset), Float.parseFloat(p.getAngle()), p.getSize(),
					p.getMagnitude(), p.getRange(), rolledDamage, false, p.getFlags(), p.getAmplitude(),
					p.getFrequency());
		}

		this.lastAbilityTick = System.currentTimeMillis();
	}

	public void getPlayerEquipmentItemByUid(String uid) {
		this.replaceLootContainerItemByUid(uid, null);
	}

	public GameItem getLootContainerItemByUid(String uid) {
		for (LootContainer lc : this.realm.getLoot().values()) {
			for (GameItem item : lc.getItems()) {
				if (item.getUid().equals(uid))
					return item;
			}
		}
		return null;
	}


	public void removeLootContainerItemByUid(String uid) {
		this.replaceLootContainerItemByUid(uid, null);
	}

	public void replaceLootContainerItemByUid(String uid, GameItem replacement) {
		for (LootContainer lc : this.realm.getLoot().values()) {
			int foundIdx = -1;
			for (int i = 0; i < lc.getItems().length; i++) {
				GameItem item = lc.getItems()[i];
				if (item == null) {
					continue;
				}
				if (item.getUid().equals(uid)) {
					foundIdx = i;
				}
			}
			if (foundIdx > -1) {
				lc.setItem(foundIdx, replacement);
			}
		}
	}

	public Chest getNearestChest() {
		for (LootContainer lc : this.realm.getLoot().values()) {
			if ((this.realm.getPlayer(this.playerId).getBounds().distance(lc.getPos()) < 31) && (lc instanceof Chest))
				return (Chest) lc;
		}
		return null;
	}

	public LootContainer getNearestLootContainer() {
		for (LootContainer lc : this.realm.getLoot().values()) {
			if ((this.realm.getPlayer(this.playerId).getBounds().distance(lc.getPos()) < 31))
				return lc;
		}
		return null;
	}

	@Override
	public void render(Graphics2D g) {
		this.realm.getTileManager().render(g);
		Player player = this.realm.getPlayer(this.playerId);
		if (player != null) {
			player.render(g);
		}
		this.pui.render(g);


		GameObject[] gameObject = this.realm.getGameObjectsInBounds(this.cam.getBounds());


		for (int i = 0; i < gameObject.length; i++) {
			GameObject toRender = gameObject[i];
			if (toRender != null) {
				toRender.render(g);
			}
		}

		this.renderCloseLoot(g);

		for (DamageText text : this.getDamageText()) {
			text.render(g);
		}

		// this.renderCollisionBoxes(g);

		g.setColor(Color.white);

		String fps = GamePanel.oldFrameCount + " FPS";
		g.drawString(fps, 0 + (6 * 32), 32);

		String tps = GamePanel.oldTickCount + " TPS";
		g.drawString(tps, 0 + (6 * 32), 64);

		this.cam.render(g);
	}

	public void renderCloseLoot(Graphics2D g) {
		List<LootContainer> toRemove = new ArrayList<>();
		Player player = this.realm.getPlayer(this.playerId);
		if (player == null)
			return;
		LootContainer closeLoot = null;
		for (LootContainer lc : this.realm.getLoot().values()) {
			if ((player.getBounds().distance(lc.getPos()) < 32)) {
				closeLoot = lc;
			}
			if (!lc.isEmpty()) {
				lc.render(g);

			}
			if (lc.isEmpty() || lc.isExpired()) {
				toRemove.add(lc);
			}
		}

		if ((this.getPui().isGroundLootEmpty() && (closeLoot != null))) {
			this.getPui().setGroundLoot(closeLoot.getItems(), g);

		} else if ((closeLoot == null) && !this.getPui().isGroundLootEmpty()) {
			this.getPui().setGroundLoot(new GameItem[8], g);

		}

		for (LootContainer tr : toRemove) {
			this.realm.removeLootContainer(tr);
		}
	}

	public Player getPlayer() {
		return this.realm.getPlayer(this.playerId);
	}

	private void renderCollisionBoxes(Graphics2D g) {

		GameObject[] gameObject = this.realm.getGameObjectsInBounds(this.cam.getBounds());
		AABB[] colBoxes = this.realm.getCollisionBoxesInBounds(this.cam.getBounds());
		for (GameObject go : gameObject) {
			AABB node = go.getBounds();

			g.setColor(Color.BLUE);
			Vector2f pos = node.getPos().getWorldVar();
			pos.addX(node.getXOffset());
			pos.addY(node.getYOffset());

			g.drawLine((int) pos.x, (int) pos.y, (int) pos.x + (int) node.getWidth(), (int) pos.y);
			// Top Right-> Bottom Right
			g.drawLine((int) pos.x + (int) node.getWidth(), (int) pos.y, (int) pos.x + (int) node.getWidth(),
					(int) pos.y + (int) node.getHeight());
			// Bottom Left -> Bottom Right
			g.drawLine((int) pos.x, (int) pos.y + (int) node.getHeight(), (int) pos.x + (int) node.getWidth(),
					(int) pos.y + (int) node.getHeight());

			g.drawLine((int) pos.x, (int) pos.y, (int) pos.x, (int) pos.y + (int) node.getHeight());
		}

		for (AABB node : colBoxes) {

			g.setColor(Color.BLUE);
			Vector2f pos = node.getPos().getWorldVar();
			pos.addX(node.getXOffset());
			pos.addY(node.getYOffset());

			g.drawLine((int) pos.x, (int) pos.y, (int) pos.x + (int) node.getWidth(), (int) pos.y);
			// Top Right-> Bottom Right
			g.drawLine((int) pos.x + (int) node.getWidth(), (int) pos.y, (int) pos.x + (int) node.getWidth(),
					(int) pos.y + (int) node.getHeight());
			// Bottom Left -> Bottom Right
			g.drawLine((int) pos.x, (int) pos.y + (int) node.getHeight(), (int) pos.x + (int) node.getWidth(),
					(int) pos.y + (int) node.getHeight());

			g.drawLine((int) pos.x, (int) pos.y, (int) pos.x, (int) pos.y + (int) node.getHeight());
		}
	}

	private void await(long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {

		}
	}
}
