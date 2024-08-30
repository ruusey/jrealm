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

import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.ui.EffectText;
import com.jrealm.game.ui.PlayerUI;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.Cardinality;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerClient;
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
    private PlayerAccountDto account;
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

    @Override
    public void update(double time) {

        final Player player = this.realmManager.getRealm().getPlayer(this.realmManager.getCurrentPlayerId());

        if (player == null)
            return;
        PlayState.map.x = player.getPos().x - (GamePanel.width / 2);
        PlayState.map.y = player.getPos().y - (GamePanel.height / 2);

        Vector2f.setWorldVar(PlayState.map.x, PlayState.map.y);
        if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
            if (!this.gsm.isStateActive(GameStateManager.EDIT)) {

                final Runnable monitorDamageText = () -> {
                    final List<EffectText> toRemove = new ArrayList<>();
                    for (EffectText text : this.getDamageText()) {
                        text.update();
                        if (text.getRemove()) {
                            toRemove.add(text);
                        }
                    }
                    this.damageText.removeAll(toRemove);
                };

                final Runnable playerShootDequeue = () -> {
                    for (int i = 0; i < this.shotDestQueue.size(); i++) {
                        final Vector2f dest = this.shotDestQueue.remove(i);
                        final Vector2f source = this.getPlayer().getCenteredPosition();
                        if (this.realmManager.getRealm().getTileManager().isCollisionTile(source)) {
                            PlayState.log.error("Failed to invoke player shoot. Projectile is out of bounds.");
                            continue;
                        }
                        try {
                            PlayerShootPacket packet = PlayerShootPacket.from(Realm.RANDOM.nextLong(), player, dest);
                            this.realmManager.getClient().sendRemote(packet);
                        } catch (Exception e) {
                            PlayState.log.error("Failed to build player shoot packet. Reason: {}", e.getMessage());
                        }
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

                        if (gameObject[i] instanceof Player && gameObject[i].getId() != player.getId()) {
                            final Player playerOther = ((Player) gameObject[i]);
                            if (playerOther != null) {
                                playerOther.update(time);
                                this.movePlayer(playerOther);
                            }
                        }
                    }
                };

                final Runnable updatePlayerAndUi = () -> {
                    // player.removeExpiredEffects();
                    player.update(time);
                    this.movePlayer(player);
                    this.pui.update(time);
                };
                WorkerThread.submitAndRun(processGameObjects, playerShootDequeue, updatePlayerAndUi, monitorDamageText);
            }
            this.cam.target(player);
            this.cam.update();
            for (final LootContainer lc : this.realmManager.getRealm().getLoot().values()) {
                lc.setContentsChanged(false);
            }
        }
    }

    private void movePlayer(Player p) {
        if (p.hasEffect(EffectType.PARALYZED)) {
            p.setDx(0);
            p.setDy(0);
        }
        if (!this.getRealmManager().getRealm().getTileManager().collisionTile(p, p.getDx(), 0)
                && !this.getRealmManager().getRealm().getTileManager().collidesXLimit(p, p.getDx())) {
            p.xCol = false;
            if (p.getDx() != 0.0f) {
                // p.applyMovementLerp(p.getDx(), 0, 0.65f);
                p.getPos().x += (p.getDx() * 0.45f);
            }
        } else {
            p.xCol = true;
        }

        if (!this.getRealmManager().getRealm().getTileManager().collisionTile(p, 0, p.getDy())
                && !this.getRealmManager().getRealm().getTileManager().collidesYLimit(p, p.getDy())) {
            p.yCol = false;
            if (p.getDy() != 0.0f) {
                // p.applyMovementLerp(0, p.getDy(), 0.65f);
                p.getPos().y += (p.getDy() * 0.45f);
            }
        } else {
            p.yCol = true;
        }
        p.move();
    }

    public synchronized void addProjectile(int projectileGroupId, int projectileId, Vector2f src, Vector2f dest,
            short size, float magnitude, float range, short damage, boolean isEnemy, List<Short> flags) {
        Player player = this.realmManager.getRealm().getPlayer(this.playerId);
        if (player == null)
            return;

        if (!isEnemy) {
            damage = (short) (damage + player.getStats().getAtt());
        }
        Bullet b = new Bullet(Realm.RANDOM.nextLong(), projectileId, src, dest, size, magnitude, range, damage,
                isEnemy);
        b.setFlags(flags);
        this.realmManager.getRealm().addBullet(b);
    }

    public synchronized long addProjectile(int projectileGroupId, int projectileId, Vector2f src, float angle,
            short size, float magnitude, float range, short damage, boolean isEnemy, List<Short> flags, short amplitude,
            short frequency) {
        Player player = this.realmManager.getRealm().getPlayer(this.playerId);
        if (player == null)
            return -1;

        if (!isEnemy) {
            damage = (short) (damage + player.getStats().getAtt());
        }
        Bullet b = new Bullet(Realm.RANDOM.nextLong(), projectileId, src, angle, size, magnitude, range, damage,
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
        if (player == null)
            return;

        this.cam.input(mouse, key);

        if (!this.gsm.isStateActive(GameStateManager.PAUSE)) {
            if ((this.cam.getTarget() == player) && !player.hasEffect(EffectType.PARALYZED)) {
                final Map<Cardinality, Boolean> lastDirectionTempMap = new HashMap<>();
                player.input(mouse, key);
                Cardinality c = null;
                float spd = (float) ((5.6 * (player.getComputedStats().getSpd() + 53.5)) / 75.0f);
                spd = spd / 1.5f;
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
                    for (Map.Entry<Cardinality, Boolean> entry : lastDirectionTempMap.entrySet()) {
                        if (this.lastDirectionMap.get(entry.getKey()) != entry.getValue()) {
                            try {
                                PlayerMovePacket packet = PlayerMovePacket.from(player, entry.getKey(),
                                        entry.getValue());
                                this.realmManager.getClient().sendRemote(packet);
                            } catch (Exception e) {
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
                    if (this.realmManager.getRealm().getMapId() != 1) {
                        UsePortalPacket usePortal = UsePortalPacket.toVault(this.realmManager.getRealm().getRealmId(),
                                this.getPlayerId());
                        this.realmManager.getClient().sendRemote(usePortal);
                        this.realmManager.getRealm().loadMap(1);
                    }
                } catch (Exception e) {
                    PlayState.log.error("Failed to send test UsePortalPacket", e.getMessage());
                }

            }
            if (this.pui != null) {
                this.pui.input(mouse, key);
            }
            if (key.one.clicked) {
                try {
                    GameItem from = this.getPlayer().getInventory()[4];

                    MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(),
                            (byte) 4, false, false);
                    if (from.isConsumable()) {
                        moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte) 4, false,
                                true);
                    }
                    this.realmManager.getClient().sendRemote(moveItem);
                } catch (Exception e) {
                    PlayState.log.error("Failed to send move item packet: {}", "No Item in slot");
                }
            }
            if (key.two.clicked) {
                try {
                    GameItem from = this.getPlayer().getInventory()[5];
                    MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(),
                            (byte) 5, false, false);
                    if (from.isConsumable()) {
                        moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte) 5, false,
                                true);
                    }
                    this.realmManager.getClient().sendRemote(moveItem);
                } catch (Exception e) {
                    PlayState.log.error("Failed to send move item packet: {}", "No Item in slot");
                }
            }
            if (key.three.clicked) {
                try {
                    GameItem from = this.getPlayer().getInventory()[6];
                    MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(),
                            (byte) 6, false, false);
                    if (from.isConsumable()) {
                        moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte) 6, false,
                                true);
                    }
                    this.realmManager.getClient().sendRemote(moveItem);
                } catch (Exception e) {
                    PlayState.log.error("Failed to send move item packet: {}", "No Item in slot");
                }
            }
            if (key.four.clicked) {
                try {
                    GameItem from = this.getPlayer().getInventory()[7];
                    MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(),
                            (byte) 7, false, false);
                    if (from.isConsumable()) {
                        moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte) 7, false,
                                true);
                    }
                    this.realmManager.getClient().sendRemote(moveItem);
                } catch (Exception e) {
                    PlayState.log.error("Failed to send move item packet: {}", "No Item in slot");
                }
            }
            if (key.five.clicked) {
                try {
                    GameItem from = this.getPlayer().getInventory()[8];
                    MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(),
                            (byte) 8, false, false);
                    if (from.isConsumable()) {
                        moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte) 8, false,
                                true);
                    }
                    this.realmManager.getClient().sendRemote(moveItem);
                } catch (Exception e) {
                    PlayState.log.error("Failed to send move item packet: {}", "No Item in slot");
                }
            }
            if (key.six.clicked) {
                try {
                    GameItem from = this.getPlayer().getInventory()[9];
                    MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(),
                            (byte) 9, false, false);
                    if (from.isConsumable()) {
                        moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte) 9, false,
                                true);
                    }
                    this.realmManager.getClient().sendRemote(moveItem);
                } catch (Exception e) {
                    PlayState.log.error("Failed to send move item packet: {}", "No Item in slot");
                }
            }
            if (key.seven.clicked) {
                try {
                    GameItem from = this.getPlayer().getInventory()[10];
                    MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(),
                            (byte) 10, false, false);
                    if (from.isConsumable()) {
                        moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte) 10, false,
                                true);
                    }
                    this.realmManager.getClient().sendRemote(moveItem);
                } catch (Exception e) {
                    PlayState.log.error("Failed to send move item packet: {}", "No Item in slot");
                }
            }
            if (key.eight.clicked) {
                try {
                    GameItem from = this.getPlayer().getInventory()[11];
                    MoveItemPacket moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(),
                            (byte) 11, false, false);
                    if (from.isConsumable()) {
                        moveItem = MoveItemPacket.from(this.getPlayer().getId(), from.getTargetSlot(), (byte) 11, false,
                                true);
                    }
                    this.realmManager.getClient().sendRemote(moveItem);
                } catch (Exception e) {
                    PlayState.log.error("Failed to send move item packet: {}", "No Item in slot");
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
                PauseState pause = new PauseState(this.gsm, this.getAccount());
                this.gsm.add(GameStateManager.PAUSE, pause);
            }
        }

        int dex = (int) ((6.5 * (this.getPlayer().getComputedStats().getDex() + 17.3)) / 75);
        boolean canShoot = true;
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
    public void render(Graphics2D g) {
        Player player = this.realmManager.getRealm().getPlayer(this.playerId);
        // TODO: Translate everything to screen coordinates to keep it centered
        if (player == null)
            return;
        this.realmManager.getRealm().getTileManager().render(player, g);

        for (Player p : this.realmManager.getRealm().getPlayers().values()) {
            p.render(g);
            p.updateAnimation();
        }

        GameObject[] gameObject = this.realmManager.getRealm()
                .getGameObjectsInBounds(this.realmManager.getRealm().getTileManager().getRenderViewPort(player));

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

        if (this.pui == null)
            return;
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
        if ((closeLoot != null && this.getPui().isGroundLootEmpty())
                || (closeLoot != null && closeLoot.getContentsChanged())) {
            this.getPui().setGroundLoot(LootContainer.getCondensedItems(closeLoot), g);

        } else if ((closeLoot == null) && !this.getPui().isGroundLootEmpty()) {
            this.getPui().setGroundLoot(new GameItem[8], g);
        }

        if (closeLoot != null && !this.getPui().isGroundLootEmpty()) {
            final boolean contentsChanged = this.getPui().getNonEmptySlotCount() != closeLoot.getNonEmptySlotCount();
            if (contentsChanged) {
                this.getPui().setGroundLoot(LootContainer.getCondensedItems(closeLoot), g);
            }
        }
    }

    public Player getPlayer() {
        return this.realmManager.getRealm().getPlayer(this.playerId);
    }

    @SuppressWarnings("unused")
    private void renderCollisionBoxes(Graphics2D g) {

        GameObject[] gameObject = this.realmManager.getRealm().getGameObjectsInBounds(this.cam.getBounds());
        Rectangle[] colBoxes = this.realmManager.getRealm().getCollisionBoxesInBounds(this.cam.getBounds());
        for (GameObject go : gameObject) {
            Rectangle node = go.getBounds();

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

        for (Rectangle node : colBoxes) {

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
