package com.jrealm.game.states;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.Chest;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.tiles.TileMap;
import com.jrealm.game.tiles.blocks.Tile;
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

	private Queue<DamageText> damageText;
	private List<Vector2f> shotDestQueue;

	private Camera cam;
	private PlayerUI pui;
	public static Vector2f map;
	public long lastShotTick = 0;
	public long lastAbilityTick = 0;

	public long playerId = -1l;


	public PlayState(GameStateManager gsm, Camera cam) {
		super(gsm);
		PlayState.map = new Vector2f();
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
		this.cam = cam;
		this.realm = new Realm(this.cam);


		this.shotDestQueue = new ArrayList<>();
		this.damageText = new ConcurrentLinkedQueue<>();
		this.loadClass(CharacterClass.ARCHER, true);
		this.setupChests();

	}

	private void setupChests() {
		Vector2f chestLoc = new Vector2f((0 + (GamePanel.width / 2)) - 32, (0 + (GamePanel.height / 2)) - 96);
		this.await(100);
		this.realm.addLootContainer(new Chest(chestLoc));
		this.await(100);
		this.realm.addLootContainer(new Chest(chestLoc.clone(-128, 0)));
		this.await(100);
		this.realm.addLootContainer(new Chest(chestLoc.clone(-256, 0)));
	}

	private void loadClass(CharacterClass cls, boolean setEquipment) {

		Player player = new Player(cls.classId, this.cam, GameDataManager.loadClassSprites(cls),
				new Vector2f((0 + (GamePanel.width / 2)) - 32, (0 + (GamePanel.height / 2)) - 32), 30,
				this.realm.getTileManager());
		if (setEquipment || (this.playerId == -1l)) {
			player.equipSlots(this.getStartingEquipment(cls));
		} else {
			GameItem[] existing = this.getPlayer().getInventory();
			player.setInventory(existing);
		}
		this.cam.target(player);

		if ((this.playerId != -1) || (this.realm.getPlayer(this.playerId) != null)) {
			this.realm.removePlayer(this.playerId);
		}

		this.playerId = this.realm.addPlayer(player);
		this.pui = new PlayerUI(this);

		this.getPui().setEquipment(player.getInventory());

	}

	public Map<Integer, GameItem> getStartingEquipment(CharacterClass characterClass) {
		Map<Integer, GameItem> result = new HashMap<>();

		switch(characterClass) {
		case ROGUE:
			result.put(0, GameDataManager.GAME_ITEMS.get(49));
			result.put(1, GameDataManager.GAME_ITEMS.get(152));

			result.put(2, GameDataManager.GAME_ITEMS.get(32));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(5, GameDataManager.GAME_ITEMS.get(2));
			break;
		case ARCHER:
			result.put(0, GameDataManager.GAME_ITEMS.get(17));
			result.put(2, GameDataManager.GAME_ITEMS.get(32));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(5, GameDataManager.GAME_ITEMS.get(0));

			break;
		case WIZARD:
			result.put(0, GameDataManager.GAME_ITEMS.get(121));
			result.put(1, GameDataManager.GAME_ITEMS.get(136));
			result.put(2, GameDataManager.GAME_ITEMS.get(106));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
			// priest
		case PRIEST:
			result.put(0, GameDataManager.GAME_ITEMS.get(137));
			result.put(2, GameDataManager.GAME_ITEMS.get(106));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
			// warr
		case WARRIOR:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
			// knight
		case KNIGHT:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		case PALLADIN:
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

						if (gameObject[i] instanceof Bullet) {
							Bullet bullet = ((Bullet) gameObject[i]);
							if (bullet != null) {
								//								if (bullet.remove()) {
								//									toRemove.add(bullet.getBulletId());
								//								} else {
								bullet.update();
								// }
							}
						}
					}
					// this.realm.removeBullet(toRemove);
					this.processBulletHit();
				};
				// Rewrite this asap
				Runnable checkAbilityUsage = () -> {
					if (this.getPlayer() == null)
						return;
					GameItem playerAbility = this.getPlayer().getAbility();
					if ((playerAbility != null) && (playerAbility.getEffect() != null)) {
						this.getPlayer().removeExpiredEffects();
					}
				};
				Runnable updatePlayerAndUi = () -> {
					player.update(time);
					this.pui.update(time);
				};
				WorkerThread.submitAndRun(playerShootDequeue, processGameObjects, updatePlayerAndUi, monitorDamageText,
						checkAbilityUsage);
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
		}
		this.proccessTerrainHit();
	}

	private void proccessTerrainHit() {
		List<Bullet> toRemove = new ArrayList<>();
		TileMap currentMap = this.realm.getTileManager().getTm().get(1);
		Tile[] viewportTiles = null;
		if(currentMap == null)
			return;
		viewportTiles = currentMap.getBlocksInBounds(this.getCam().getBounds());
		for(Bullet b : this.getBullets()) {
			if (b.remove()) {
				toRemove.add(b);
				continue;
			}
			for (Tile tile : viewportTiles) {
				if (tile == null) {
					continue;
				}
				if (b.getBounds().intersect(new AABB(tile.getPos(), tile.getWidth(), tile.getHeight()))) {
					toRemove.add(b);
				}
			}
		}
		toRemove.forEach(bullet -> {
			this.realm.removeBullet(bullet);

		});
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
		if (this.realm.hasHitEnemy(b.getBulletId(), e.getEnemyId()))
			return;
		if (b.getBounds().collides(0, 0, e.getBounds()) && !b.isEnemy()) {
			this.realm.hitEnemy(b.getBulletId(), e.getEnemyId());

			e.setHealth(e.getHealth() - b.getDamage(), 0, false);
			Vector2f sourcePos = e.getPos();
			DamageText hitText = DamageText.builder().damage("" + b.getDamage()).effect(TextEffect.DAMAGE)
					.sourcePos(sourcePos).build();
			this.damageText.add(hitText);
			if (b.hasFlag((short) 10) && !b.isEnemyHit()) {
				b.setEnemyHit(true);
			} else if (b.remove()) {
				this.realm.removeBullet(b);
			}

			if (e.getDeath()) {
				this.realm.clearHitMap();
				this.realm.spawnRandomEnemy();
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

				this.realm.loadMap("tile/vault.xml");
				this.loadClass(CharacterClass.valueOf(this.getPlayer().getId()), false);
				this.setupChests();
			}

			this.pui.input(mouse, key);

			if (key.one.down) {
				this.loadClass(CharacterClass.ARCHER, true);
			}
			if (key.zero.down) {
				this.loadClass(CharacterClass.ROGUE, true);
			}
			if (key.two.down) {
				this.loadClass(CharacterClass.WIZARD, true);
			}
			if (key.three.down) {
				this.loadClass(CharacterClass.PRIEST, true);
			}
			if (key.four.down) {
				this.loadClass(CharacterClass.WARRIOR, true);
			}
			if (key.five.down) {
				this.loadClass(CharacterClass.KNIGHT, true);
			}
			if (key.six.down) {
				this.loadClass(CharacterClass.PALLADIN, true);
			}


			if (key.q.down && ((System.currentTimeMillis() - this.lastAbilityTick) > 1000)) {
				this.useAbility(mouse);
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
		if (abilityItem == null)
			return;
		if ((abilityItem != null) && (abilityItem.getDamage() != null)) {
			ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(abilityItem.getDamage().getProjectileGroupId());
			Player player = this.realm.getPlayer(this.playerId);
			Vector2f dest = new Vector2f(mouse.getX(), mouse.getY());
			dest.addX(PlayState.map.x);
			dest.addY(PlayState.map.y);
			for (Projectile p : group.getProjectiles()) {

				short offset = (short) (p.getSize() / (short) 2);
				short rolledDamage = player.getInventory()[0].getDamage().getInRange();
				rolledDamage += player.getComputedStats().getAtt();
				this.addProjectile(abilityItem.getDamage().getProjectileGroupId(), dest.clone(-offset, -offset),
						Float.parseFloat(p.getAngle()), p.getSize(),
						p.getMagnitude(), p.getRange(), rolledDamage, false, p.getFlags(), p.getAmplitude(),
						p.getFrequency());
			}

		} else if (abilityItem.getEffect() != null) {
			Effect effect = abilityItem.getEffect();
			if (this.getPlayer().getMana() >= effect.getMpCost()) {
				this.getPlayer().setMana(this.getPlayer().getMana() - effect.getMpCost());
				this.getPlayer().addEffect(effect.getEffectId(), effect.getDuration());
				this.lastAbilityTick = System.currentTimeMillis();
				// this.getPlayer().getSprite().getSpriteSheet().setEffect(Sprite.effect.DECAY);
			}
		}
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


		GameObject[] gameObject = this.realm.getGameObjectsInBounds(this.realm.getRealmCamera().getBounds());


		for (int i = 0; i < gameObject.length; i++) {
			GameObject toRender = gameObject[i];
			if (toRender != null) {
				toRender.render(g);
			}
		}
		this.pui.render(g);

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
