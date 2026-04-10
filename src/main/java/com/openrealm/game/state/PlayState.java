package com.openrealm.game.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.openrealm.account.dto.PlayerAccountDto;
import com.openrealm.game.OpenRealmGame;
import com.badlogic.gdx.graphics.Color;
import com.openrealm.game.contants.CharacterClass;
import com.openrealm.game.contants.GlobalConstants;
import com.openrealm.game.contants.StatusEffectType;
import com.openrealm.game.model.TileModel;
import com.openrealm.game.tile.Tile;
import com.openrealm.game.tile.TileData;
import com.openrealm.game.tile.TileMap;
import com.openrealm.game.data.GameDataManager;
import com.openrealm.game.entity.Bullet;
import com.openrealm.game.entity.Enemy;
import com.openrealm.game.entity.Entity;
import com.openrealm.game.entity.GameObject;
import com.openrealm.game.entity.Player;
import com.openrealm.game.entity.Portal;
import com.openrealm.game.entity.item.GameItem;
import com.openrealm.game.entity.item.LootContainer;
import com.openrealm.game.math.Rectangle;
import com.openrealm.game.math.Vector2f;
import com.openrealm.game.model.PortalModel;
import com.openrealm.game.ui.ActiveVisualEffect;
import com.openrealm.game.ui.EffectText;
import com.openrealm.game.ui.PlayerUI;
import com.openrealm.net.client.packet.CreateEffectPacket;
import com.openrealm.net.client.ClientGameLogic;
import com.openrealm.net.client.SocketClient;
import com.openrealm.net.messaging.CommandType;
import com.openrealm.net.messaging.LoginRequestMessage;
import com.openrealm.net.realm.Realm;
import com.openrealm.net.realm.RealmManagerClient;
import com.openrealm.net.server.packet.CommandPacket;
import com.openrealm.net.server.packet.MoveItemPacket;
import com.openrealm.net.server.packet.PlayerMovePacket;
import com.openrealm.net.server.packet.PlayerShootPacket;
import com.openrealm.net.server.packet.UseAbilityPacket;
import com.openrealm.net.server.packet.LoginAckPacket;
import com.openrealm.net.server.packet.UsePortalPacket;
import com.openrealm.util.Camera;
import com.openrealm.util.Cardinality;
import com.openrealm.util.KeyHandler;
import com.openrealm.util.MouseHandler;
import com.openrealm.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class PlayState extends GameState {
    private RealmManagerClient realmManager;
    private Queue<EffectText> damageText;
    private Queue<ActiveVisualEffect> activeEffects;
    private List<Vector2f> shotDestQueue;
    private PlayerAccountDto account;
    private Camera cam;
    private PlayerUI pui;
    public static Vector2f map;
    public long lastShotTick = 0;
    public long lastAbilityTick = 0;
    private long lastQuickUseTick = 0;
    private long lastPortalTick = 0;
    private static final long QUICK_USE_COOLDOWN_MS = 250;
    private static final long PORTAL_COOLDOWN_MS = 1000;
    public long playerId = -1l;
    
    private long lastSampleTime;
    private long frames;
    private long lastFrames;

    private Map<Cardinality, Boolean> lastDirectionMap;
    private boolean sentChat = false;
    private boolean debugMode = false;

    public PlayState(GameStateManager gsm, Camera cam) {
        super(gsm);
        PlayState.map = new Vector2f();
        Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
        this.cam = cam;
        this.realmManager = new RealmManagerClient(this, new Realm(false, 2));
        this.shotDestQueue = new ArrayList<>();
        this.damageText = new ConcurrentLinkedQueue<>();
        this.activeEffects = new ConcurrentLinkedQueue<>();
        try {
            this.doLogin();
        } catch (Exception e) {
            log.error("Failed to send initial LoginRequest. Reason: {}", e);
            System.exit(-1);
        }
        WorkerThread.submitAndForkRun(this.realmManager);
    }

    public void loadClass(Player player, CharacterClass cls, boolean setEquipment) {
        if (setEquipment || (this.playerId == -1l)) {
            player.equipSlots(GameDataManager.getStartingEquipment(cls));
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

        this.getPui().setEquipment(player.getInventory());
    }

    public long getPlayerId() {
        return this.playerId;
    }

    public Vector2f getPlayerPos() {
        return this.realmManager.getRealm().getPlayers().get(this.playerId).getPos();
    }
    
    public void doLogin() throws Exception {
        final LoginRequestMessage login = LoginRequestMessage.builder().characterUuid(SocketClient.CHARACTER_UUID)
                .email(SocketClient.PLAYER_EMAIL).password(SocketClient.PLAYER_PASSWORD).build();
        final CommandPacket loginPacket = CommandPacket.from(CommandType.LOGIN_REQUEST, login);
        this.realmManager.getClient().sendRemote(loginPacket);
    }

    @Override
    public void update(double time) {

        final Player player = this.realmManager.getRealm().getPlayer(this.realmManager.getCurrentPlayerId());

        if (player == null)
            return;
        float targetMapX = player.getPos().x - (OpenRealmGame.width / 2);
        float targetMapY = player.getPos().y - (OpenRealmGame.height / 2);
        // Snap camera directly to player - no lerp lag, keeps movement feeling crisp
        PlayState.map.x = targetMapX;
        PlayState.map.y = targetMapY;

        Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
        if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
            // Process all client-side updates inline — these are fast and
            // pool dispatch overhead exceeds the work itself
            final Realm clientRealm = this.realmManager.getRealm();
            final GameObject[] gameObject = clientRealm.getAllGameObjects();
            for (int i = 0; i < gameObject.length; i++) {
                if (gameObject[i] instanceof Enemy) {
                    ((Enemy) gameObject[i]).update(this.getRealmManager(), time);
                } else if (gameObject[i] instanceof Bullet) {
                    ((Bullet) gameObject[i]).update();
                } else if (gameObject[i] instanceof Player && gameObject[i].getId() != player.getId()) {
                    final Player playerOther = (Player) gameObject[i];
                    playerOther.update(time);
                    this.movePlayer(playerOther);
                    // Blend dead reckoning correction offset for other players
                    playerOther.blendCorrectionOffset();
                }
            }

            for (int i = 0; i < this.shotDestQueue.size(); i++) {
                final Vector2f dest = this.shotDestQueue.remove(i);
                final Vector2f source = this.getPlayer().getCenteredPosition();
                if (this.realmManager.getRealm().getTileManager().isCollisionTile(source)) {
                    continue;
                }
                try {
                    PlayerShootPacket packet = PlayerShootPacket.from(Realm.RANDOM.nextLong(), player, dest);
                    this.realmManager.getClient().sendRemote(packet);
                } catch (Exception e) {
                    PlayState.log.error("Failed to build player shoot packet. Reason: {}", e.getMessage());
                }
            }

            player.update(time);
            this.movePlayer(player);
            if (this.pui != null) {
                this.pui.update(time);
            }

            final List<EffectText> toRemove = new ArrayList<>();
            for (EffectText text : this.getDamageText()) {
                text.update();
                if (text.getRemove()) {
                    toRemove.add(text);
                }
            }
            this.damageText.removeAll(toRemove);

            final float deltaMs = (float) (time * 1000.0);
            final List<ActiveVisualEffect> effectsToRemove = new ArrayList<>();
            for (ActiveVisualEffect vfx : this.activeEffects) {
                vfx.update(deltaMs);
                if (vfx.getRemove()) {
                    effectsToRemove.add(vfx);
                }
            }
            this.activeEffects.removeAll(effectsToRemove);

            this.cam.target(player);
            this.cam.update();
            for (final LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
                lc.setContentsChanged(false);
            }
        }
        this.frames++;
        if((Instant.now().toEpochMilli()-lastSampleTime)>=1000) {
        	this.lastFrames = frames;
            this.lastSampleTime = Instant.now().toEpochMilli();
            this.frames=0;
        }
    }

    private void movePlayer(Player p) {
        if (p.hasEffect(StatusEffectType.PARALYZED)) {
            p.setDx(0);
            p.setDy(0);
        }
        if (!this.getRealmManager().getRealm().getTileManager().collisionTile(p, p.getDx(), 0)
                && !this.getRealmManager().getRealm().getTileManager().collidesXLimit(p, p.getDx()) 
                && !this.getRealmManager().getRealm().getTileManager().isVoidTile(p.getPos().clone(p.getSize()/2, p.getSize()/2), p.getDx(), 0)) {
            p.xCol = false;
            if (p.getDx() != 0.0f) {
                if (this.getRealmManager().getRealm().getTileManager().collidesSlowTile(p)) {
                    p.getPos().x += p.getDx() / 3.0f;
                } else {
                    p.getPos().x += p.getDx();
                }
            }
        } else {
            p.xCol = true;
        }

        if (!this.getRealmManager().getRealm().getTileManager().collisionTile(p, 0, p.getDy())
                && !this.getRealmManager().getRealm().getTileManager().collidesYLimit(p, p.getDy())
                && !this.getRealmManager().getRealm().getTileManager().isVoidTile(p.getPos().clone(p.getSize()/2, p.getSize()/2), 0, p.getDy())) {
            p.yCol = false;
            if (p.getDy() != 0.0f) {
                if (this.getRealmManager().getRealm().getTileManager().collidesSlowTile(p)) {
                    p.getPos().y += p.getDy() / 3.0f;
                } else {
                    p.getPos().y += p.getDy();
                }
            }
        } else {
            p.yCol = true;
        }
    }

    public synchronized void addProjectile(int projectileGroupId, int projectileId, Vector2f src, Vector2f dest, short size, float magnitude,
            float range, short damage, boolean isEnemy, List<Short> flags) {
        Player player = this.realmManager.getRealm().getPlayer(this.playerId);
        if (player == null)
            return;

        if (!isEnemy) {
            damage = (short) (damage + player.getStats().getAtt());
        }
        Bullet b = new Bullet(Realm.RANDOM.nextLong(), projectileId, src, dest, size, magnitude, range, damage, isEnemy);
        b.setFlags(flags);
        this.realmManager.getRealm().addBullet(b);
    }

    public synchronized long addProjectile(int projectileGroupId, int projectileId, Vector2f src, float angle, short size, float magnitude,
            float range, short damage, boolean isEnemy, List<Short> flags, short amplitude, short frequency) {
        Player player = this.realmManager.getRealm().getPlayer(this.playerId);
        if (player == null)
            return -1;

        if (!isEnemy) {
            damage = (short) (damage + player.getStats().getAtt());
        }
        Bullet b = new Bullet(Realm.RANDOM.nextLong(), projectileId, src, angle, size, magnitude, range, damage, isEnemy);
        b.setAmplitude(amplitude);
        b.setFrequency(frequency);
        b.setFlags(flags);
        return this.realmManager.getRealm().addBullet(b);
    }

    @SuppressWarnings("unused")
    private List<Bullet> getBullets() {
        final GameObject[] gameObject = this.realmManager.getRealm()
                .getGameObjectsInBounds(this.realmManager.getRealm().getTileManager().getRenderViewPort(this.getPlayer()));

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
        key.m.tick();
        key.plus.tick();
        key.minus.tick();

        Player player = this.realmManager.getRealm().getPlayer(this.playerId);
        if (player == null)
            return;

        this.cam.input(mouse, key);

        if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
            if ((this.cam.getTarget() == player) && !player.hasEffect(StatusEffectType.PARALYZED)) {
                final Map<Cardinality, Boolean> lastDirectionTempMap = new HashMap<>();
                player.input(mouse, key);
                Cardinality c = null;
                // RotMG speed formula: tiles/sec = 4 + 5.6 * (spd_stat / 75)
                // Convert to pixels/frame: tiles/sec * tile_size / fps
                float tilesPerSec = 4.0f + 5.6f * (player.getComputedStats().getSpd() / 75.0f);
                if (player.hasEffect(StatusEffectType.SPEEDY)) {
                    tilesPerSec *= 1.5f;
                }
                float spd = tilesPerSec * 32.0f / 60.0f;
                if (player.getIsUp()) {
                    player.setDy(-spd);
                    c = Cardinality.NORTH;
                    lastDirectionTempMap.put(Cardinality.NORTH, true);
                } else {
                    lastDirectionTempMap.put(Cardinality.NORTH, false);
                }

                if (player.getIsDown()) {
                    player.setDy(spd);
                    c = Cardinality.SOUTH;
                    lastDirectionTempMap.put(Cardinality.SOUTH, true);
                } else {
                    lastDirectionTempMap.put(Cardinality.SOUTH, false);
                }

                if (player.getIsLeft()) {
                    player.setDx(-spd);
                    c = Cardinality.WEST;
                    lastDirectionTempMap.put(Cardinality.WEST, true);
                } else {
                    lastDirectionTempMap.put(Cardinality.WEST, false);
                }

                if (player.getIsRight()) {
                    player.setDx(spd);
                    c = Cardinality.EAST;
                    lastDirectionTempMap.put(Cardinality.EAST, true);
                } else {
                    lastDirectionTempMap.put(Cardinality.EAST, false);
                }
                if (player.getIsUp() && player.getIsRight()) {
                    float diagSpd = (float) ((spd * Math.sqrt(2)) / 2.0f);
                    player.setDy(-diagSpd);
                    player.setDx(diagSpd);
                }

                if (player.getIsUp() && player.getIsLeft()) {
                    float diagSpd = (float) ((spd * Math.sqrt(2)) / 2.0f);
                    player.setDy(-diagSpd);
                    player.setDx(-diagSpd);
                }

                if (player.getIsDown() && player.getIsRight()) {
                    float diagSpd = (float) ((spd * Math.sqrt(2)) / 2.0f);
                    player.setDy(diagSpd);
                    player.setDx(diagSpd);
                }

                if (player.getIsDown() && player.getIsLeft()) {
                    float diagSpd = (float) ((spd * Math.sqrt(2)) / 2.0f);
                    player.setDy(diagSpd);
                    player.setDx(-diagSpd);
                }

                if (c == null) {
                    player.setDx(0);
                    player.setDy(0);
                    c = Cardinality.NONE;
                    lastDirectionTempMap.put(Cardinality.NONE, true);
                }

                if (this.lastDirectionMap == null) {
                    this.lastDirectionMap = lastDirectionTempMap;
                }

                if (!this.lastDirectionMap.equals(lastDirectionTempMap)) {
                    try {
                        // Build dirFlags bitmask: bit0=up, bit1=down, bit2=left, bit3=right
                        byte dirFlags = 0;
                        if (player.getIsUp())    dirFlags |= 0x01;
                        if (player.getIsDown())  dirFlags |= 0x02;
                        if (player.getIsLeft())  dirFlags |= 0x04;
                        if (player.getIsRight()) dirFlags |= 0x08;
                        player.setLastInputSeq(player.getLastInputSeq() + 1);
                        PlayerMovePacket packet = PlayerMovePacket.from(player, player.getLastInputSeq(), dirFlags);
                        this.realmManager.getClient().sendRemote(packet);
                    } catch (Exception e) {
                        PlayState.log.error("Failed to create player move packet. Reason: {}", e);
                    }
                    this.lastDirectionMap = lastDirectionTempMap;
                }
            }
            boolean canUsePortal = (System.currentTimeMillis() - this.lastPortalTick) > PORTAL_COOLDOWN_MS;
            if (key.f2.clicked && canUsePortal) {
                try {
                    Portal closestPortal = this.realmManager.getState().getClosestPortal(this.getPlayerPos(), 32);
                    if (closestPortal != null) {
                        PortalModel portalModel = GameDataManager.PORTALS.get((int) closestPortal.getPortalId());
                        UsePortalPacket usePortal = UsePortalPacket.from(closestPortal.getId(), this.realmManager.getRealm().getRealmId(),
                                this.getPlayerId());
                        this.realmManager.getClient().sendRemote(usePortal);
                        this.realmManager.getRealm().loadMap(portalModel.getMapId());
                        // Flag that we're transitioning realms - next ObjectMovePacket should snap position
                        this.realmManager.setAwaitingRealmTransition(true);
                        // Tell server we're ready for tiles after map rebuild
                        this.realmManager.getClient().sendRemote(LoginAckPacket.from(this.getPlayerId()));
                        this.lastPortalTick = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    PlayState.log.error("Failed to send test UsePortalPacket", e.getMessage());
                }

            }
            if (key.f1.clicked && canUsePortal) {
                try {
                    if (this.realmManager.getRealm().getMapId() != 1) {
                        UsePortalPacket usePortal = UsePortalPacket.toVault(this.realmManager.getRealm().getRealmId(), this.getPlayerId());
                        this.realmManager.getClient().sendRemote(usePortal);
                        this.realmManager.getRealm().loadMap(1);
                        this.realmManager.setAwaitingRealmTransition(true);
                        this.realmManager.getClient().sendRemote(LoginAckPacket.from(this.getPlayerId()));
                        this.lastPortalTick = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    PlayState.log.error("Failed to send test UsePortalPacket", e.getMessage());
                }

            }
            if (this.pui != null) {
                this.pui.input(mouse, key);
            }
            boolean canQuickUse = (System.currentTimeMillis() - this.lastQuickUseTick) > QUICK_USE_COOLDOWN_MS;
            if (canQuickUse) {
                boolean used = false;
                if (key.one.clicked) { this.handleQuickUseKey(4); used = true; }
                else if (key.two.clicked) { this.handleQuickUseKey(5); used = true; }
                else if (key.three.clicked) { this.handleQuickUseKey(6); used = true; }
                else if (key.four.clicked) { this.handleQuickUseKey(7); used = true; }
                else if (key.five.clicked) { this.handleQuickUseKey(8); used = true; }
                else if (key.six.clicked) { this.handleQuickUseKey(9); used = true; }
                else if (key.seven.clicked) { this.handleQuickUseKey(10); used = true; }
                else if (key.eight.clicked) { this.handleQuickUseKey(11); used = true; }
                if (used) this.lastQuickUseTick = System.currentTimeMillis();
            }

            if (this.pui != null) {
                if (key.m.clicked) this.pui.getMinimap().toggle();
                if (key.plus.down) this.pui.getMinimap().zoomIn();
                if (key.minus.down) this.pui.getMinimap().zoomOut();
            }
        }

        if (key.escape.clicked) {
            if (this.gsm.isStateActive(GameStateManager.PAUSE)) {
                this.gsm.pop(GameStateManager.PAUSE);
            } else {
                try {
					final PlayerAccountDto account = ClientGameLogic.DATA_SERVICE
					        .executeGet("/data/account/" + this.getAccount().getAccountUuid(), null, PlayerAccountDto.class);
					this.setAccount(account);
	                PauseState pause = new PauseState(this.gsm, this.getAccount());
	                this.gsm.add(GameStateManager.PAUSE, pause);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

            }
        }

        double dex = (int) ((6.5 * (this.getPlayer().getComputedStats().getDex() + 17.3)) / 75);
		if (player.hasEffect(StatusEffectType.SPEEDY)) {
			dex = dex * 1.5;
		}
        boolean canShoot = (System.currentTimeMillis() - this.lastShotTick) > (1000 / dex + 10);
        boolean canUseAbility = (System.currentTimeMillis() - this.lastAbilityTick) > 1000;
        boolean clickingWorld = mouse.isPressed(1) && (this.pui == null || !this.pui.isHoveringInventory(mouse.getX()));
        player.setAttacking(clickingWorld);
        // Pass mouse screen position for aim-based attack animation direction
        player.setAimX(mouse.getX());
        player.setAimY(mouse.getY());
        if (clickingWorld && canShoot) {
            this.lastShotTick = System.currentTimeMillis();
            Vector2f dest = new Vector2f(mouse.getX(), mouse.getY());
            dest.addX(PlayState.map.x);
            dest.addY(PlayState.map.y);
            this.shotDestQueue.add(dest);
        }
        if ((mouse.isPressed(3)) && canUseAbility && (this.pui == null || !this.pui.isHoveringInventory(mouse.getX()))) {
            try {
                Vector2f pos = new Vector2f(mouse.getX(), mouse.getY());
                pos.addX(PlayState.map.x);
                pos.addY(PlayState.map.y);
                UseAbilityPacket useAbility = UseAbilityPacket.from(this.getPlayer(), pos);
                this.realmManager.getClient().sendRemote(useAbility);
                this.lastAbilityTick = System.currentTimeMillis();

            } catch (Exception e) {
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
    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        Player player = this.realmManager.getRealm().getPlayer(this.playerId);
        if (player == null)
            return;
        this.realmManager.getRealm().getTileManager().render(player, batch, shapes);

        GameObject[] gameObject = this.realmManager.getRealm()
                .getGameObjectsInBounds(this.realmManager.getRealm().getTileManager().getRenderViewPort(player));

        // Collect visible entities by type for batched rendering
        final List<Entity> visibleEntities = new ArrayList<>();
        final List<Bullet> visibleBullets = new ArrayList<>();
        final List<Enemy> visibleEnemies = new ArrayList<>();

        for (Player p : this.realmManager.getRealm().getPlayers().values()) {
            visibleEntities.add(p);
            p.updateAnimation();
        }

        for (int i = 0; i < gameObject.length; i++) {
            if (gameObject[i] instanceof Enemy) {
                Enemy e = (Enemy) gameObject[i];
                visibleEntities.add(e);
                visibleEnemies.add(e);
            } else if (gameObject[i] instanceof Bullet) {
                visibleBullets.add((Bullet) gameObject[i]);
            }
            // Players already added above, skip to avoid double-render
        }

        // Update visual effect state for all entities before rendering
        for (int i = 0; i < visibleEntities.size(); i++) {
            visibleEntities.get(i).updateEffectState();
        }

        // Pass 1: All silhouette outlines (1 shader switch, 2 draws per entity)
        com.openrealm.game.graphics.ShaderManager.applyEffect(batch, com.openrealm.game.graphics.Sprite.EffectEnum.SILHOUETTE);
        for (int i = 0; i < visibleEntities.size(); i++) {
            visibleEntities.get(i).renderOutline(batch);
        }
        com.openrealm.game.graphics.ShaderManager.clearEffect(batch);

        // Pass 2: All entity bodies grouped by effect (minimize shader switches)
        com.openrealm.game.graphics.Sprite.EffectEnum currentEffect = null;
        for (int i = 0; i < visibleEntities.size(); i++) {
            Entity e = visibleEntities.get(i);
            com.openrealm.game.graphics.Sprite.EffectEnum effect = e.getCurrentEffect();
            if (effect != currentEffect) {
                com.openrealm.game.graphics.ShaderManager.applyEffect(batch, effect);
                currentEffect = effect;
            }
            e.renderBody(batch);
        }
        com.openrealm.game.graphics.ShaderManager.clearEffect(batch);

        // Pass 3: All bullets (no shader needed)
        for (int i = 0; i < visibleBullets.size(); i++) {
            visibleBullets.get(i).render(batch);
        }

        // Pass 4: Enemy health bars
        batch.end();
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < visibleEnemies.size(); i++) {
            Enemy enemy = visibleEnemies.get(i);
            float wx = enemy.getPos().getWorldVar().x;
            float wy = enemy.getPos().getWorldVar().y;
            int barWidth = enemy.getSize();
            int barHeight = 4;
            float barY = wy - 6;
            shapes.setColor(0.2f, 0.2f, 0.2f, 0.8f);
            shapes.rect(wx, barY, barWidth, barHeight);
            shapes.setColor(1f, 0f, 0f, 0.9f);
            shapes.rect(wx, barY, barWidth * enemy.getHealthpercent(), barHeight);
        }
        shapes.end();

        // Pass 5: Visual ability effects (rings, arcs, particles)
        this.renderVisualEffects(shapes);

        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        batch.begin();

        Collection<Portal> portals = this.realmManager.getRealm().getPortals().values();
        for (Portal portal : portals) {
            portal.render(batch);
        }

        if (this.pui == null)
            return;
        this.pui.render(batch, shapes, font);

        this.renderCloseLoot(batch);

        for (EffectText text : this.getDamageText()) {
            text.render(batch, font);
        }

        if (this.debugMode) {
            this.renderDebugTileOverlay(batch, shapes, font, player);
        }

        font.setColor(Color.WHITE);
        String fps = this.lastFrames + " FPS";
        font.draw(batch, fps, 6 * 32, 32);
    }

    private void renderDebugTileOverlay(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, Player player) {
        int mx = Gdx.input.getX();
        int my = Gdx.input.getY();

        // Convert screen coords to world coords
        float worldX = mx + PlayState.map.x;
        float worldY = my + PlayState.map.y;

        int tileSize = GlobalConstants.BASE_TILE_SIZE;
        int tileCol = (int) (worldX / tileSize);
        int tileRow = (int) (worldY / tileSize);

        TileMap baseLayer = this.realmManager.getRealm().getTileManager().getBaseLayer();
        TileMap collisionLayer = this.realmManager.getRealm().getTileManager().getCollisionLayer();

        if (tileCol < 0 || tileCol >= baseLayer.getWidth() || tileRow < 0 || tileRow >= baseLayer.getHeight()) {
            return;
        }

        // Get tiles at hovered position
        Tile baseTile = baseLayer.getBlocks()[tileRow][tileCol];
        Tile collTile = collisionLayer.getBlocks()[tileRow][tileCol];

        // Draw green outline around hovered tile in world space
        float drawX = (tileCol * tileSize) - PlayState.map.x;
        float drawY = (tileRow * tileSize) - PlayState.map.y;

        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Filled green tint
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 1f, 0f, 0.15f);
        shapes.rect(drawX, drawY, tileSize, tileSize);
        shapes.end();

        // Green border
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0f, 1f, 0f, 1f);
        shapes.rect(drawX, drawY, tileSize, tileSize);
        shapes.end();

        // Build tooltip text
        int tooltipX = mx + 16;
        int tooltipY = my + 16;
        int lineHeight = 16;
        int padding = 6;

        List<String> lines = new ArrayList<>();
        lines.add("Tile [" + tileCol + ", " + tileRow + "]");

        if (baseTile != null && !baseTile.isVoid()) {
            String baseName = "ID " + baseTile.getTileId();
            TileModel baseModel = GameDataManager.TILES.get((int) baseTile.getTileId());
            if (baseModel != null && baseModel.getName() != null) {
                baseName = baseModel.getName() + " (" + baseTile.getTileId() + ")";
            }
            lines.add("Base: " + baseName);
        } else {
            lines.add("Base: void");
        }

        if (collTile != null && !collTile.isVoid()) {
            String collName = "ID " + collTile.getTileId();
            TileModel collModel = GameDataManager.TILES.get((int) collTile.getTileId());
            if (collModel != null && collModel.getName() != null) {
                collName = collModel.getName() + " (" + collTile.getTileId() + ")";
            }
            lines.add("Collision: " + collName);
        }

        // Show tile data flags
        TileData data = null;
        if (collTile != null && collTile.getData() != null && collTile.getData().hasCollision()) {
            data = collTile.getData();
        } else if (baseTile != null && baseTile.getData() != null) {
            data = baseTile.getData();
        }

        if (data != null) {
            List<String> flags = new ArrayList<>();
            if (data.hasCollision()) flags.add("COLLISION");
            if (data.slows()) flags.add("SLOWS");
            if (data.damaging()) flags.add("DAMAGING");
            if (!flags.isEmpty()) {
                lines.add("Flags: " + String.join(", ", flags));
            }
        }

        int tooltipWidth = 0;
        for (String line : lines) {
            tooltipWidth = Math.max(tooltipWidth, line.length() * 7 + padding * 2);
        }
        int tooltipHeight = padding * 2 + lines.size() * lineHeight;

        // Clamp tooltip to screen
        if (tooltipX + tooltipWidth > OpenRealmGame.width) {
            tooltipX = mx - tooltipWidth - 8;
        }
        if (tooltipY + tooltipHeight > OpenRealmGame.height) {
            tooltipY = my - tooltipHeight - 8;
        }

        // Draw tooltip background
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.1f, 0.1f, 0.12f, 0.92f);
        shapes.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0f, 0.8f, 0f, 1f);
        shapes.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        // Draw tooltip text
        font.setColor(Color.GREEN);
        for (int i = 0; i < lines.size(); i++) {
            font.draw(batch, lines.get(i), tooltipX + padding, tooltipY + padding + lineHeight + (i * lineHeight));
        }
        font.setColor(Color.WHITE);
    }

    public void renderCloseLoot(SpriteBatch batch) {
        Player player = this.realmManager.getRealm().getPlayer(this.playerId);
        if (player == null)
            return;

        for (LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
            lc.render(batch);
        }

        // Skip normal loot container logic while trading - trade UI manages ground loot area
        if (this.getPui().isTrading()) {
            return;
        }

        final LootContainer closeLoot = this.getClosestLootContainer(player.getPos(), player.getSize() / 2);

        if ((closeLoot != null && this.getPui().isGroundLootEmpty()) || (closeLoot != null && closeLoot.getContentsChanged())) {
            this.getPui().setGroundLoot(closeLoot.getItems());

        } else if ((closeLoot == null) && !this.getPui().isGroundLootEmpty()) {
            this.getPui().setGroundLoot(new GameItem[8]);
        }

        if (closeLoot != null && !this.getPui().isGroundLootEmpty()) {
            final boolean contentsChanged = this.getPui().getNonEmptySlotCount() != closeLoot.getNonEmptySlotCount();
            if (contentsChanged) {
                this.getPui().setGroundLoot(closeLoot.getItems());
            }
        }
    }

    private void handleQuickUseKey(int slotIndex) {
        try {
            GameItem from = this.getPlayer().getInventory()[slotIndex];
            if (from == null) return;
            boolean consume = from.isConsumable();
            MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte) slotIndex, false, consume);
            this.realmManager.getClient().sendRemote(moveItem);
        } catch (Exception e) {
            PlayState.log.error("Failed to send move item packet: {}", "No Item in slot");
        }
    }

    public Player getPlayer() {
        return this.realmManager.getRealm().getPlayer(this.playerId);
    }

    /**
     * Render all active visual effects using ShapeRenderer.
     * Called between batch.end() and batch.begin() while blending is enabled.
     */
    private void renderVisualEffects(ShapeRenderer shapes) {
        if (this.activeEffects.isEmpty()) return;

        final float wx = Vector2f.worldX;
        final float wy = Vector2f.worldY;

        for (ActiveVisualEffect vfx : this.activeEffects) {
            final float t = vfx.getProgress();
            final short type = vfx.getEffectType();

            if (vfx.isAoe()) {
                renderAoeEffect(shapes, vfx, type, t, wx, wy);
            } else {
                renderLineEffect(shapes, vfx, t, wx, wy);
            }
        }
    }

    private void renderAoeEffect(ShapeRenderer shapes, ActiveVisualEffect vfx, short type, float t, float wx, float wy) {
        final float cx = vfx.getPosX() - wx;
        final float cy = vfx.getPosY() - wy;
        final float maxRadius = vfx.getRadius();
        // Ring expands fast then holds
        final float currentRadius = maxRadius * Math.min(t * 3.0f, 1.0f);
        // Stay fully visible for 70% of duration, then fade
        final float alpha = t < 0.7f ? 1.0f : 1.0f - (t - 0.7f) * 3.33f;

        float r, g, b;
        switch (type) {
        case CreateEffectPacket.EFFECT_HEAL_RADIUS:
            r = 0.1f; g = 1.0f; b = 0.2f;
            break;
        case CreateEffectPacket.EFFECT_VAMPIRISM:
            r = 0.9f; g = 0.0f; b = 1.0f;
            break;
        case CreateEffectPacket.EFFECT_STASIS_FIELD:
            r = 0.3f; g = 0.6f; b = 1.0f;
            break;
        case CreateEffectPacket.EFFECT_CURSE_RADIUS:
            r = 0.8f; g = 0.0f; b = 0.15f;
            break;
        case CreateEffectPacket.EFFECT_POISON_SPLASH:
            r = 0.2f; g = 0.8f; b = 0.2f;
            break;
        default:
            r = 1.0f; g = 1.0f; b = 1.0f;
            break;
        }

        // Filled translucent disc - much more visible
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(r, g, b, alpha * 0.35f);
        drawCircle(shapes, cx, cy, currentRadius, 48);
        shapes.end();

        // Thick bright outer ring (draw multiple concentric rings for thickness)
        shapes.begin(ShapeRenderer.ShapeType.Line);
        com.badlogic.gdx.Gdx.gl.glLineWidth(4f);
        shapes.setColor(r, g, b, alpha);
        drawCircleOutline(shapes, cx, cy, currentRadius, 64);
        shapes.setColor(r, g, b, alpha * 0.7f);
        drawCircleOutline(shapes, cx, cy, currentRadius * 0.97f, 64);
        drawCircleOutline(shapes, cx, cy, currentRadius * 1.03f, 64);
        shapes.end();

        // Second inner ring, pulsing
        float pulse = 0.7f + 0.3f * (float) Math.sin(t * Math.PI * 8);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        com.badlogic.gdx.Gdx.gl.glLineWidth(2f);
        shapes.setColor(r, g, b, alpha * 0.8f * pulse);
        drawCircleOutline(shapes, cx, cy, currentRadius * 0.6f, 48);
        shapes.end();

        // Large orbiting particles on the ring edge
        int particleCount = 16;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < particleCount; i++) {
            float angle = (float) (i * Math.PI * 2 / particleCount) + t * (float) Math.PI * 4;
            float px = cx + (float) Math.cos(angle) * currentRadius;
            float py = cy + (float) Math.sin(angle) * currentRadius;
            float pAlpha = alpha * (0.6f + 0.4f * (float) Math.sin(angle * 3 + t * Math.PI * 10));
            shapes.setColor(Math.min(r + 0.3f, 1f), Math.min(g + 0.3f, 1f), Math.min(b + 0.3f, 1f), pAlpha);
            shapes.rect(px - 3, py - 3, 6, 6);
        }

        // Inner scattered particles (moving outward or inward)
        int innerParticles = 12;
        for (int i = 0; i < innerParticles; i++) {
            float angle = (float) (i * Math.PI * 2 / innerParticles) - t * (float) Math.PI * 3;
            float dist;
            if (type == CreateEffectPacket.EFFECT_VAMPIRISM) {
                dist = currentRadius * (1.0f - t);
            } else {
                dist = currentRadius * 0.2f + currentRadius * 0.6f * t;
            }
            float px = cx + (float) Math.cos(angle) * dist;
            float py = cy + (float) Math.sin(angle) * dist;
            float pAlpha = alpha * 0.9f;
            shapes.setColor(Math.min(r + 0.2f, 1f), Math.min(g + 0.2f, 1f), Math.min(b + 0.2f, 1f), pAlpha);
            shapes.rect(px - 2.5f, py - 2.5f, 5, 5);
        }

        // Bright center flash at start
        if (t < 0.3f) {
            float flashAlpha = (0.3f - t) * 3.0f;
            shapes.setColor(1f, 1f, 1f, flashAlpha * 0.5f);
            drawCircle(shapes, cx, cy, currentRadius * 0.3f * (1.0f - t * 2), 24);
        }
        shapes.end();

        com.badlogic.gdx.Gdx.gl.glLineWidth(1f);
    }

    private void renderLineEffect(ShapeRenderer shapes, ActiveVisualEffect vfx, float t, float wx, float wy) {
        if (vfx.getEffectType() == CreateEffectPacket.EFFECT_POISON_SPLASH) {
            renderPoisonThrow(shapes, vfx, t, wx, wy);
            return;
        }
        final float x1 = vfx.getPosX() - wx;
        final float y1 = vfx.getPosY() - wy;
        final float x2 = vfx.getTargetPosX() - wx;
        final float y2 = vfx.getTargetPosY() - wy;
        // Stay fully visible for 80% of duration, then fade
        final float alpha = t < 0.8f ? 1.0f : 1.0f - (t - 0.8f) * 5.0f;

        final float dx = x2 - x1;
        final float dy = y2 - y1;
        final float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1f) return;

        int segments = Math.max(8, (int) (length / 10));
        float perpX = -dy / length;
        float perpY = dx / length;

        // Pre-compute jitter offsets for main bolt (reused by glow)
        float[] jitters = new float[segments + 1];
        jitters[0] = 0;
        jitters[segments] = 0;
        for (int i = 1; i < segments; i++) {
            float frac = (float) i / segments;
            jitters[i] = (float) (Math.sin(frac * Math.PI * 5 + t * Math.PI * 14) * 12.0f
                    + Math.cos(frac * Math.PI * 9 + t * Math.PI * 8) * 5.0f);
        }

        // Outer glow (thick, dim blue-purple)
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < segments; i++) {
            float frac0 = (float) i / segments;
            float frac1 = (float) (i + 1) / segments;
            float px0 = x1 + dx * frac0 + perpX * jitters[i];
            float py0 = y1 + dy * frac0 + perpY * jitters[i];
            float px1 = x1 + dx * frac1 + perpX * jitters[i + 1];
            float py1 = y1 + dy * frac1 + perpY * jitters[i + 1];
            // Draw thick quads along the bolt as glow
            float glowSize = 6f;
            shapes.setColor(0.3f, 0.4f, 1.0f, alpha * 0.3f);
            shapes.rectLine(px0, py0, px1, py1, glowSize);
        }
        shapes.end();

        // Main bright bolt - thick electric blue
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < segments; i++) {
            float frac0 = (float) i / segments;
            float frac1 = (float) (i + 1) / segments;
            float px0 = x1 + dx * frac0 + perpX * jitters[i];
            float py0 = y1 + dy * frac0 + perpY * jitters[i];
            float px1 = x1 + dx * frac1 + perpX * jitters[i + 1];
            float py1 = y1 + dy * frac1 + perpY * jitters[i + 1];
            shapes.setColor(0.4f, 0.7f, 1.0f, alpha);
            shapes.rectLine(px0, py0, px1, py1, 3f);
        }
        shapes.end();

        // Inner white-hot core
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < segments; i++) {
            float frac0 = (float) i / segments;
            float frac1 = (float) (i + 1) / segments;
            float px0 = x1 + dx * frac0 + perpX * jitters[i];
            float py0 = y1 + dy * frac0 + perpY * jitters[i];
            float px1 = x1 + dx * frac1 + perpX * jitters[i + 1];
            float py1 = y1 + dy * frac1 + perpY * jitters[i + 1];
            shapes.setColor(0.8f, 0.9f, 1.0f, alpha * 0.9f);
            shapes.rectLine(px0, py0, px1, py1, 1.5f);
        }
        shapes.end();

        // Secondary fork bolt (different jitter pattern)
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < segments; i++) {
            float frac0 = (float) i / segments;
            float frac1 = (float) (i + 1) / segments;
            float j0 = (float) (Math.cos(frac0 * Math.PI * 7 + t * Math.PI * 18) * 8.0f);
            float j1 = (float) (Math.cos(frac1 * Math.PI * 7 + t * Math.PI * 18) * 8.0f);
            if (i == 0) j0 = 0;
            if (i == segments - 1) j1 = 0;
            float px0 = x1 + dx * frac0 + perpX * j0;
            float py0 = y1 + dy * frac0 + perpY * j0;
            float px1 = x1 + dx * frac1 + perpX * j1;
            float py1 = y1 + dy * frac1 + perpY * j1;
            shapes.setColor(0.5f, 0.6f, 1.0f, alpha * 0.5f);
            shapes.rectLine(px0, py0, px1, py1, 2f);
        }
        shapes.end();

        // Bright glow particles along the bolt
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        int particleCount = Math.max(6, segments / 2);
        for (int i = 0; i < particleCount; i++) {
            float frac = (float) i / particleCount;
            int segIdx = Math.min((int) (frac * segments), segments - 1);
            float px = x1 + dx * frac + perpX * jitters[segIdx];
            float py = y1 + dy * frac + perpY * jitters[segIdx];
            float pAlpha = alpha * (0.5f + 0.5f * (float) Math.sin(frac * Math.PI));
            shapes.setColor(0.6f, 0.8f, 1.0f, pAlpha);
            shapes.rect(px - 3, py - 3, 6, 6);
        }

        // Bright impact circles at endpoints
        float endSize = 8f + 4f * (float) Math.sin(t * Math.PI * 10);
        shapes.setColor(0.5f, 0.7f, 1.0f, alpha * 0.8f);
        drawCircle(shapes, x1, y1, endSize, 12);
        drawCircle(shapes, x2, y2, endSize, 12);
        shapes.setColor(1.0f, 1.0f, 1.0f, alpha);
        drawCircle(shapes, x1, y1, endSize * 0.4f, 8);
        drawCircle(shapes, x2, y2, endSize * 0.4f, 8);
        shapes.end();
    }

    /** Render a chunky poison vial arc from player to target position (800ms flight) */
    private void renderPoisonThrow(ShapeRenderer shapes, ActiveVisualEffect vfx, float t, float wx, float wy) {
        final float x1 = vfx.getPosX() - wx;
        final float y1 = vfx.getPosY() - wy;
        final float x2 = vfx.getTargetPosX() - wx;
        final float y2 = vfx.getTargetPosY() - wy;

        final float dx = x2 - x1;
        final float dy = y2 - y1;
        final float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 1f) return;

        // Tall parabolic arc — 50% of throw distance as peak height
        int steps = 24;
        float arcHeight = dist * 0.5f;

        // Vial position along arc (t goes 0→1 over the 800ms duration)
        float vialFrac = Math.min(t, 1.0f);

        // Compute arc positions
        float[] arcX = new float[steps + 1];
        float[] arcY = new float[steps + 1];
        for (int i = 0; i <= steps; i++) {
            float f = (float) i / steps;
            arcX[i] = x1 + dx * f;
            arcY[i] = y1 + dy * f - 4.0f * arcHeight * f * (1.0f - f);
        }

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Thick green trail behind the vial
        for (int i = 0; i < steps; i++) {
            float f = (float) (i + 1) / steps;
            if (f > vialFrac) break;
            // Trail fades from thin at start to thick near vial
            float thickness = 3.0f + 5.0f * (f / Math.max(vialFrac, 0.01f));
            float trailAlpha = 0.15f + 0.4f * (f / Math.max(vialFrac, 0.01f));
            shapes.setColor(0.2f, 0.65f, 0.15f, trailAlpha);
            shapes.rectLine(arcX[i], arcY[i], arcX[i + 1], arcY[i + 1], thickness);
        }

        // Dripping particles along the trail
        for (int i = 0; i < 6; i++) {
            float pf = vialFrac * (0.3f + 0.7f * i / 6.0f);
            int idx = Math.min((int) (pf * steps), steps);
            float dripY = arcY[idx] + (t * 30.0f * (i + 1) / 6.0f);  // drip downward over time
            float dripAlpha = Math.max(0, 0.5f - t * 0.6f);
            if (dripAlpha > 0) {
                shapes.setColor(0.15f, 0.6f, 0.1f, dripAlpha);
                shapes.rect(arcX[idx] - 2, dripY - 1, 4, 3 + i);
            }
        }

        // Fat vial blob
        if (vialFrac < 1.0f) {
            int vialIdx = Math.min((int) (vialFrac * steps), steps);
            float vx = arcX[vialIdx];
            float vy = arcY[vialIdx];

            // Outer glow
            shapes.setColor(0.2f, 0.7f, 0.1f, 0.4f);
            drawCircle(shapes, vx, vy, 12f, 10);
            // Main vial body
            shapes.setColor(0.3f, 0.9f, 0.2f, 0.9f);
            drawCircle(shapes, vx, vy, 8f, 10);
            // Bright core / highlight
            shapes.setColor(0.7f, 1.0f, 0.5f, 0.8f);
            drawCircle(shapes, vx - 2, vy - 2, 3.5f, 8);
        }

        shapes.end();
    }

    /** Draw a filled circle using triangles (ShapeRenderer.Filled mode must be active) */
    private static void drawCircle(ShapeRenderer shapes, float cx, float cy, float radius, int segments) {
        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * Math.PI * 2 / segments);
            float a2 = (float) ((i + 1) * Math.PI * 2 / segments);
            shapes.triangle(cx, cy,
                    cx + (float) Math.cos(a1) * radius, cy + (float) Math.sin(a1) * radius,
                    cx + (float) Math.cos(a2) * radius, cy + (float) Math.sin(a2) * radius);
        }
    }

    /** Draw a circle outline (ShapeRenderer.Line mode must be active) */
    private static void drawCircleOutline(ShapeRenderer shapes, float cx, float cy, float radius, int segments) {
        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * Math.PI * 2 / segments);
            float a2 = (float) ((i + 1) * Math.PI * 2 / segments);
            shapes.line(
                    cx + (float) Math.cos(a1) * radius, cy + (float) Math.sin(a1) * radius,
                    cx + (float) Math.cos(a2) * radius, cy + (float) Math.sin(a2) * radius);
        }
    }

}
