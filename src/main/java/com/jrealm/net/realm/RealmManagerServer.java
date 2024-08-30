package com.jrealm.net.realm;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.jrealm.account.dto.CharacterDto;
import com.jrealm.account.dto.CharacterStatsDto;
import com.jrealm.account.dto.GameItemRefDto;
import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.EntityType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.LootTier;
import com.jrealm.game.contants.PacketType;
import com.jrealm.game.contants.ProjectilePositionMode;
import com.jrealm.game.contants.TextEffect;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.messaging.ServerCommandMessage;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.LootTableModel;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.script.Enemy10Script;
import com.jrealm.game.script.Enemy11Script;
import com.jrealm.game.script.Enemy12Script;
import com.jrealm.game.script.Enemy13Script;
import com.jrealm.game.script.Enemy14Script;
import com.jrealm.game.script.EnemyScriptBase;
import com.jrealm.game.script.item.Item153Script;
import com.jrealm.game.script.item.Item156Script;
import com.jrealm.game.script.item.Item157Script;
import com.jrealm.game.script.item.UseableItemScript;
import com.jrealm.game.script.item.UseableItemScriptBase;
import com.jrealm.game.tile.NetTile;
import com.jrealm.game.tile.Tile;
import com.jrealm.game.tile.TileMap;
import com.jrealm.game.tile.decorators.Beach0Decorator;
import com.jrealm.game.tile.decorators.Grasslands0Decorator;
import com.jrealm.game.tile.decorators.RealmDecorator;
import com.jrealm.game.tile.decorators.RealmDecoratorBase;
import com.jrealm.game.util.CommandHandler;
import com.jrealm.game.util.PacketHandler;
import com.jrealm.game.util.TimedWorkerThread;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.PlayerDeathPacket;
import com.jrealm.net.client.packet.TextEffectPacket;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.ProcessingThread;
import com.jrealm.net.server.ServerCommandHandler;
import com.jrealm.net.server.ServerGameLogic;
import com.jrealm.net.server.SocketServer;
import com.jrealm.net.server.packet.TextPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("unused")
public class RealmManagerServer implements Runnable {
    private SocketServer server;
    private boolean shutdown = false;
    private Reflections classPathScanner = new Reflections("com.jrealm", Scanners.SubTypes, Scanners.MethodsAnnotated);
    private MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
    private final Map<Byte, BiConsumer<RealmManagerServer, Packet>> packetCallbacksServer = new HashMap<>();
    private final Map<Byte, List<MethodHandle>> userPacketCallbacksServer = new HashMap<>();

    private List<Vector2f> shotDestQueue;
    private Map<Long, Realm> realms = new HashMap<>();
    private Map<String, Long> remoteAddresses = new HashMap<>();
    private Map<Long, Long> playerAbilityState = new HashMap<>();
    private Map<Long, LoadPacket> playerLoadState = new HashMap<>();
    private Map<Long, UpdatePacket> playerUpdateState = new HashMap<>();
    private Map<Long, UnloadPacket> playerUnloadState = new HashMap<>();
    private Map<Long, LoadMapPacket> playerLoadMapState = new HashMap<>();
    private Map<Long, ObjectMovePacket> playerObjectMoveState = new HashMap<>();
    private UnloadPacket lastUnload;
    private volatile Queue<Packet> outboundPacketQueue = new ConcurrentLinkedQueue<>();
    private volatile Map<Long, ConcurrentLinkedQueue<Packet>> playerOutboundPacketQueue = new HashMap<Long, ConcurrentLinkedQueue<Packet>>();
    private List<RealmDecoratorBase> realmDecorators = new ArrayList<>();
    private List<EnemyScriptBase> enemyScripts = new ArrayList<>();
    private List<UseableItemScriptBase> itemScripts = new ArrayList<>();
    private Semaphore realmLock = new Semaphore(1);
    private int currentTickCount = 0;
    private long tickSampleTime = 0;

    public RealmManagerServer() {
        this.registerRealmDecorators();
        this.registerEnemyScripts();
        this.registerPacketCallbacks();
        this.registerPacketCallbacksReflection();
        this.registerItemScripts();
        this.registerCommandHandlersReflection();
        this.server = new SocketServer(2222);
        this.shotDestQueue = new ArrayList<>();
        WorkerThread.submitAndForkRun(this.server);
        this.beginPlayerSync();
    }

    // Adds a specified amount of random headless players
    public void spawnTestPlayers(final long realmId, final int count, final Vector2f pos) {
        final Realm targetRealm = this.realms.get(realmId);
        final Runnable spawnTestPlayers = () -> {
            final Random random = new Random(Instant.now().toEpochMilli());
            for (int i = 0; i < count; i++) {
                final CharacterClass classToSpawn = CharacterClass.getCharacterClasses()
                        .get(random.nextInt(CharacterClass.getCharacterClasses().size()));
                try {
                    final Vector2f spawnPos = targetRealm.getTileManager().getSafePosition();
                    final Player player = new Player(Realm.RANDOM.nextLong(), pos, GlobalConstants.PLAYER_SIZE,
                            classToSpawn);
                    String playerName = UUID.randomUUID().toString().replaceAll("-", "");
                    playerName = playerName.substring(playerName.length() / 2);
                    player.setName(playerName);
                    player.equipSlots(GameDataManager.getStartingEquipment(classToSpawn));
                    player.setCharacterUuid(UUID.randomUUID().toString());
                    player.setAccountUuid(UUID.randomUUID().toString());

                    final boolean up = random.nextBoolean();
                    final boolean right = random.nextBoolean();

                    if (up) {
                        player.setUp(true);
                    } else {
                        player.setDown(true);
                    }
                    if (right) {
                        player.setRight(true);
                    } else {
                        player.setLeft(true);
                    }

                    player.setHeadless(true);
                    final long newId = targetRealm.addPlayer(player);
                } catch (Exception e) {
                    RealmManagerServer.log.error("Failed to spawn test character of class type {}. Reason: {}",
                            classToSpawn, e);
                }
            }
        };
        // Run this in a completely separate thread
        WorkerThread.submitAndForkRun(spawnTestPlayers);
    }

    @Override
    public void run() {
        RealmManagerServer.log.info("Starting JRealm Server");
        final Runnable tick = () -> {
            this.tick();
            this.update(0);
        };

        final TimedWorkerThread workerThread = new TimedWorkerThread(tick, 64);
        WorkerThread.submitAndForkRun(workerThread);
        RealmManagerServer.log.info("[SERVER] RealmManagerServer exiting run().");
    }

    private void tick() {
        try {

            final Runnable enqueueGameData = () -> {
                this.enqueueGameData();
            };

            final Runnable processServerPackets = () -> {
                this.processServerPackets();
            };

            final Runnable sendGameData = () -> {
                this.sendGameData();
            };
            // We dont necessarily care what order these three tasks take place in
            // rather that all three are completed at least once per tick, so run them
            // asynchronously
            WorkerThread.submitAndRun(enqueueGameData, processServerPackets, sendGameData);
            if (Instant.now().toEpochMilli() - this.tickSampleTime > 1000) {
                this.tickSampleTime = Instant.now().toEpochMilli();
                log.info("[SERVER] ticks this second: {}", this.currentTickCount);
                this.currentTickCount = 0;
            } else {
                this.currentTickCount++;
            }
        } catch (Exception e) {
            RealmManagerServer.log.error("Failed to sleep");
        }
    }

