package com.jrealm.game.states;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.PlayerLocation;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.Chest;
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
import com.jrealm.game.realm.RealmManagerClient;
import com.jrealm.game.ui.DamageText;
import com.jrealm.game.ui.PlayerUI;
import com.jrealm.game.ui.TextEffect;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.Cardinality;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;
import com.jrealm.game.util.Tuple;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.server.packet.MoveItemPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.net.server.packet.UseAbilityPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class PlayState extends GameState {
	private RealmManagerClient realmManager;
	private Queue<DamageText> damageText;
	private List<Vector2f> shotDestQueue;

	private Camera cam;
	private PlayerUI pui;
	public static Vector2f map;
	public long lastShotTick = 0;
	public long lastAbilityTick = 0;

	public long playerId = -1l;

	private PlayerLocation playerLocation = PlayerLocation.VAULT;
	private Tuple<Cardinality, Boolean> lastDirection;
	private boolean sentChat = false;

	public PlayState(GameStateManager gsm, Camera cam) {
		super(gsm);
		PlayState.map = new Vector2f();
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
		this.cam = cam;
		this.realmManager = new RealmManagerClient(this, new Realm(this.cam, false));

		this.shotDestQueue = new ArrayList<>();
		this.damageText = new ConcurrentLinkedQueue<>();
		WorkerThread.submitAndForkRun(this.realmManager);
	}


	public void loadClass(Player player, CharacterClass cls, boolean setEquipment) {
		if (setEquipment || (this.playerId == -1l)) {
			player.equipSlots(PlayState.getStartingEquipment(cls));
		} else {
			GameItem[] existing = this.getPlayer().getInventory();
			player.setInventory(existing);
		}
		this.cam.target(player);

		if ((this.playerId != -1) || (this.realmManager.getRealm().getPlayer(this.playerId) != null)) {
			this.realmManager.getRealm().removePlayer(this.playerId);
		}
		this.playerId = this.realmManager.getRealm().addPlayer(player);
		this.realmManager.setCurrentPlayerId(this.playerId);
		this.pui = new PlayerUI(this);
		this.getPlayer().getStats().setDef((short)50);

		this.getPui().setEquipment(player.getInventory());
	}

	private void loadClass(CharacterClass cls, boolean setEquipment) {
		Player player = new Player(Realm.RANDOM.nextLong(), this.cam, GameDataManager.loadClassSprites(cls),
				new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
						(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
				GlobalConstants.PLAYER_SIZE, cls);
		this.loadClass(player, cls, setEquipment);
	}

	public static Map<Integer, GameItem> getStartingEquipment(final CharacterClass characterClass) {
		Map<Integer, GameItem> result = new HashMap<>();

		switch (characterClass) {
		case ROGUE:
			result.put(0, GameDataManager.GAME_ITEMS.get(49));
			result.put(1, GameDataManager.GAME_ITEMS.get(152));
			result.put(2, GameDataManager.GAME_ITEMS.get(32));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(5, GameDataManager.GAME_ITEMS.get(2));
			break;
		case ARCHER:
			result.put(0, GameDataManager.GAME_ITEMS.get(17));
			result.put(1, GameDataManager.GAME_ITEMS.get(154));
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
		case PRIEST:
			result.put(0, GameDataManager.GAME_ITEMS.get(137));
			result.put(1, GameDataManager.GAME_ITEMS.get(157));
			result.put(2, GameDataManager.GAME_ITEMS.get(106));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		case WARRIOR:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(1, GameDataManager.GAME_ITEMS.get(156));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		case KNIGHT:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(1, GameDataManager.GAME_ITEMS.get(155));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		case PALLADIN:
			result.put(0, GameDataManager.GAME_ITEMS.get(75));
			result.put(1, GameDataManager.GAME_ITEMS.get(153));
			result.put(2, GameDataManager.GAME_ITEMS.get(60));
			result.put(3, GameDataManager.GAME_ITEMS.get(56));
			result.put(4, GameDataManager.GAME_ITEMS.get(2));
			break;
		default:
			break;
		}
		return result;
	}

	public long getPlayerId() {
		return this.playerId;
	}

	public Vector2f getPlayerPos() {
		return this.realmManager.getRealm().getPlayers().get(this.playerId).getPos();
	}

	@Override
	public void update(double time) {
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
		Player player = this.realmManager.getRealm().getPlayer(this.realmManager.getCurrentPlayerId());
		if (player == null)
			return;
		if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
			if (!this.gsm.isStateActive(GameStateManager.EDIT)) {

				if (player.getDeath()) {
					this.gsm.add(GameStateManager.GAMEOVER);
					this.gsm.pop(GameStateManager.PLAY);
				}

				Runnable monitorDamageText = () -> {
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

						Vector2f source = this.getPlayerPos().clone(player.getSize() / 2, player.getSize() / 2);
						ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(player.getWeaponId());
						float angle = Bullet.getAngle(source, dest);
						for (Projectile p : group.getProjectiles()) {
							short offset = (short) (p.getSize() / (short) 2);
							short rolledDamage = player.getInventory()[0].getDamage().getInRange();
							rolledDamage += player.getComputedStats().getAtt();

							long newProj = this.addProjectile(player.getWeaponId(), p.getProjectileId(), source.clone(-offset, -offset),
									angle + Float.parseFloat(p.getAngle()), p.getSize(), p.getMagnitude(), p.getRange(),
									rolledDamage, false, p.getFlags(), p.getAmplitude(), p.getFrequency());
							try {
								PlayerShootPacket packet = PlayerShootPacket.from(newProj, player, dest);
								this.realmManager.getClient().sendRemote(packet);
							}catch(Exception e) {
								PlayState.log.error("Failed to build player shoot packet. Reason: {}", e.getMessage());
							}
						}
					}
				};

				Runnable processGameObjects = () -> {
					this.processBulletHit();
				};
				// Rewrite this asap
				Runnable checkAbilityUsage = () -> {
					if (this.getPlayer() == null)
						return;
					for (GameObject e : this.realmManager.getRealm()
							.getGameObjectsInBounds(this.realmManager.getRealm().getTileManager().getRenderViewPort())) {
						if ((e instanceof Entity) || (e instanceof Enemy)) {
							Entity entCast = (Entity) e;
							entCast.removeExpiredEffects();
						}
					}
				};
				Runnable updatePlayerAndUi = () -> {
					this.getPlayer().update(time);
					this.pui.update(time);
				};
				WorkerThread.submitAndRun(playerShootDequeue, processGameObjects, updatePlayerAndUi, monitorDamageText,
						checkAbilityUsage);
			}

			this.cam.target(player);
			this.cam.update();
		}
	}

	public synchronized void addProjectile(int projectileGroupId, int projectileId, Vector2f src, Vector2f dest, short size,
			float magnitude, float range, short damage, boolean isEnemy, List<Short> flags) {
		Player player = this.realmManager.getRealm().getPlayer(this.playerId);
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
		Bullet b = new Bullet(Realm.RANDOM.nextLong(), projectileId, bulletImage, src, dest, size, magnitude, range, damage, isEnemy);
		b.setFlags(flags);

		this.realmManager.getRealm().addBullet(b);
	}

	public synchronized long addProjectile(int projectileGroupId, int projectileId, Vector2f src, float angle, short size,
			float magnitude, float range, short damage, boolean isEnemy, List<Short> flags, short amplitude,
			short frequency) {
		Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if (player == null)
			return -1;
		ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		if (pg.getAngleOffset() != null) {
			bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
		}
		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}
		Bullet b = new Bullet(Realm.RANDOM.nextLong(), projectileId, bulletImage, src, angle, size, magnitude, range, damage,
				isEnemy);
		b.setAmplitude(amplitude);
		b.setFrequency(frequency);

		b.setFlags(flags);
		return this.realmManager.getRealm().addBullet(b);
	}

	public synchronized void processBulletHit() {
		List<Bullet> results = this.getBullets();
		GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(this.cam.getBounds());

		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Enemy) {
				Enemy enemy = ((Enemy) gameObject[i]);
				for (Bullet b : results) {
					this.proccessEnemyHit(b, enemy);
				}
			}
		}
	}


	private synchronized void proccessEnemyHit(Bullet b, Enemy e) {
		if (this.realmManager.getRealm().hasHitEnemy(b.getId(), e.getId()))
			return;
		if (b.getBounds().collides(0, 0, e.getBounds()) && !b.isEnemy()) {
			if (!this.realmManager.getRealm().hasHitEnemy(b.getId(), e.getId())) {
				Vector2f sourcePos = e.getPos();
				DamageText hitText = DamageText.builder().damage("" + b.getDamage()).effect(TextEffect.DAMAGE)
						.sourcePos(sourcePos).build();
				this.damageText.add(hitText);
			}
		}
	}

	private List<Bullet> getBullets() {

		GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(this.realmManager.getRealm().getTileManager().getRenderViewPort());

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
		key.f2.tick();
		key.shift.tick();
		key.t.tick();
		key.enter.tick();
		Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if(player==null) return;
		if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {

			if (this.cam.getTarget() == player) {
				player.input(mouse, key);
				Cardinality c = null;
				boolean move = true;
				if (player.getIsUp()) {
					c = Cardinality.NORTH;
				}

				if (player.getIsDown()) {
					c = Cardinality.SOUTH;
				}

				if (player.getIsLeft()) {
					c = Cardinality.WEST;
				}
				if (player.getIsRight()) {
					c = Cardinality.EAST;
				}

				if(c==null) {
					c = Cardinality.NONE;
					move = false;
				}

				if(this.lastDirection==null) {
					this.lastDirection = new Tuple<Cardinality, Boolean>(c, move);
				}

				Tuple<Cardinality, Boolean> temp = new Tuple<Cardinality, Boolean>(c, move);
				try {
					if(!this.lastDirection.equals(temp)) {
						this.lastDirection = temp;
						PlayerMovePacket packet = PlayerMovePacket.from(player, temp.getX(), temp.getY());
						this.realmManager.getClient().sendRemote(packet);
					}
				}catch(Exception e) {

				}
			}
			this.cam.input(mouse, key);
			if (key.f2.clicked && !this.playerLocation.equals(PlayerLocation.VAULT)) {
				try {
					LoadMapPacket loadMap = LoadMapPacket.from(this.getPlayer(), "tile/vault.xml");
					this.realmManager.getClient().sendRemote(loadMap);
				}catch(Exception e) {
					PlayState.log.error("Failed to send load map packet for map {}. Reason: {}", "tile/vault.xml", e.getMessage());
				}
				this.playerLocation = PlayerLocation.VAULT;
				this.realmManager.getRealm().loadMap("tile/vault.xml", this.getPlayer());
				this.loadClass(this.getPlayer(), this.currentPlayerCharacterClass(), false);
			}
			if (key.f1.clicked && !this.playerLocation.equals(PlayerLocation.REALM)) {
				try {
					LoadMapPacket loadMap = LoadMapPacket.from(this.getPlayer(), "tile/nexus2.xml");
					this.realmManager.getClient().sendRemote(loadMap);
				}catch(Exception e) {
					PlayState.log.error("Failed to send load map packet for map {}. Reason: {}", "tile/nexus2.xml", e.getMessage());
				}

				this.playerLocation = PlayerLocation.REALM;
				this.realmManager.getRealm().loadMap("tile/nexus2.xml", this.getPlayer());
				this.loadClass(this.getPlayer(), this.currentPlayerCharacterClass(), false);

			}
			if(this.pui!=null) {
				this.pui.input(mouse, key);
			}
			if (!this.playerLocation.equals(PlayerLocation.REALM)) {
				if (key.one.down) {
					try {
						GameItem from = this.getPlayer().getInventory()[4];
						MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)4, false, false);
						this.realmManager.getClient().sendRemote(moveItem);

					} catch (Exception e) {
						PlayState.log.error("Failed to send test move item packet: {}", e);
					}
//					this.loadClass(CharacterClass.ARCHER, true);
				}
				if (key.zero.down) {
					this.loadClass(CharacterClass.ROGUE, true);
				}
				if (key.two.down) {
					try {
						GameItem from = this.getPlayer().getInventory()[5];

						MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)5, false, false);
						this.realmManager.getClient().sendRemote(moveItem);

					} catch (Exception e) {
						PlayState.log.error("Failed to send test move item packet: {}", e);
					}
