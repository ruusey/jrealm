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
import java.util.stream.Collectors;

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
	private Map<Cardinality, Boolean> lastDirection;
	private boolean sentStill = false;
	private boolean sentChat = false;

	public PlayState(final GameStateManager gsm, final Camera cam) {
		super(gsm);
		PlayState.map = new Vector2f();
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
		this.cam = cam;
		this.realmManager = new RealmManagerClient(this, new Realm(this.cam, false));
		this.shotDestQueue = new ArrayList<>();
		this.damageText = new ConcurrentLinkedQueue<>();
		this.lastDirection = new HashMap<>();
		WorkerThread.submitAndForkRun(this.realmManager);
	}


	public void loadClass(final Player player, final CharacterClass cls, final boolean setEquipment) {
		if (setEquipment || (this.playerId == -1l)) {
			player.equipSlots(PlayState.getStartingEquipment(cls));
		} else {
			final GameItem[] existing = this.getPlayer().getInventory();
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

	@SuppressWarnings("unused")
	private void loadClass(final CharacterClass cls, final boolean setEquipment) {
		final Player player = new Player(Realm.RANDOM.nextLong(), this.cam, GameDataManager.loadClassSprites(cls),
				new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
						(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
				GlobalConstants.PLAYER_SIZE, cls);
		this.loadClass(player, cls, setEquipment);
	}

	public static Map<Integer, GameItem> getStartingEquipment(final CharacterClass characterClass) {
		final Map<Integer, GameItem> result = new HashMap<>();

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
		final Player player = this.realmManager.getRealm().getPlayer(this.realmManager.getCurrentPlayerId());
		if (player == null)
			return;
		if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
			if (!this.gsm.isStateActive(GameStateManager.EDIT)) {

				if (player.getDeath()) {
					this.gsm.add(GameStateManager.GAMEOVER);
					this.gsm.pop(GameStateManager.PLAY);
				}

				final Runnable monitorDamageText = () -> {
					List<DamageText> toRemove = new ArrayList<>();
					for (DamageText text : this.getDamageText()) {
						text.update();
						if (text.getRemove()) {
							toRemove.add(text);
						}
					}
					this.damageText.removeAll(toRemove);
				};

				final Runnable playerShootDequeue = () -> {
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

				final Runnable processGameObjects = () -> {
					this.processBulletHit();
				};
				// Rewrite this asap
				final Runnable checkAbilityUsage = () -> {
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
				final Runnable updatePlayerAndUi = () -> {
					this.getPlayer().update(time);
					this.pui.update(time);
					this.cam.target(player);
					this.cam.update();
				};
				WorkerThread.submitAndRun(playerShootDequeue, processGameObjects, updatePlayerAndUi, monitorDamageText,
						checkAbilityUsage);
			}
		}
	}

	public synchronized void addProjectile(int projectileGroupId, int projectileId, Vector2f src, Vector2f dest, short size,
			float magnitude, float range, short damage, boolean isEnemy, List<Short> flags) {
		final Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if (player == null)
			return;
		final ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		final SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		final Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		if (pg.getAngleOffset() != null) {
			bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
		}
		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}
		final Bullet b = new Bullet(Realm.RANDOM.nextLong(), projectileId, bulletImage, src, dest, size, magnitude, range, damage, isEnemy);
		b.setFlags(flags);
		this.realmManager.getRealm().addBullet(b);
	}

	public synchronized long addProjectile(int projectileGroupId, int projectileId, Vector2f src, float angle, short size,
			float magnitude, float range, short damage, boolean isEnemy, List<Short> flags, short amplitude,
			short frequency) {
		final Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if (player == null)
			return -1;
		final ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		final SpriteSheet bulletSprite = GameDataManager.SPRITE_SHEETS.get(pg.getSpriteKey());
		final Sprite bulletImage = bulletSprite.getSprite(pg.getCol(), pg.getRow());
		if (pg.getAngleOffset() != null) {
			bulletImage.setAngleOffset(Float.parseFloat(pg.getAngleOffset()));
		}
		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}
		final Bullet b = new Bullet(Realm.RANDOM.nextLong(), projectileId, bulletImage, src, angle, size, magnitude, range, damage,
				isEnemy);
		b.setAmplitude(amplitude);
		b.setFrequency(frequency);

		b.setFlags(flags);
		return this.realmManager.getRealm().addBullet(b);
	}

	public synchronized void processBulletHit() {
		final List<Bullet> results = this.getBullets();
		final GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(this.cam.getBounds());

		for (int i = 0; i < gameObject.length; i++) {
			if (gameObject[i] instanceof Enemy) {
				final Enemy enemy = ((Enemy) gameObject[i]);
				for (Bullet b : results) {
					this.proccessEnemyHit(b, enemy);
				}
			}
		}
	}


	private synchronized void proccessEnemyHit(final Bullet b, final Enemy e) {
		if (this.realmManager.getRealm().hasHitEnemy(b.getId(), e.getId()))
			return;
		if (b.getBounds().collides(0, 0, e.getBounds()) && !b.isEnemy()) {
			if (!this.realmManager.getRealm().hasHitEnemy(b.getId(), e.getId())) {
				final Vector2f sourcePos = e.getPos();
				final DamageText hitText = DamageText.builder().damage("" + b.getDamage()).effect(TextEffect.DAMAGE)
						.sourcePos(sourcePos).build();
				this.damageText.add(hitText);
				this.realmManager.getRealm().hitEnemy(b.getId(), e.getId());
			}
		}
	}

	private List<Bullet> getBullets() {
		final GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(this.realmManager.getRealm().getTileManager().getRenderViewPort());
		final List<Bullet> results = new ArrayList<>();
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
		key.one.tick();
		key.two.tick();
		key.three.tick();
		key.four.tick();
		key.five.tick();
		key.six.tick();
		key.seven.tick();
		key.eight.tick();

		final Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if(player==null) return;
		if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
			if (this.cam.getTarget() == player) {
				player.input(mouse, key);
				
				if (player.getIsUp()) {
					this.sentStill = false;
					this.lastDirection.put(Cardinality.NORTH, true);
				}else {
					this.lastDirection.put(Cardinality.NORTH, false);
				}

				if (player.getIsDown()) {
					this.sentStill = false;
					this.lastDirection.put(Cardinality.SOUTH, true);
				}else {
					this.lastDirection.put(Cardinality.SOUTH, false);
				}

				if (player.getIsLeft()) {
					this.sentStill = false;
					this.lastDirection.put(Cardinality.WEST, true);
				}else {
					this.lastDirection.put(Cardinality.WEST, false);

				}
				if (player.getIsRight()) {
					this.sentStill = false;
					this.lastDirection.put(Cardinality.EAST, true);
				}else {
					this.lastDirection.put(Cardinality.EAST, false);
				}
				
				final List<Boolean> movements = this.lastDirection.values().stream().filter(movement->!movement).collect(Collectors.toList());

				if(movements.size()==4) {
					this.lastDirection.put(Cardinality.NONE, true);
				}else {
					this.lastDirection.put(Cardinality.NONE, false);
				}

				try {
					if(this.lastDirection.get(Cardinality.NONE) && !this.sentStill) {
						final PlayerMovePacket packet = PlayerMovePacket.from(player, Cardinality.NONE, true);
						this.realmManager.getClient().sendRemote(packet);
						this.sentStill = true;
					}else {
						for(Map.Entry<Cardinality, Boolean> movementDirection : this.lastDirection.entrySet()) {
							if(movementDirection.getKey()!=Cardinality.NONE && movementDirection.getValue()) {
								final PlayerMovePacket packet = PlayerMovePacket.from(player, movementDirection.getKey(), movementDirection.getValue());
								this.realmManager.getClient().sendRemote(packet);
							}
						}
					}
				}catch(Exception e) {
					log.error("Failed to send player movement to server. Reason: {}", e);
				}
			}
			this.cam.input(mouse, key);
			if (key.f2.clicked && !this.playerLocation.equals(PlayerLocation.VAULT)) {
				try {
					final LoadMapPacket loadMap = LoadMapPacket.from(this.getPlayer(), "tile/vault.xml");
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
					final LoadMapPacket loadMap = LoadMapPacket.from(this.getPlayer(), "tile/nexus2.xml");
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
			if (key.one.down) {
				try {
					final GameItem from = this.getPlayer().getInventory()[4];
					final MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)4, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send MoveItem packet. Reason: {}", e);
				}
			}
			if (key.two.down) {
				try {
					final GameItem from = this.getPlayer().getInventory()[5];
					final MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)5, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send MoveItem packet. Reason: {}", e);
				}
			}
			if (key.three.down) {
				try {
					final GameItem from = this.getPlayer().getInventory()[6];
					final MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)6, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send MoveItem packet. Reason: {}", e);
				}
			}
			if (key.four.down) {
				try {
					final GameItem from = this.getPlayer().getInventory()[7];
					final MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)7, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send MoveItem packet. Reason: {}", e);
				}
			}
			if (key.five.down) {
				try {
					final GameItem from = this.getPlayer().getInventory()[8];
					final MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)8, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send MoveItem packet. Reason: {}", e);
				}
			}
			if (key.six.down) {
				try {
					final GameItem from = this.getPlayer().getInventory()[9];
					final MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)9, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send MoveItem packet. Reason: {}", e);
				}
			}
			if (key.seven.down) {
				try {
					final GameItem from = this.getPlayer().getInventory()[9];
					final MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)9, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send MoveItem packet. Reason: {}", e);
				}
			}
			if (key.eight.down) {
				try {
					final GameItem from = this.getPlayer().getInventory()[9];
					final MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)9, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send MoveItem packet. Reason: {}", e);
				}
			}
		} 

		if (key.escape.clicked) {
			if (this.gsm.isStateActive(GameStateManager.PAUSE)) {
				this.gsm.pop(GameStateManager.PAUSE);
			} else {
				this.gsm.add(GameStateManager.PAUSE);
			}
		}
		// TODO: Remove when no longer needed for testing
		if (key.t.down && !this.sentChat) {
			try {
				this.sentChat = true;
//				MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), (byte)0, (byte)4, false, false);
//				this.realmManager.getClient().sendRemote(moveItem);
			} catch (Exception e) {
				PlayState.log.error("Failed to send test text packet: {}", e);
			}

		}
		final Stats stats = player.getComputedStats();
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
				final Vector2f pos = new Vector2f(mouse.getX(), mouse.getY());
				pos.addX(PlayState.map.x);
				pos.addY(PlayState.map.y);
				final UseAbilityPacket useAbility = UseAbilityPacket.from(this.getPlayer(), pos);
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

	public GameItem getLootContainerItemByUid(final String uid) {
		for (final LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
			for (final GameItem item : lc.getItems()) {
				if (item.getUid().equals(uid))
					return item;
			}
		}
		return null;
	}

	public void removeLootContainerItemByUid(final String uid) {
		this.replaceLootContainerItemByUid(uid, null);
	}

	public void replaceLootContainerItemByUid(final String uid, final GameItem replacement) {
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
		for (final LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
			if ((this.realmManager.getRealm().getPlayer(this.playerId).getBounds().distance(lc.getPos()) <= (GlobalConstants.PLAYER_SIZE*2))
					&& (lc instanceof Chest) && !this.playerLocation.equals(PlayerLocation.REALM))
				return (Chest) lc;
		}
		return null;
	}

	public LootContainer getNearestLootContainer() {
		for (final LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
			if ((this.realmManager.getRealm().getPlayer(this.playerId).getBounds().distance(lc.getPos()) <= (GlobalConstants.PLAYER_SIZE*2)))
				return lc;
		}
		return null;
	}

	@Override
	public void render(Graphics2D g) {
		final Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if(player==null) return;
		this.realmManager.getRealm().getTileManager().render(g);

		for(final Player p : this.realmManager.getRealm().getPlayers().values()) {
			p.render(g);
			p.updateAnimation();
		}

		// AABB test = new AABB(new Vector2f(this.getPlayerPos().x * 0.5f,
		// this.getPlayerPos().y * 0.5f),
		// (int) 32 * 8, (int) 32 * 8);

		final GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(this.realmManager.getRealm().getTileManager().getRenderViewPort());

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

		final String fps = GamePanel.oldFrameCount + " FPS";
		g.drawString(fps, 0 + (6 * 32), 32);

		final String tps = GamePanel.oldTickCount + " TPS";
		g.drawString(tps, 0 + (6 * 32), 64);

		this.cam.render(g);
	}

	public void renderCloseLoot(Graphics2D g) {
		final List<LootContainer> toRemove = new ArrayList<>();
		final Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if (player == null)
			return;
		final AABB renderBounds = this.realmManager.getRealm().getTileManager().getRenderViewPort();
		LootContainer closeLoot = null;
		for (final LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
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

		for (final LootContainer tr : toRemove) {
			this.realmManager.getRealm().removeLootContainer(tr);
		}
	}

	public Player getPlayer() {
		return this.realmManager.getRealm().getPlayer(this.playerId);
	}

	@SuppressWarnings("unused")
	private void renderCollisionBoxes(Graphics2D g) {

		final GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(this.cam.getBounds());
		final AABB[] colBoxes = this.realmManager.getRealm().getCollisionBoxesInBounds(this.cam.getBounds());
		for (final GameObject go : gameObject) {
			final AABB node = go.getBounds();

			g.setColor(Color.BLUE);
			final Vector2f pos = node.getPos().getWorldVar();
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

		for (final AABB node : colBoxes) {

			g.setColor(Color.BLUE);
			final Vector2f pos = node.getPos().getWorldVar();
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