    private void sendGameData() {
        final List<Packet> packetsToBroadcast = new ArrayList<>();
        // TODO: Possibly rework this queue as we dont usually send stuff globally
        while (!this.outboundPacketQueue.isEmpty()) {
            packetsToBroadcast.add(this.outboundPacketQueue.remove());
        }
        final List<Map.Entry<String, ProcessingThread>> staleProcessingThreads = new ArrayList<>();
        for (final Map.Entry<String, ProcessingThread> client : this.server.getClients().entrySet()) {
            if (client.getValue().getClientSocket().isClosed() || !client.getValue().getClientSocket().isConnected()) {
                staleProcessingThreads.add(client);
            }
        }
        staleProcessingThreads.forEach(thread -> {
            try {
                thread.getValue().setShutdownProcessing(true);
                this.server.getClients().remove(thread.getKey());
                this.server.getClients().remove(thread.getKey(), thread.getValue());
            } catch (Exception e) {
                log.error("Failed to remove stale processing threads. Reason:  {}", e);
            }
        });
        final List<String> disconnectedClients = new ArrayList<>();
        for (final Map.Entry<String, ProcessingThread> client : this.server.getClients().entrySet()) {
            try {
                // Dequeue and send any player specific packets
                final List<Packet> playerPackets = new ArrayList<>();
                final ConcurrentLinkedQueue<Packet> playerPacketsToSend = this.playerOutboundPacketQueue
                        .get(this.remoteAddresses.get(client.getKey()));

                while ((playerPacketsToSend != null) && !playerPacketsToSend.isEmpty()) {
                    playerPackets.add(playerPacketsToSend.remove());
                }
                
                final OutputStream toClientStream = client.getValue().getClientSocket().getOutputStream();
                final DataOutputStream dosToClient = new DataOutputStream(toClientStream);

                for (final Packet packet : packetsToBroadcast) {
                    packet.serializeWrite(dosToClient);
                }

                for (final Packet packet : playerPackets) {
                    packet.serializeWrite(dosToClient);
                }
            } catch (Exception e) {
                disconnectedClients.add(client.getKey());
                RealmManagerServer.log.error("Failed to get OutputStream to Client. Reason: {}", e);
            }
        }
    }

    // Enqueues outbound game packets every tick. Manages
    // when/if packets should be broadcast and to who
    public void enqueueGameData() {
        final List<String> disconnectedClients = new ArrayList<>();
        // TODO: Parallelize work for each realm
        // For each realm we have to do work for

        // Prevent concurrent modification errors by acquiring a semaphore lease
        // while we are building the game data for this tick
        this.acquireRealmLock();
        for (final Map.Entry<Long, Realm> realmEntry : this.realms.entrySet()) {
            final Realm realm = realmEntry.getValue();
            for (final Map.Entry<Long, Player> player : realm.getPlayers().entrySet()) {
                if (player.getValue().isHeadless()) {
                    continue;
                }
                try {
                    // Get UpdatePacket for this player and all players in this players viewport
                    // Contains, player stat info, inventory, status effects, health and mana data
                    final UpdatePacket updatePacket = realm.getPlayerAsPacket(player.getValue().getId());

                    // Get the background + collision tiles in this players viewport
                    // condensed into a single array
                    final NetTile[] netTilesForPlayer = realm.getTileManager().getLoadMapTiles(player.getValue());
                    // Build those tiles into a load map packet (NetTile[] wrapper)
                    final LoadMapPacket newLoadMapPacket = LoadMapPacket.from(realm.getRealmId(),
                            (short) realm.getMapId(), netTilesForPlayer);

                    // If we dont have load map state for this player, map it and
                    // then transmit all the tiles
                    if (this.playerLoadMapState.get(player.getKey()) == null) {
                        this.playerLoadMapState.put(player.getKey(), newLoadMapPacket);
                        this.enqueueServerPacket(player.getValue(), newLoadMapPacket);
                    } else {
                        // Get the previous loadMap packet and check for Delta,
                        // only send the delta to the client
                        final LoadMapPacket oldLoadMapPacket = this.playerLoadMapState.get(player.getKey());

                        if (!oldLoadMapPacket.equals(newLoadMapPacket)) {
                            final LoadMapPacket loadMapDiff = oldLoadMapPacket.difference(newLoadMapPacket);
                            this.playerLoadMapState.put(player.getKey(), newLoadMapPacket);
                            if (loadMapDiff != null) {
                                this.enqueueServerPacket(player.getValue(), loadMapDiff);
                            }
                        }
                    }
                    // Get LoadPacket for this player
                    // Contains newly spawned bullets, entities, players
                    final LoadPacket loadPacket = realm
                            .getLoadPacket(realm.getTileManager().getRenderViewPort(player.getValue()));

                    // Get the posX, posY, dX, dY of all Entities in this players viewport
                    final ObjectMovePacket movePacket = realm
                            .getGameObjectsAsPackets(realm.getTileManager().getRenderViewPort(player.getValue()));

                    // Only transmit this players update data if it has not been sent
                    // or if it has changed since last tick
                    if (this.playerUpdateState.get(player.getKey()) == null) {
                        this.playerUpdateState.put(player.getKey(), updatePacket);
                        this.enqueueServerPacket(player.getValue(), updatePacket);
                    } else {
                        final UpdatePacket oldUpdate = this.playerUpdateState.get(player.getKey());
                        if (!oldUpdate.equals(updatePacket)) {
                            this.playerUpdateState.put(player.getKey(), updatePacket);
                            this.enqueueServerPacket(player.getValue(), updatePacket);
                        }
                    }

                    // Only transmit the LoadPacket if its state is changed (it can potentially be
                    // large).
                    // If the state is changed, only transmit the DELTA data
                    if (this.playerLoadState.get(player.getKey()) == null) {
                        this.playerLoadState.put(player.getKey(), loadPacket);
                        this.enqueueServerPacket(player.getValue(), loadPacket);
                    } else {
                        final LoadPacket oldLoad = this.playerLoadState.get(player.getKey());
                        if (!oldLoad.equals(loadPacket)) {
                            // Get the LoadPacket delta
                            final LoadPacket toSend = oldLoad.combine(loadPacket);
                            this.playerLoadState.put(player.getKey(), loadPacket);
                            this.enqueueServerPacket(player.getValue(), toSend);

                            // Unload the delta objects that were in the old LoadPacket
                            // but are NOT in the new LoadPacket
                            final UnloadPacket unloadDelta = oldLoad.difference(loadPacket);
                            if (unloadDelta.isNotEmpty()) {
                                this.enqueueServerPacket(player.getValue(), unloadDelta);
                            }
                        }
                    }

                    // If the ObjectMove packet isnt empty
                    if (this.playerObjectMoveState.get(player.getKey()) == null && movePacket != null) {
                        this.playerObjectMoveState.put(player.getKey(), movePacket);
                        this.enqueueServerPacket(player.getValue(), movePacket);
                    } else if (movePacket != null) {
                        final ObjectMovePacket oldMove = this.playerObjectMoveState.get(player.getKey());
                        if (oldMove != null && !oldMove.equals(movePacket)) {
                            this.playerObjectMoveState.put(player.getKey(), movePacket);
                            this.enqueueServerPacket(player.getValue(), movePacket);
                        } else {
                            final ObjectMovePacket moveDiff = oldMove.getMoveDiff(movePacket);
                            if (moveDiff != null) {
                                this.playerObjectMoveState.put(player.getKey(), movePacket);
                                this.enqueueServerPacket(player.getValue(), movePacket);
                            }
                        }
                    }

                    // Used to dynamically re-render changed loot containers (chests) on the client
                    // if their contents change in a server tick (receive MoveItem packet from
                    // client this tick)
                    for (LootContainer lc : realm.getLoot().values()) {
                        lc.setContentsChanged(false);
                    }
                } catch (Exception e) {
                    RealmManagerServer.log.error("Failed to build game data for Player {}. Reason: {}", player.getKey(),
                            e);
                }
            }
        }
        this.releaseRealmLock();
    }

