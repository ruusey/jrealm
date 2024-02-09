package com.jrealm.game.state;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerClient;
import com.jrealm.game.ui.EffectText;
import com.jrealm.game.ui.PlayerUI;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.Cardinality;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.server.packet.MoveItemPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.net.server.packet.UseAbilityPacket;
import com.jrealm.net.server.packet.UsePortalPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class PlayState extends GameState {
	private RealmManagerClient realmManager;
	private Queue<EffectText> damageText;
	private List<Vector2f> shotDestQueue;

	private Camera cam;
	private PlayerUI pui;
	public static Vector2f map;
	public long lastShotTick = 0;
	public long lastAbilityTick = 0;

	public long playerId = -1l;

	private Map<Cardinality, Boolean> lastDirectionMap;
	private boolean sentChat = false;

	public PlayState(GameStateManager gsm, Camera cam) {
		super(gsm);
		PlayState.map = new Vector2f();
		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
		this.cam = cam;
		this.realmManager = new RealmManagerClient(this, new Realm(false, 2));
		this.shotDestQueue = new ArrayList<>();
		this.damageText = new ConcurrentLinkedQueue<>();
		WorkerThread.submitAndForkRun(this.realmManager);
	}

	public void loadClass(Player player, CharacterClass cls, boolean setEquipment) {
		if (setEquipment || (this.playerId == -1l)) {
			player.equipSlots(GameDataManager.getStartingEquipment(cls));
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

		this.getPui().setEquipment(player.getInventory());
	}

	@SuppressWarnings("unused")
	private void loadClass(CharacterClass cls, boolean setEquipment) {
		Player player = new Player(Realm.RANDOM.nextLong(), this.cam, GameDataManager.loadClassSprites(cls),
				new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
						(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
				GlobalConstants.PLAYER_SIZE, cls);
		this.loadClass(player, cls, setEquipment);
	}

	public long getPlayerId() {
		return this.playerId;
	}

	public Vector2f getPlayerPos() {
		return this.realmManager.getRealm().getPlayers().get(this.playerId).getPos();
	}

	@Override
	public void update(double time) {

		Player player = this.realmManager.getRealm().getPlayer(this.realmManager.getCurrentPlayerId());

		if (player == null)
			return;
		PlayState.map.x = player.getPos().x - (GamePanel.width / 2);
		PlayState.map.y = player.getPos().y - (GamePanel.height / 2);

		Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
		if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
			if (!this.gsm.isStateActive(GameStateManager.EDIT)) {

				Runnable monitorDamageText = () -> {
					List<EffectText> toRemove = new ArrayList<>();
					for (EffectText text : this.getDamageText()) {
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

						Vector2f source = this.getPlayer().getCenteredPosition();
						if (this.realmManager.getRealm().getTileManager().isCollisionTile(source)) {
							PlayState.log.error("Failed to invoke player shoot. Projectile is out of bounds.");
							continue;
						}
						ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(player.getWeaponId());
						float angle = Bullet.getAngle(source, dest);

						try {
							PlayerShootPacket packet = PlayerShootPacket.from(Realm.RANDOM.nextLong(), player, dest);
							this.realmManager.getClient().sendRemote(packet);
						} catch (Exception e) {
							PlayState.log.error("Failed to build player shoot packet. Reason: {}", e.getMessage());
						}
						//						for (Projectile p : group.getProjectiles()) {
						//							short offset = (short) (p.getSize() / (short) 2);
						//							short rolledDamage = player.getInventory()[0].getDamage().getInRange();
						//							rolledDamage += player.getComputedStats().getAtt();
						//
						//							long newProj = this.addProjectile(player.getWeaponId(), p.getProjectileId(), source.clone(-offset, -offset),
						//									angle + Float.parseFloat(p.getAngle()), p.getSize(), p.getMagnitude(), p.getRange(),
						//									rolledDamage, false, p.getFlags(), p.getAmplitude(), p.getFrequency());
						//
						//						}
					}
				};
				// Testing out optimistic update of enemies/bullets
				final Runnable processGameObjects = () -> {
					final Realm clientRealm = this.realmManager.getRealm();
					final GameObject[] gameObject = clientRealm.getAllGameObjects();
					for (int i = 0; i < gameObject.length; i++) {
						if (gameObject[i] instanceof Enemy) {
							final Enemy enemy = ((Enemy) gameObject[i]);
							enemy.update(this.getRealmManager(), time);
						}

						if (gameObject[i] instanceof Bullet) {
							final Bullet bullet = ((Bullet) gameObject[i]);
							if (bullet != null) {
								bullet.update();
							}
						}
					}
				};

				Runnable updatePlayerAndUi = () -> {
					player.update(time);
					this.movePlayer(player);
					this.pui.update(time);
				};
				WorkerThread.submitAndRun(processGameObjects, playerShootDequeue, updatePlayerAndUi, monitorDamageText);
			}
			this.cam.target(player);
			this.cam.update();
		}
	}

	private void movePlayer(Player p) {
		if (!this.getRealmManager().getRealm().getTileManager().collisionTile(p, p.getDx(), 0)
				&& !this.getRealmManager().getRealm().getTileManager().collidesXLimit(p, p.getDx())) {
			p.xCol = false;
			if (p.getDx() != 0.0f) {
				p.getPos().x += p.getDx() / 3;
			}
		} else {
			p.xCol = true;
		}

		if (!this.getRealmManager().getRealm().getTileManager().collisionTile(p, 0, p.getDy())
				&& !this.getRealmManager().getRealm().getTileManager().collidesYLimit(p, p.getDy())) {
			p.yCol = false;
			if (p.getDy() != 0.0f) {
				p.getPos().y += p.getDy() / 3;
			}
		} else {
			p.yCol = true;
		}
		p.move();
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

	@SuppressWarnings("unused")
	private List<Bullet> getBullets() {
		final GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(
				this.realmManager.getRealm().getTileManager().getRenderViewPort(this.getPlayer()));

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

		Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if(player==null) return;

		this.cam.input(mouse, key);

		if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
			if (this.cam.getTarget() == player) {
				final Map<Cardinality, Boolean > lastDirectionTempMap = new HashMap<>();
				player.input(mouse, key);
				Cardinality c = null;
				float spd = (float) ((5.6 * (player.getComputedStats().getSpd() + 53.5)) / 75.0f);
				if (player.getIsUp()) {
					player.setDy(-spd);
					c = Cardinality.NORTH;
					lastDirectionTempMap.put(Cardinality.NORTH, true);
				}else {
					lastDirectionTempMap.put(Cardinality.NORTH, false);
				}

				if (player.getIsDown()) {
					player.setDy(spd);
					c = Cardinality.SOUTH;
					lastDirectionTempMap.put(Cardinality.SOUTH, true);
				}else {
					lastDirectionTempMap.put(Cardinality.SOUTH, false);
				}

				if (player.getIsLeft()) {
					player.setDx(-spd);
					c = Cardinality.WEST;
					lastDirectionTempMap.put(Cardinality.WEST, true);
				}else {
					lastDirectionTempMap.put(Cardinality.WEST, false);
				}

				if (player.getIsRight()) {
					player.setDx(spd);
					c = Cardinality.EAST;
					lastDirectionTempMap.put(Cardinality.EAST, true);
				}else {
					lastDirectionTempMap.put(Cardinality.EAST, false);
				}
				spd = (float) (spd * Math.sqrt(2)) / 2.0f;

				if (player.getIsUp() && player.getIsRight()) {
					player.setDy(-spd);
					player.setDx(spd);
				}

				if (player.getIsUp() && player.getIsLeft()) {
					player.setDy(-spd);
					player.setDx(-spd);
				}

				if (player.getIsDown() && player.getIsRight()) {
					player.setDy(spd);
					player.setDx(spd);
				}

				if (player.getIsDown() && player.getIsLeft()) {
					player.setDy(spd);
					player.setDx(-spd);
				}

				if(c==null) {
					player.setDx(0);
					player.setDy(0);
					c = Cardinality.NONE;
					lastDirectionTempMap.put(Cardinality.NONE, true);
				}

				if(this.lastDirectionMap == null) {
					this.lastDirectionMap = lastDirectionTempMap;
				}

				if(!this.lastDirectionMap.equals(lastDirectionTempMap)) {
					for(Map.Entry<Cardinality, Boolean> entry : lastDirectionTempMap.entrySet()) {
						if(this.lastDirectionMap.get(entry.getKey())!=entry.getValue()) {
							try {
								PlayerMovePacket packet = PlayerMovePacket.from(player, entry.getKey(), entry.getValue());
								this.realmManager.getClient().sendRemote(packet);
							}catch(Exception e) {
								PlayState.log.error("Failed to create player move packet. Reason: {}", e);
							}
						}
					}
					this.lastDirectionMap = lastDirectionTempMap;
				}
			}
			if (key.f2.clicked) {
				try {
					Portal closestPortal = this.realmManager.getState().getClosestPortal(this.getPlayerPos(), 32);
					if (closestPortal != null) {
						PortalModel portalModel = GameDataManager.PORTALS.get((int) closestPortal.getPortalId());
						UsePortalPacket usePortal = UsePortalPacket.from(closestPortal.getId(),
								this.realmManager.getRealm().getRealmId(), this.getPlayerId());
						this.realmManager.getClient().sendRemote(usePortal);
						this.realmManager.getRealm().loadMap(portalModel.getMapId());
					}
				} catch (Exception e) {
					PlayState.log.error("Failed to send test UsePortalPacket", e.getMessage());
				}

			}
			if (key.f1.clicked) {
				try {
					Portal closestPortal = this.realmManager.getState().getClosestPortal(this.getPlayerPos(), 32);
					if (closestPortal != null) {
						PortalModel portalModel = GameDataManager.PORTALS.get((int) closestPortal.getPortalId());
						UsePortalPacket usePortal = UsePortalPacket.from(-1, this.realmManager.getRealm().getRealmId(),
								this.getPlayerId());

						this.realmManager.getClient().sendRemote(usePortal);
						this.realmManager.getRealm().loadMap(portalModel.getMapId());
					}
				} catch (Exception e) {
					PlayState.log.error("Failed to send test UsePortalPacket", e.getMessage());
				}

			}
			if(this.pui!=null) {
				this.pui.input(mouse, key);
			}
			if (key.one.clicked) {
				try {
					GameItem from = this.getPlayer().getInventory()[4];
					MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)4, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send test move item packet: {}", e);
				}
			}
			if (key.two.clicked) {
				try {
					GameItem from = this.getPlayer().getInventory()[5];
					MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)5, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send test move item packet: {}", e);
				}
			}
			if (key.three.clicked) {
				try {
					GameItem from = this.getPlayer().getInventory()[6];
					MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)6, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send test move item packet: {}", e);
				}
			}
			if (key.four.clicked) {
				try {
					GameItem from = this.getPlayer().getInventory()[7];
					MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)7, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send test move item packet: {}", e);
				}
			}
			if (key.five.clicked) {
				try {
					GameItem from = this.getPlayer().getInventory()[8];
					MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)8, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send test move item packet: {}", e);
				}
			}
			if (key.six.clicked) {
				try {
					GameItem from = this.getPlayer().getInventory()[9];
					MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)9, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send test move item packet: {}", e);
				}
			}
			if (key.seven.clicked) {
				try {
					GameItem from = this.getPlayer().getInventory()[10];
					MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)10, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send test move item packet: {}", e);
				}
			}
			if (key.eight.clicked) {
				try {
					GameItem from = this.getPlayer().getInventory()[11];
					MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte)11, false, false);
					this.realmManager.getClient().sendRemote(moveItem);
				} catch (Exception e) {
					PlayState.log.error("Failed to send test move item packet: {}", e);
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
				//				MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), (byte)0, (byte)4, false, false);
				//				this.realmManager.getClient().sendRemote(moveItem);

			} catch (Exception e) {
				PlayState.log.error("Failed to send test text packet: {}", e);
			}

		}
		Stats stats = player.getComputedStats();
		if (this.getPlayer().hasEffect(EffectType.SPEEDY)) {
			stats.setDex((short) (stats.getDex() * 2));
		}
		int dex = (int) ((6.5 * (this.getPlayer().getComputedStats().getDex() + 17.3)) / 75);
		boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > (1000 / dex));
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

	@SuppressWarnings("unused")
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

	public LootContainer getClosestLootContainer(final Vector2f pos, final float limit) {
		float best = Float.MAX_VALUE;
		LootContainer bestLoot = null;
		for (final LootContainer lootContainer : this.realmManager.getRealm().getLoot().values()) {
			float dist = lootContainer.getPos().distanceTo(pos);
			if ((dist < best) && (dist <= limit)) {
				best = dist;
				bestLoot = lootContainer;
			}
		}
		return bestLoot;
	}

	public Portal getClosestPortal(final Vector2f pos, final float limit) {
		float best = Float.MAX_VALUE;
		Portal bestPortal = null;
		for (final Portal portal : this.realmManager.getRealm().getPortals().values()) {
			float dist = portal.getPos().distanceTo(pos);
			if ((dist < best) && (dist <= limit)) {
				best = dist;
				bestPortal = portal;
			}
		}
		return bestPortal;
	}

	@Override
	public void render(Graphics2D g) {
		Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		// TODO: Translate everything to screen coordinates to keep it centered
		if(player==null) return;
		this.realmManager.getRealm().getTileManager().render(player, g);

		for(Player p : this.realmManager.getRealm().getPlayers().values()) {
			p.render(g);
			p.updateAnimation();
		}

		GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(this.realmManager.getRealm().getTileManager().getRenderViewPort(player));

		for (int i = 0; i < gameObject.length; i++) {
			GameObject toRender = gameObject[i];
			if (toRender != null) {
				toRender.render(g);
			}
		}

		Collection<Portal> portals = this.realmManager.getRealm().getPortals().values();
		for (Portal portal : portals) {
			portal.render(g);
		}

		if(this.pui==null) return;
		this.pui.render(g);

		this.renderCloseLoot(g);

		for (EffectText text : this.getDamageText()) {
			text.render(g);
		}

		// this.renderCollisionBoxes(g);

		g.setColor(Color.white);

		String fps = GamePanel.oldFrameCount + " FPS";
		g.drawString(fps, 0 + (6 * 32), 32);

		String tps = GamePanel.oldTickCount + " TPS";
		g.drawString(tps, 0 + (6 * 32), 64);

		// this.cam.render(g);
	}

	public void renderCloseLoot(Graphics2D g) {
		Player player = this.realmManager.getRealm().getPlayer(this.playerId);
		if (player == null)
			return;
		final LootContainer closeLoot = this.getClosestLootContainer(player.getPos(), player.getSize() / 2);
		for (LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
			lc.render(g);
		}

		if ((this.getPui().isGroundLootEmpty() && (closeLoot != null)) || ((closeLoot != null) && closeLoot.getContentsChanged())) {
			this.getPui().setGroundLoot(closeLoot.getItems(), g);

		} else if ((closeLoot == null) && !this.getPui().isGroundLootEmpty()) {
			this.getPui().setGroundLoot(new GameItem[8], g);
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