//					this.loadClass(CharacterClass.WIZARD, true);
				}
				if (key.three.down) {
					try {
						GameItem from = this.getPlayer().getInventory()[6];
						MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)6, false, false);
						this.realmManager.getClient().sendRemote(moveItem);

					} catch (Exception e) {
						PlayState.log.error("Failed to send test move item packet: {}", e);
					}
//					this.loadClass(CharacterClass.PRIEST, true);
				}
				if (key.four.down) {
					try {
						GameItem from = this.getPlayer().getInventory()[7];
						MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)7, false, false);
						this.realmManager.getClient().sendRemote(moveItem);

					} catch (Exception e) {
						PlayState.log.error("Failed to send test move item packet: {}", e);
					}
					//this.loadClass(CharacterClass.WARRIOR, true);
				}
				if (key.five.down) {
					this.loadClass(CharacterClass.KNIGHT, true);
				}
				if (key.six.down) {
					this.loadClass(CharacterClass.PALLADIN, true);
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
		if (key.t.down && !this.sentChat) {
			try {
				this.sentChat = true;
				MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), (byte)0, (byte)4, false, false);
				this.realmManager.getClient().sendRemote(moveItem);

			} catch (Exception e) {
				PlayState.log.error("Failed to send test text packet: {}", e);
			}

		}
		Stats stats = player.getComputedStats();
		if (this.getPlayer().hasEffect(EffectType.SPEEDY)) {
			stats.setDex((short) (stats.getDex() * 2));
		}
		boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > (400 - (stats.getDex() * 15)))
				&& !this.gsm.isStateActive(GameStateManager.EDIT);
		boolean canUseAbility = (System.currentTimeMillis() - this.lastAbilityTick) > 1000;
		if ((mouse.isPressed(MouseEvent.BUTTON1)) && canShoot) {
			this.lastShotTick = System.currentTimeMillis();
			Vector2f dest = new Vector2f(mouse.getX(), mouse.getY());
			dest.addX(PlayState.map.x);
			dest.addY(PlayState.map.y);
			this.shotDestQueue.add(dest);
		}
		if ((mouse.isPressed(MouseEvent.BUTTON3)) && canUseAbility) {
			try {
				Vector2f pos = new Vector2f(mouse.getX(), mouse.getY());
				pos.addX(PlayState.map.x);
				pos.addY(PlayState.map.y);
				UseAbilityPacket useAbility = UseAbilityPacket.from(this.getPlayer(), pos);
				this.realmManager.getClient().sendRemote(useAbility);
				this.lastAbilityTick = System.currentTimeMillis();

			}catch(Exception e) {
				PlayState.log.error("Failed to send UseAbility packet. Reason: {}", e);
			}
		}
	}

	private CharacterClass currentPlayerCharacterClass() {
		return CharacterClass.valueOf(this.getPlayer().getClassId());
	}

	public GameItem getLootContainerItemByUid(String uid) {
		for (LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
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
		for (LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
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
		for (LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
			if ((this.realmManager.getRealm().getPlayer(this.playerId).getBounds().distance(lc.getPos()) <= (GlobalConstants.PLAYER_SIZE*2))
					&& (lc instanceof Chest) && !this.playerLocation.equals(PlayerLocation.REALM))
				return (Chest) lc;
		}
		return null;
	}

	public LootContainer getNearestLootContainer() {
		for (LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
			if ((this.realmManager.getRealm().getPlayer(this.playerId).getBounds().distance(lc.getPos()) <= (GlobalConstants.PLAYER_SIZE*2)))
				return lc;
		}
		return null;
	}

	@Override
	public void render(Graphics2D g) {
		Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if(player==null) return;
		this.realmManager.getRealm().getTileManager().render(g);

		for(Player p : this.realmManager.getRealm().getPlayers().values()) {
			p.render(g);
			p.updateAnimation();
		}


		// AABB test = new AABB(new Vector2f(this.getPlayerPos().x * 0.5f,
		// this.getPlayerPos().y * 0.5f),
		// (int) 32 * 8, (int) 32 * 8);

		GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(this.realmManager.getRealm().getTileManager().getRenderViewPort());

		for (int i = 0; i < gameObject.length; i++) {
			GameObject toRender = gameObject[i];
			if (toRender != null) {
				toRender.render(g);
			}
		}
		if(this.pui==null) return;
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
		Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if (player == null)
			return;
		AABB renderBounds = this.realmManager.getRealm().getTileManager().getRenderViewPort();
		LootContainer closeLoot = null;
		for (LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
			if ((lc instanceof Chest) && this.playerLocation.equals(PlayerLocation.REALM)) {
				continue;
			}
			if ((player.getBounds().distance(lc.getPos()) < GlobalConstants.PLAYER_SIZE)) {
				closeLoot = lc;
			}
			AABB lcBounds = new AABB(lc.getPos(), 32, 32);
			if (!lc.isEmpty() && lcBounds.intersect(renderBounds)) {
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

		} else if(closeLoot!=null && closeLoot.getContentsChanged()) {
			this.getPui().setGroundLoot(closeLoot.getItems(), g);
			closeLoot.setContentsChanged(false);
		}

		for (LootContainer tr : toRemove) {
			this.realmManager.getRealm().removeLootContainer(tr);
		}
	}

	public Player getPlayer() {
		return this.realmManager.getRealm().getPlayer(this.playerId);
	}

	@SuppressWarnings("unused")
	private void renderCollisionBoxes(Graphics2D g) {

		GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(this.cam.getBounds());
		AABB[] colBoxes = this.realmManager.getRealm().getCollisionBoxesInBounds(this.cam.getBounds());
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

}