    // For each connected client, dequeue all pending packets
    // pass the packet and RealmManager context to the handler
    // script
    public void processServerPackets() {
        for (final Map.Entry<String, ProcessingThread> thread : this.getServer().getClients().entrySet()) {
            if (!thread.getValue().isShutdownProcessing()) {
                // Read all packets from the ProcessingThread (client's) queue
                while (!thread.getValue().getPacketQueue().isEmpty()) {
                    final Packet packet = thread.getValue().getPacketQueue().remove();
                    try {
                        final Packet created = Packet.newInstance(packet.getId(), packet.getData());
                        created.setSrcIp(packet.getSrcIp());
                        // Invoke packet callback
                        List<MethodHandle> packetHandles = userPacketCallbacksServer.get(packet.getId());
                        long start = System.nanoTime();
                        if(packetHandles!=null) {
                            for(MethodHandle handler : packetHandles) {
                                try {
                                    handler.invokeExact(this, created);
                                } catch (Throwable e) {
                                   log.error("Failed to invoke packet callback. Reason: {}", e);
                                }
                            }
                            log.info("Invoked {} packet callbacks for PacketType {} using reflection in {} nanos", packetHandles.size(), PacketType.valueOf(created.getId()).getY(),(System.nanoTime()-start));

                        }
                        start = System.nanoTime();
                        this.packetCallbacksServer.get(created.getId()).accept(this, created);
                        log.info("Invoked callback using map in {} nanos", (System.nanoTime()-start));
                    
                    } catch (Exception e) {
                        RealmManagerServer.log.error("Failed to process server packets {}", e);
                        thread.getValue().setShutdownProcessing(true);
                    }
                }
            } else {
                // Player Disconnect routine
                final Long dcPlayerId = this.getRemoteAddresses().get(thread.getKey());
                if (dcPlayerId == null) {
                    thread.getValue().setShutdownProcessing(true);
                    return;
                }
                final Realm playerLocation = this.findPlayerRealm(dcPlayerId);
                if (playerLocation != null) {
                    final Player dcPlayer = playerLocation.getPlayer(dcPlayerId);
                    this.persistPlayerAsync(dcPlayer);
                    playerLocation.getExpiredPlayers().add(dcPlayerId);
                    playerLocation.getPlayers().remove(dcPlayerId);
                }
                this.server.getClients().remove(thread.getKey());
            }
        }
    }

    public Realm getTopRealm() {
        Realm result = null;
        for (final Realm realm : this.realms.values()) {
            if (realm.getDepth() == 0) {
                result = realm;
            }
        }
        return result;
    }

    public Portal getClosestPortal(final long realmId, final Vector2f pos, final float limit) {
        float best = Float.MAX_VALUE;
        Portal bestPortal = null;
        final Realm targetRealm = this.realms.get(realmId);
        for (final Portal portal : targetRealm.getPortals().values()) {
            float dist = portal.getPos().distanceTo(pos);
            if ((dist < best) && (dist <= limit)) {
                best = dist;
                bestPortal = portal;
            }
        }
        return bestPortal;
    }

    public Player getClosestPlayer(final long realmId, final Vector2f pos, final float limit) {
        float best = Float.MAX_VALUE;
        Player bestPlayer = null;
        final Realm targetRealm = this.realms.get(realmId);
        for (final Player player : targetRealm.getPlayers().values()) {
            final float dist = player.getPos().distanceTo(pos);
            if ((dist < best) && (dist <= limit)) {
                best = dist;
                bestPlayer = player;
            }
        }
        return bestPlayer;
    }

    public LootContainer getClosestLootContainer(final long realmId, final Vector2f pos, final float limit) {
        float best = Float.MAX_VALUE;
        LootContainer bestLoot = null;
        final Realm targetRealm = this.realms.get(realmId);
        for (final LootContainer lootContainer : targetRealm.getLoot().values()) {
            float dist = lootContainer.getPos().distanceTo(pos);
            if ((dist < best) && (dist <= limit)) {
                best = dist;
                bestLoot = lootContainer;
            }
        }
        return bestLoot;
    }

    private UnloadPacket getUnloadPacket(long realmId) throws Exception {
        final Realm targetRealm = this.realms.get(realmId);
        final Long[] expiredBullets = targetRealm.getExpiredBullets().toArray(new Long[0]);
        final Long[] expiredEnemies = targetRealm.getExpiredEnemies().toArray(new Long[0]);
        final Long[] expiredPlayers = targetRealm.getExpiredPlayers().toArray(new Long[0]);
        targetRealm.getExpiredPlayers().clear();
        targetRealm.getExpiredBullets().clear();
        targetRealm.getExpiredEnemies().clear();
        final List<Long> lootContainers = targetRealm.getLoot().values().stream()
                .filter(lc -> lc.isExpired() || lc.isEmpty()).map(LootContainer::getLootContainerId)
                .collect(Collectors.toList());
        final List<Long> portals = targetRealm.getPortals().values().stream().filter(Portal::isExpired)
                .map(Portal::getId).collect(Collectors.toList());
        for (final Long lcId : lootContainers) {
            targetRealm.getLoot().remove(lcId);
        }
        for (final Long pId : portals) {
            targetRealm.getPortals().remove(pId);
        }
        return UnloadPacket.from(expiredPlayers, lootContainers.toArray(new Long[0]), expiredBullets, expiredEnemies,
                portals.toArray(new Long[0]));
    }

    public void tryDecorate(final Realm realm) {
        final RealmDecorator decorator = this.getRealmDecorator(realm.getMapId());
        if (decorator != null) {
            decorator.decorate(realm);
        }
    }

    private RealmDecorator getRealmDecorator(int mapId) {
        RealmDecorator result = null;
        for (final RealmDecorator decorator : this.realmDecorators) {
            if (decorator.getTargetMapId() == mapId) {
                result = decorator;
            }
        }
        return result;
    }

    public EnemyScriptBase getEnemyScript(int enemyId) {
        EnemyScriptBase result = null;
        for (final EnemyScriptBase enemyScript : this.enemyScripts) {
            if (enemyScript.getTargetEnemyId() == enemyId) {
                result = enemyScript;
            }
        }
        return result;
    }

    public UseableItemScriptBase getItemScript(int itemId) {
        UseableItemScriptBase result = null;
        for (final UseableItemScriptBase itemScript : this.itemScripts) {
            if (itemScript.getTargetItemId() == itemId) {
                result = itemScript;
            }
        }
        return result;
    }

    // Register any custom realm type decorators.
    // Each time a realm of the target type is added, an instance
    // of it will be passed to the decorator for post processing
    // (generate static enemies, terrain, events)
    private void registerRealmDecorators() {
        this.registerRealmDecoratorsReflection();
    }

    private void registerRealmDecoratorsReflection() {
        Set<Class<? extends RealmDecoratorBase>> subclasses = this.classPathScanner
                .getSubTypesOf(RealmDecoratorBase.class);
        for (Class<? extends RealmDecoratorBase> clazz : subclasses) {
            try {
                final RealmDecoratorBase realmDecoratorInstance = clazz.getDeclaredConstructor(RealmManagerServer.class)
                        .newInstance(this);
                this.realmDecorators.add(realmDecoratorInstance);
            } catch (Exception e) {
                log.error("Failed to register realm decorator for script {}. Reason: {}", clazz, e.getMessage());
            }
        }
    }

    private void registerEnemyScripts() {
        this.registerEnemyScriptsReflection();
    }

    private void registerEnemyScriptsReflection() {
        Set<Class<? extends EnemyScriptBase>> subclasses = this.classPathScanner.getSubTypesOf(EnemyScriptBase.class);
        for (Class<? extends EnemyScriptBase> clazz : subclasses) {
            try {
                final EnemyScriptBase realmDecoratorInstance = clazz.getDeclaredConstructor(RealmManagerServer.class)
                        .newInstance(this);
                this.enemyScripts.add(realmDecoratorInstance);
            } catch (Exception e) {
                log.error("Failed to register enemy script for script {}. Reason: {}", clazz, e.getMessage());
            }
        }
    }

    private void registerItemScripts() {
        this.registerItemScriptsReflection();
    }

    private void registerItemScriptsReflection() {
        Set<Class<? extends UseableItemScriptBase>> subclasses = this.classPathScanner
                .getSubTypesOf(UseableItemScriptBase.class);
        for (Class<? extends UseableItemScriptBase> clazz : subclasses) {
            try {
                final UseableItemScriptBase realmDecoratorInstance = clazz
                        .getDeclaredConstructor(RealmManagerServer.class).newInstance(this);
                this.itemScripts.add(realmDecoratorInstance);
            } catch (Exception e) {
                log.error("Failed to register useable item script for script {}. Reason: {}", clazz, e.getMessage());
            }
        }
    }

    private void registerCommandHandlersReflection() {
        // Target method signature. ex. public static void myMethod(RealmManagerServer,
        // Player, ServerCommandMessage)
        final MethodType mt = MethodType.methodType(void.class, RealmManagerServer.class, Player.class,
                ServerCommandMessage.class);
        final Set<Method> subclasses = this.classPathScanner.getMethodsAnnotatedWith(CommandHandler.class);
        for (final Method method : subclasses) {
            try {
                // Get the annotation on the method
                final CommandHandler commandToHandle = method.getDeclaredAnnotation(CommandHandler.class);
                // Find the static method with given name in the target class
                final MethodHandle handleToHandler = this.publicLookup.findStatic(ServerCommandHandler.class,
                        method.getName(), mt);
                if (handleToHandler != null) {
                    ServerCommandHandler.COMMAND_CALLBACKS.put(commandToHandle.value(), handleToHandler);
                }
            } catch (Exception e) {
                log.error("Failed to get MethodHandle to method {}. Reason: {}", method.getName(), e);
            }
        }
    }

    // Registers any user defined packet callbacks with the server
    private void registerPacketCallbacksReflection() {
        log.info("Registering packet handlers using reflection");
        final MethodType mt = MethodType.methodType(void.class, RealmManagerServer.class, Packet.class);
        final Set<Method> subclasses = this.classPathScanner.getMethodsAnnotatedWith(PacketHandler.class);
        for (final Method method : subclasses) {
            try {
                final PacketHandler packetToHandle = method.getDeclaredAnnotation(PacketHandler.class);
                final MethodHandle handleToHandler = this.publicLookup.findStatic(ServerGameLogic.class,
                        method.getName(), mt);
                if (handleToHandler != null) {
                    PacketType targetPacketType = PacketType.valueOf(packetToHandle.value());
                    List<MethodHandle> existing  = userPacketCallbacksServer.get(targetPacketType.getPacketId());
                    if(existing==null) {
                        existing = new ArrayList<>();
                    }
                    existing.add(handleToHandler);
                    log.info("Added new packet handler for packet {}. Handler method: {}", targetPacketType, handleToHandler.toString());
                    this.userPacketCallbacksServer.put(targetPacketType.getPacketId(), existing);

                }
            } catch (Exception e) {
                log.error("Failed to get MethodHandle to method {}. Reason: {}", method.getName(), e);
            }
        }
    }
    // For packet callbacks requiring high performance we will invoke them in a functional mager using
    // a hashmap to store the references. The server operator is encouraged to add auxilary packet
    // handling functionality using the @PacketHandler annotation
    private void registerPacketCallbacks() {
        this.registerPacketCallback(PacketType.PLAYER_MOVE.getPacketId(), ServerGameLogic::handlePlayerMoveServer);
        this.registerPacketCallback(PacketType.PLAYER_SHOOT.getPacketId(), ServerGameLogic::handlePlayerShootServer);
        this.registerPacketCallback(PacketType.HEARTBEAT.getPacketId(), ServerGameLogic::handleHeartbeatServer);
        this.registerPacketCallback(PacketType.TEXT.getPacketId(), ServerGameLogic::handleTextServer);
        this.registerPacketCallback(PacketType.COMMAND.getPacketId(), ServerGameLogic::handleCommandServer);
        this.registerPacketCallback(PacketType.LOAD_MAP.getPacketId(), ServerGameLogic::handleLoadMapServer);
        this.registerPacketCallback(PacketType.USE_ABILITY.getPacketId(), ServerGameLogic::handleUseAbilityServer);
        this.registerPacketCallback(PacketType.MOVE_ITEM.getPacketId(), ServerGameLogic::handleMoveItemServer);
        this.registerPacketCallback(PacketType.USE_PORTAL.getPacketId(), ServerGameLogic::handleUsePortalServer);
    }

    private void registerPacketCallback(final byte packetId, final BiConsumer<RealmManagerServer, Packet> callback) {
        this.packetCallbacksServer.put(packetId, callback);
    }

    // Updates all game objects on the server
    public void update(double time) {
        // For each world on the server
        for (final Map.Entry<Long, Realm> realmEntry : this.realms.entrySet()) {
            final Realm realm = realmEntry.getValue();
            // Update player specific game objects (bullets, the players themselves)
            for (final Map.Entry<Long, Player> player : realm.getPlayers().entrySet()) {
                final Player p = realm.getPlayer(player.getValue().getId());
                if (p == null) {
                    continue;
                }

                final Runnable processGameObjects = () -> {
                    this.processBulletHit(realm.getRealmId(), p);
                };
                // Rewrite this asap
                final Runnable updatePlayer = () -> {
                    p.update(time);
                    p.removeExpiredEffects();
                    this.movePlayer(realm.getRealmId(), p);
                };
                // Run the player update tasks Asynchronously
                WorkerThread.submitAndRun(processGameObjects, updatePlayer);
            }
            // Once per tick update all non player game objects
            // (bullets, enemies)
            final Runnable processGameObjects = () -> {
                final GameObject[] gameObject = realm.getAllGameObjects();
                for (int i = 0; i < gameObject.length; i++) {
                    if (gameObject[i] instanceof Enemy) {
                        final Enemy enemy = ((Enemy) gameObject[i]);
                        enemy.update(realm.getRealmId(), this, time);
                        enemy.removeExpiredEffects();
                    }

                    if (gameObject[i] instanceof Bullet) {
                        final Bullet bullet = ((Bullet) gameObject[i]);
                        if (bullet != null) {
                            bullet.update();
                        }
                    }
                }
            };
            WorkerThread.submitAndRun(processGameObjects);
        }

        final Runnable removeExpiredObjects = () -> {
            this.removeExpiredBullets();
            this.removeExpiredLootContainers();
            this.removeExpiredPortals();
        };
        WorkerThread.submitAndRun(removeExpiredObjects);
    }

    private void movePlayer(final long realmId, final Player p) {
        final Realm targetRealm = this.realms.get(realmId);

        if (!targetRealm.getTileManager().collisionTile(p, p.getDx(), 0)
                && !targetRealm.getTileManager().collidesXLimit(p, p.getDx())) {
            p.xCol = false;
            if (targetRealm.getTileManager().collidesSlowTile(p)) {
                p.getPos().x += p.getDx() / 3.0f;
            } else {
                p.getPos().x += p.getDx();
            }
        } else {
            p.xCol = true;
        }

        if (!targetRealm.getTileManager().collisionTile(p, 0, p.getDy())
                && !targetRealm.getTileManager().collidesYLimit(p, p.getDy())) {
            p.yCol = false;
            if (targetRealm.getTileManager().collidesSlowTile(p)) {
                p.getPos().y += p.getDy() / 3.0f;
            } else {
                p.getPos().y += p.getDy();
            }
        } else {
            p.yCol = true;
        }

        p.move();
    }

    // Invokes an ability usage server side for the given player at the
    // desired location if applicable
    public void useAbility(final long realmId, final long playerId, final Vector2f pos) {
        final Realm targetRealm = this.realms.get(realmId);

        final Player player = targetRealm.getPlayer(playerId);
        final GameItem abilityItem = player.getAbility();
        if ((abilityItem == null))
            return;
        final Effect effect = abilityItem.getEffect();
        final Long lastAbilityUsage = this.playerAbilityState.get(playerId);
        if(lastAbilityUsage==null || (Instant.now().toEpochMilli() - lastAbilityUsage >= effect.getCooldownDuration())) {
        	this.playerAbilityState.put(playerId, Instant.now().toEpochMilli());
        }else {
        	log.info("Ability {} is on cooldown", abilityItem);
        	return;
        }
        if (player.getMana() < effect.getMpCost())
            return;
        player.setMana(player.getMana() - effect.getMpCost());
        // If the ability is damaging (knight stun, archer arrow, wizard spell)
        if (((abilityItem.getDamage() != null) && (abilityItem.getEffect() != null))) {
            final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS
                    .get(abilityItem.getDamage().getProjectileGroupId());

            final Vector2f dest = new Vector2f(pos.x, pos.y);

            Vector2f source = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
            final float angle = Bullet.getAngle(source, dest);

            for (final Projectile p : group.getProjectiles()) {
                final short offset = (short) (p.getSize() / (short) 2);
                short rolledDamage = player.getInventory()[1].getDamage().getInRange();
                rolledDamage += player.getComputedStats().getAtt();
                if (p.getPositionMode() != ProjectilePositionMode.TARGET_PLAYER) {
                    source = dest;
                }
                this.addProjectile(realmId, 0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
                        p.getProjectileId(), source.clone(-offset, -offset), angle + Float.parseFloat(p.getAngle()),
                        p.getSize(), p.getMagnitude(), p.getRange(), rolledDamage, false, p.getFlags(),
                        p.getAmplitude(), p.getFrequency());
            }
        } else if ((abilityItem.getDamage() != null)) {
            final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS
                    .get(abilityItem.getDamage().getProjectileGroupId());
            final Vector2f dest = new Vector2f(pos.x, pos.y);
            for (final Projectile p : group.getProjectiles()) {

                final short offset = (short) (p.getSize() / (short) 2);
                short rolledDamage = player.getInventory()[1].getDamage().getInRange();
                rolledDamage += player.getComputedStats().getAtt();
                this.addProjectile(realmId, 0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
                        p.getProjectileId(), dest.clone(-offset, -offset), Float.parseFloat(p.getAngle()), p.getSize(),
                        p.getMagnitude(), p.getRange(), rolledDamage, false, p.getFlags(), p.getAmplitude(),
                        p.getFrequency());
            }

            // If the ability is non damaging (rogue cloak, priest tome)
        } else if (abilityItem.getEffect() != null) {
            player.addEffect(abilityItem.getEffect().getEffectId(), abilityItem.getEffect().getDuration());
        }

        UseableItemScriptBase script = this.getItemScript(abilityItem.getItemId());
        if (script != null) {
            script.invokeItemAbility(targetRealm, player, abilityItem);
        }
    }

    public void removeExpiredBullets() {
        for (final Map.Entry<Long, Realm> realmEntry : this.realms.entrySet()) {
            final Realm realm = realmEntry.getValue();

            final List<Bullet> toRemove = new ArrayList<>();
            for (final Bullet b : realm.getBullets().values()) {
                if (b.remove()) {
                    toRemove.add(b);
                }
            }
            toRemove.forEach(bullet -> {
                realm.getExpiredBullets().add(bullet.getId());
                realm.removeBullet(bullet);
            });
        }
    }

    public void removeExpiredLootContainers() {
        for (final Map.Entry<Long, Realm> realmEntry : this.realms.entrySet()) {
            final Realm realm = realmEntry.getValue();

            final List<LootContainer> toRemove = new ArrayList<>();
            for (final LootContainer lc : realm.getLoot().values()) {
                if (lc.isExpired() || lc.isEmpty()) {
                    toRemove.add(lc);
                }
            }
            toRemove.forEach(lc -> {
                realm.removeLootContainer(lc);
            });
        }
    }

    public void removeExpiredPortals() {
        for (final Map.Entry<Long, Realm> realmEntry : this.realms.entrySet()) {
            final Realm realm = realmEntry.getValue();

            final List<Portal> toRemove = new ArrayList<>();
            for (final Portal portal : realm.getPortals().values()) {
                if (portal.isExpired()) {
                    toRemove.add(portal);
                }
            }
            toRemove.forEach(portal -> {
                realm.removePortal(portal);
            });
        }
    }

    public void processBulletHit(final long realmId, final Player p) {
        final Realm targetRealm = this.realms.get(realmId);
        final List<Bullet> results = this.getBullets(realmId, p);
        final GameObject[] gameObject = targetRealm
                .getGameObjectsInBounds(targetRealm.getTileManager().getRenderViewPort(p));
        final Player player = targetRealm.getPlayer(p.getId());

        if (!player.hasEffect(EffectType.INVINCIBLE)) {
            for (final Bullet b : results) {
                this.processPlayerHit(realmId, b, player);
            }
        }

        for (int i = 0; i < gameObject.length; i++) {
            if (gameObject[i] instanceof Enemy) {
                final Enemy enemy = ((Enemy) gameObject[i]);
                for (final Bullet b : results) {
                    this.proccessEnemyHit(realmId, b, enemy);
                }
            }
        }
        this.proccessTerrainHit(realmId, p);
    }

    // This may not need to be synchronized
    // Enqueues a server packet to be transmitted to all players
    public synchronized void enqueueServerPacket(final Packet packet) {
        this.outboundPacketQueue.add(packet);
    }

    // This may not need to be synchronized
    // Enqueues a packet to be transmitted to only Player player
    public synchronized void enqueueServerPacket(final Player player, final Packet packet) {
        if (this.playerOutboundPacketQueue.get(player.getId()) == null) {
            final ConcurrentLinkedQueue<Packet> packets = new ConcurrentLinkedQueue<>();
            packets.add(packet);
            this.playerOutboundPacketQueue.put(player.getId(), packets);
        } else {
            this.playerOutboundPacketQueue.get(player.getId()).add(packet);
        }
    }

    private void proccessTerrainHit(final long realmId, final Player p) {
        final Realm targetRealm = this.realms.get(realmId);

        final List<Bullet> toRemove = new ArrayList<>();
        final TileMap currentMap = targetRealm.getTileManager().getCollisionLayer();
        Tile[] viewportTiles = null;
        if (currentMap == null)
            return;
        viewportTiles = targetRealm.getTileManager().getCollisionTiles(p.getPos());
        for (final Bullet b : this.getBullets(realmId, p)) {
            if (b.remove()) {
                toRemove.add(b);
                continue;
            }
            for (final Tile tile : viewportTiles) {
                if ((tile == null) || tile.isVoid()) {
                    continue;
                }
                Rectangle tileBounds = new Rectangle(tile.getPos(), GlobalConstants.BASE_TILE_SIZE,
                        GlobalConstants.BASE_TILE_SIZE);
                Vector2f bulletPosCenter = b.getCenteredPosition();
                if (tileBounds.inside((int) bulletPosCenter.x, (int) bulletPosCenter.y)) {
                    b.setRange(0);
                    toRemove.add(b);
                }
            }
        }
        toRemove.forEach(bullet -> {
            targetRealm.getExpiredBullets().add(bullet.getId());
            targetRealm.removeBullet(bullet);
        });
    }

    private void processPlayerHit(final long realmId, final Bullet b, final Player p) {
        final Realm targetRealm = this.realms.get(realmId);
        final Player player = targetRealm.getPlayer(p.getId());
        if (player == null)
            return;
        if (b.getBounds().collides(0, 0, player.getBounds()) && b.isEnemy() && !b.isPlayerHit()) {
            final Stats stats = player.getComputedStats();
            b.setPlayerHit(true);
            final short minDmg = (short) (b.getDamage() * 0.15);
            short dmgToInflict = (short) (b.getDamage() - stats.getDef());
            if (dmgToInflict < minDmg) {
                dmgToInflict = minDmg;
            }
            this.sendTextEffectToPlayer(player, TextEffect.DAMAGE, "-" + dmgToInflict);

            player.setHealth(player.getHealth() - dmgToInflict);
            targetRealm.getExpiredBullets().add(b.getId());
            targetRealm.removeBullet(b);
            if (b.hasFlag((short) 2)) {
                if (!p.hasEffect(EffectType.PARALYZED)) {
                    this.sendTextEffectToPlayer(player, TextEffect.DAMAGE, "PARALYZED");
                    p.setDx(0);
                    p.setDy(0);
                    p.addEffect(EffectType.PARALYZED, 1500);
                }
            }

            if (b.hasFlag((short) 3)) {
                if (!p.hasEffect(EffectType.STUNNED)) {
                    this.sendTextEffectToPlayer(player, TextEffect.DAMAGE, "STUNNED");
                    p.addEffect(EffectType.STUNNED, 2500);
                }
            }

            if (p.getDeath()) {
                this.playerDeath(targetRealm, player);

            }
        }
    }

    private void proccessEnemyHit(final long realmId, final Bullet b, final Enemy e) {
        final Realm targetRealm = this.realms.get(realmId);
        final EnemyModel model = GameDataManager.ENEMIES.get(e.getEnemyId());
        if (targetRealm.hasHitEnemy(b.getId(), e.getId()) || targetRealm.getExpiredEnemies().contains(e.getId()))
            return;
        if (b.getBounds().collides(0, 0, e.getBounds()) && !b.isEnemy()) {
            final short minDmg = (short) (b.getDamage() * 0.15);
            short dmgToInflict = (short) (b.getDamage() - model.getStats().getDef());
            if (dmgToInflict < minDmg) {
                dmgToInflict = minDmg;
            }
            targetRealm.hitEnemy(b.getId(), e.getId());
            e.setHealth(e.getHealth() - dmgToInflict);
            e.setHealthpercent(e.getHealth() / model.getHealth());
            if (b.hasFlag((short) 10) && !b.isEnemyHit()) {
                b.setEnemyHit(true);
            } else if (b.remove()) {
                targetRealm.getExpiredBullets().add(b.getId());
                targetRealm.removeBullet(b);
            } else {
                targetRealm.getExpiredBullets().add(b.getId());
                targetRealm.removeBullet(b);
            }

            if (b.hasFlag((short) 2)) {
                if (!e.hasEffect(EffectType.PARALYZED)) {
                    e.addEffect(EffectType.PARALYZED, 5000);
                    this.broadcastTextEffect(EntityType.ENEMY, e, TextEffect.DAMAGE, "PARALYZED");

                }
            }

            if (b.hasFlag((short) 3)) {
                if (!e.hasEffect(EffectType.STUNNED)) {
                    e.addEffect(EffectType.STUNNED, 5000);
                    this.broadcastTextEffect(EntityType.ENEMY, e, TextEffect.DAMAGE, "STUNNED");

                }
            }
            this.broadcastTextEffect(EntityType.ENEMY, e, TextEffect.DAMAGE, "-" + dmgToInflict);
            if (e.getDeath()) {
                targetRealm.getExpiredBullets().add(b.getId());
                this.enemyDeath(targetRealm, e);
            }
        }
    }

    public void addProjectile(final long realmId, final long id, final long targetPlayerId, final int projectileId,
            final int projectileGroupId, final Vector2f src, final Vector2f dest, final short size,
            final float magnitude, final float range, short damage, final boolean isEnemy, final List<Short> flags) {
        final Realm targetRealm = this.realms.get(realmId);
        final Player player = targetRealm.getPlayer(targetPlayerId);
        if (player == null)
            return;
        final ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);

        if (!isEnemy) {
            damage = (short) (damage + player.getStats().getAtt());
        }

        final long idToUse = id == 0l ? Realm.RANDOM.nextLong() : id;
        final Bullet b = new Bullet(id, projectileId, src, dest, size, magnitude, range, damage, isEnemy);
        b.setFlags(flags);
        targetRealm.addBullet(b);
    }

    public void addProjectile(final long realmId, final long id, final long targetPlayerId, final int projectileId,
            final int projectileGroupId, final Vector2f src, final float angle, final short size, final float magnitude,
            final float range, short damage, final boolean isEnemy, final List<Short> flags, final short amplitude,
            final short frequency) {
        final Realm targetRealm = this.realms.get(realmId);
        final Player player = targetRealm.getPlayer(targetPlayerId);
        if (player == null)
            return;
        final ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
        if (!isEnemy) {
            damage = (short) (damage + player.getStats().getAtt());
        }

        final long idToUse = id == 0l ? Realm.RANDOM.nextLong() : id;
        final Bullet b = new Bullet(idToUse, projectileId, src, angle, size, magnitude, range, damage, isEnemy);

        b.setAmplitude(amplitude);
        b.setFrequency(frequency);
        b.setFlags(flags);
        targetRealm.addBullet(b);
    }

    private List<Bullet> getBullets(final long realmId, final Player p) {
        final Realm targetRealm = this.realms.get(realmId);
        final GameObject[] gameObject = targetRealm
                .getGameObjectsInBounds(targetRealm.getTileManager().getRenderViewPort(p));

        final List<Bullet> results = new ArrayList<>();
        for (int i = 0; i < gameObject.length; i++) {
            if (gameObject[i] instanceof Bullet) {
                results.add((Bullet) gameObject[i]);
            }
        }
        return results;
    }

    private void enemyDeath(final Realm targetRealm, final Enemy enemy) {
        final EnemyModel model = GameDataManager.ENEMIES.get(enemy.getEnemyId());
        try {
            for (final Player player : targetRealm
                    .getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(enemy))) {
                final int xpToGive = model.getXp() * (targetRealm.getDepth() == 0 ? 1 : targetRealm.getDepth() + 1);
                player.incrementExperience(xpToGive);
                try {
                    this.enqueueServerPacket(player, TextEffectPacket.from(EntityType.PLAYER, player.getId(),
                            TextEffect.PLAYER_INFO, xpToGive + "xp"));
                } catch (Exception ex) {
                    RealmManagerServer.log.error("Failed to create player experience text effect. Reason: {}", ex);
                }
            }

            targetRealm.getExpiredEnemies().add(enemy.getId());
            targetRealm.clearHitMap();
            if ((targetRealm.getMapId() != 5) && (targetRealm.getMapId() != 1)) {
                targetRealm.spawnRandomEnemy();
            }
            targetRealm.removeEnemy(enemy);
            // TODO: Possibly rewrite portal drops to come from loot table
            if (targetRealm.getMapId() != 5 && Realm.RANDOM.nextInt(10) < 10) {

                final PortalModel portalModel = this.getPortalToDepth(targetRealm.getDepth()+1);
                final Portal toNewRealmPortal = new Portal(Realm.RANDOM.nextLong(), (short) portalModel.getPortalId(),
                        enemy.getPos().withNoise(64, 64));
               
                Optional<Realm> realmAtDepth = this.findRealmAtDepth(portalModel.getTargetRealmDepth()-1);
                if (realmAtDepth.isEmpty()) {
                    toNewRealmPortal.linkPortal(targetRealm, null);
                    log.info("New portal created. Will generate realm id {} on use", portalModel.getTargetRealmDepth());

                } else {
                    toNewRealmPortal.linkPortal(targetRealm, realmAtDepth.get());
                    log.info("Linking Portal {} to existing realm {}", toNewRealmPortal, realmAtDepth.get());
                }
                targetRealm.addPortal(toNewRealmPortal);
            }

            // Try to get the loot model mapped by this enemyId
            final LootTableModel lootTable = GameDataManager.LOOT_TABLES.get(enemy.getEnemyId());
            if (lootTable == null) {
                log.warn("No loot table registered for enemy {}", enemy.getEnemyId());
                throw new IllegalStateException("No loot table registered for enemy " + enemy.getEnemyId());
            }
            final List<GameItem> lootToDrop = GameDataManager.LOOT_TABLES.get(enemy.getEnemyId()).getLootDrop();
            if (lootToDrop.size() > 0) {
                final LootContainer dropsBag = new LootContainer(LootTier.BLUE, enemy.getPos().withNoise(64, 64),
                        lootToDrop.toArray(new GameItem[0]));
                targetRealm.addLootContainer(dropsBag);
            }
        } catch (Exception e) {
            RealmManagerServer.log.error("Failed to handle dead Enemy {}. Reason: {}", enemy.getId(), e);
        }
    }

    private void playerDeath(final Realm targetRealm, final Player player) {
        try {
            final String remoteAddrDeath = this.getRemoteAddressMapRevered().get(player.getId());
            final LootContainer graveLoot = new LootContainer(LootTier.GRAVE, player.getPos().clone(),
                    player.getSlots(4, 12));
            // this.getServer().getClients().remove(remoteAddrDeath);
            targetRealm.addLootContainer(graveLoot);
            targetRealm.getExpiredPlayers().add(player.getId());
            targetRealm.removePlayer(player);
            if (player.isHeadless())
                return;
            this.enqueueServerPacket(player, PlayerDeathPacket.from());
            if ((player.getInventory()[3] == null) || (player.getInventory()[3].getItemId() != 48)) {
                ServerGameLogic.DATA_SERVICE.executeDelete("/data/account/character/" + player.getCharacterUuid(),
                        Object.class);
            } else {
                // Remove their amulet and let them respawn
                TextPacket toBroadcast = TextPacket.create("SYSTEM", "",
                        player.getName() + "'s Amulet shatters as they disappear.");
                this.enqueueServerPacket(toBroadcast);
                player.getInventory()[3] = null;
                this.persistPlayerAsync(player);
            }

        } catch (Exception e) {
            RealmManagerServer.log.error("Failed to Remove dead Player {}. Reason: {}", e);
        }
    }

    public void clearPlayerState(long playerId) {
        this.playerLoadState.remove(playerId);
        this.playerUpdateState.remove(playerId);
        this.playerUnloadState.remove(playerId);
        this.playerLoadMapState.remove(playerId);
        this.playerObjectMoveState.remove(playerId);
        this.playerAbilityState.remove(playerId);
    }

    public PortalModel getPortalToDepth(int targetDepth) {
        return GameDataManager.PORTALS.values().stream().filter(portal -> portal.getTargetRealmDepth() == targetDepth)
                .findAny().get();
    }

    public Map<Long, String> getRemoteAddressMapRevered() {
        final Map<Long, String> result = new HashMap<>();
        for (final Entry<String, Long> entry : this.remoteAddresses.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    // Adds a realm to the map of realms after trying to decorate
    // the realm terrain using any decorators
    public void addRealm(final Realm realm) {
        this.tryDecorate(realm);
        this.realms.put(realm.getRealmId(), realm);
    }

    public Realm findPlayerRealm(long playerId) {
        Realm found = null;
        for (Map.Entry<Long, Realm> realm : this.realms.entrySet()) {
            for (Player player : realm.getValue().getPlayers().values()) {
                if (player.getId() == playerId) {
                    found = realm.getValue();
                }
            }
        }
        return found;
    }

    public Optional<Realm> findRealmAtDepth(int depth) {
        return this.getRealms().values().stream().filter(realm -> realm.getDepth() == (depth + 1)).findAny();
    }

    public Player searchRealmsForPlayer(String playerName) {
        Player found = null;
        for (Map.Entry<Long, Realm> realm : this.realms.entrySet()) {
            for (Player player : realm.getValue().getPlayers().values()) {
                if (player.getName() != null && player.getName().equalsIgnoreCase(playerName)) {
                    found = player;
                }
            }
        }
        return found;
    }
    
    public Player searchRealmsForPlayer(long playerId) {
        Player found = null;
        for (Map.Entry<Long, Realm> realm : this.realms.entrySet()) {
            for (Player player : realm.getValue().getPlayers().values()) {
                if (player.getId()==playerId) {
                    found = player;
                }
            }
        }
        return found;
    }

    private void beginPlayerSync() {
        final Runnable playerSync = () -> {
            try {
                while (!this.shutdown) {
                    Thread.sleep(12000);
                    RealmManagerServer.log.info("Performing asynchronous player data sync.");
                    this.persistsPlayersAsync();
                }
            } catch (Exception e) {
                RealmManagerServer.log.error("Failed to perform player data sync.");
            }
        };

        // Thread syncThread = new Thread(playerSync);
        WorkerThread.submitAndForkRun(playerSync);
    }

    public Thread shutdownHook() {
        final Runnable shutdownTask = () -> {
            RealmManagerServer.log.info("Performing pre-shutdown player sync...");
            this.persistsPlayersAsync();
            RealmManagerServer.log.info("Shutdown player sync complete");
        };
        return new Thread(shutdownTask);
    }

    public void persistsPlayersAsync() {
        final Runnable persist = () -> {
            for (final Map.Entry<Long, Realm> realm : this.realms.entrySet()) {
                for (final Player player : realm.getValue().getPlayers().values()) {
                    this.persistPlayer(player);
                }
            }
        };
        WorkerThread.doAsync(persist);
    }

    public void safeRemoveRealm(final Realm realm) {
        this.safeRemoveRealm(realm.getRealmId());
    }

    public void safeRemoveRealm(final long realmId) {
        this.acquireRealmLock();
        Realm realm = this.realms.remove(realmId);
        realm.setShutdown(true);
        this.releaseRealmLock();
    }

    private void persistPlayerAsync(final Player player) {
        final Runnable persist = () -> {
            this.persistPlayer(player);
        };
        WorkerThread.doAsync(persist);
    }

    private void sendTextEffectToPlayer(final Player player, final TextEffect effect, final String text) {
        try {
            this.enqueueServerPacket(player, TextEffectPacket.from(EntityType.PLAYER, player.getId(), effect, text));
        } catch (Exception e) {
            RealmManagerServer.log.error("Failed to send TextEffect Packet to Player {}. Reason: {}", player.getId(),
                    e);
        }
    }

    private void broadcastTextEffect(final EntityType entityType, final GameObject entity, final TextEffect effect,
            final String text) {
        try {
            this.enqueueServerPacket(TextEffectPacket.from(entityType, entity.getId(), effect, text));
        } catch (Exception e) {
            RealmManagerServer.log.error("Failed to broadcast TextEffect Packet for Entity {}. Reason: {}",
                    entity.getId(), e);
        }
    }

    private boolean persistPlayer(final Player player) {
        if (player.isHeadless())
            return false;
        try {
            final PlayerAccountDto account = ServerGameLogic.DATA_SERVICE
                    .executeGet("/data/account/" + player.getAccountUuid(), null, PlayerAccountDto.class);
            final Optional<CharacterDto> currentCharacter = account.getCharacters().stream()
                    .filter(character -> character.getCharacterUuid().equals(player.getCharacterUuid())).findAny();
            if (currentCharacter.isPresent()) {
                final CharacterDto character = currentCharacter.get();
                final CharacterStatsDto newStats = player.serializeStats();
                final Set<GameItemRefDto> newItems = player.serializeItems();
                character.setItems(player.serializeItems());
                character.setStats(newStats);
                final CharacterDto savedStats = ServerGameLogic.DATA_SERVICE.executePost(
                        "/data/account/character/" + character.getCharacterUuid(), character, CharacterDto.class);
                RealmManagerServer.log.info("Succesfully persisted user account {}", account.getAccountEmail());
            }
        } catch (Exception e) {
            RealmManagerServer.log.error("Failed to get player account. Reason: {}", e);
        }
        return true;
    }

    private void broadcastPacket(final Packet packet) {
        for (final Map.Entry<String, ProcessingThread> client : this.server.getClients().entrySet()) {
            if (!client.getValue().isHandshakeComplete()) {
                continue;
            }
            try {
                final OutputStream toClientStream = client.getValue().getClientSocket().getOutputStream();
                final DataOutputStream dosToClient = new DataOutputStream(toClientStream);
            } catch (Exception e) {
                RealmManagerServer.log.error("Failed to broadcast Packet to client {}", client.getKey());
            }
        }
    }

    private void acquireRealmLock() {
        try {
            this.realmLock.acquire();
        } catch (Exception e) {
            log.error("Failed to acquire the realm lock. Reason: {}", e.getMessage());
        }
    }

    private void releaseRealmLock() {
        try {
            this.realmLock.release();
        } catch (Exception e) {
            log.error("Failed to release the realm lock. Reason: {}", e.getMessage());
        }
    }
}
