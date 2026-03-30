package com.jrealm.net.realm;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
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
import com.jrealm.game.contants.ProjectileEffectType;
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
import com.jrealm.game.entity.Monster;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.TerrainGenerationParameters;
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
import com.jrealm.game.tile.Tile;
import com.jrealm.game.tile.TileMap;
import com.jrealm.game.tile.decorators.Beach0Decorator;
import com.jrealm.game.tile.decorators.Grasslands0Decorator;
import com.jrealm.game.tile.decorators.RealmDecorator;
import com.jrealm.game.tile.decorators.RealmDecoratorBase;
import com.jrealm.net.Packet;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.PlayerDeathPacket;
import com.jrealm.net.client.packet.TextEffectPacket;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.client.packet.UpdatePacket;

import com.jrealm.net.entity.NetTile;
import com.jrealm.net.entity.NetObjectMovement;
import com.jrealm.net.messaging.ServerCommandMessage;
import com.jrealm.net.server.ClientSession;
import com.jrealm.net.server.NioServer;
import com.jrealm.net.server.ServerCommandHandler;
import com.jrealm.net.server.ServerGameLogic;
import com.jrealm.net.server.ServerTradeManager;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.MoveItemPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.net.server.packet.UseAbilityPacket;
import com.jrealm.net.server.packet.UsePortalPacket;
import com.jrealm.util.AdminRestrictedCommand;
import com.jrealm.util.CommandHandler;
import com.jrealm.util.PacketHandlerServer;
import com.jrealm.util.TimedWorkerThread;
import com.jrealm.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("unused")
public class RealmManagerServer implements Runnable {
	
	private NioServer server;
	private boolean shutdown = false;
	private Reflections classPathScanner = new Reflections("com.jrealm", Scanners.SubTypes, Scanners.MethodsAnnotated);
	private MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
	private final Map<Class<? extends Packet>, BiConsumer<RealmManagerServer, Packet>> packetCallbacksServer = new HashMap<>();
	private final Map<Byte, List<MethodHandle>> userPacketCallbacksServer = new HashMap<>();

	private List<Vector2f> shotDestQueue = new ArrayList<>();
	private Map<Long, Realm> realms = new ConcurrentHashMap<>();
	private Map<String, Long> remoteAddresses = new ConcurrentHashMap<>();
	
	private Map<Long, Long> playerAbilityState = new ConcurrentHashMap<>();
	private Map<Long, LoadPacket> playerLoadState = new ConcurrentHashMap<>();
	private Map<Long, UpdatePacket> playerUpdateState = new ConcurrentHashMap<>();
	private Map<Long, UpdatePacket> enemyUpdateState = new ConcurrentHashMap<>();
	private Map<Long, UnloadPacket> playerUnloadState = new ConcurrentHashMap<>();
	private Map<Long, LoadMapPacket> playerLoadMapState = new ConcurrentHashMap<>();
	private Map<Long, ObjectMovePacket> playerObjectMoveState = new ConcurrentHashMap<>();
	private Map<Long, Long> playerGroundDamageState = new ConcurrentHashMap<>();
	private Map<Long, Long> playerLastHeartbeatTime = new ConcurrentHashMap<>();

	// Poison damage-over-time tracking
	private final List<PoisonDotState> activePoisonDots = new java.util.ArrayList<>();

	private static class PoisonDotState {
		final long realmId;
		final long enemyId;
		final int totalDamage;
		final long duration;
		final long startTime;
		final long sourcePlayerId;
		int damageApplied;

		PoisonDotState(long realmId, long enemyId, int totalDamage, long duration, long sourcePlayerId) {
			this.realmId = realmId;
			this.enemyId = enemyId;
			this.totalDamage = totalDamage;
			this.duration = duration;
			this.startTime = java.time.Instant.now().toEpochMilli();
			this.sourcePlayerId = sourcePlayerId;
			this.damageApplied = 0;
		}

		boolean isExpired() {
			return java.time.Instant.now().toEpochMilli() - startTime >= duration;
		}
	}

	private UnloadPacket lastUnload;
	// Potentially accessed by many threads many times a second.
	// marked volatile to make sure each time this queue is accessed
	// we are not looking at a cached version. Make a PR if my assumption is wrong :)
	private volatile Queue<Packet> outboundPacketQueue = new ConcurrentLinkedQueue<>();
	private volatile Map<Long, ConcurrentLinkedQueue<Packet>> playerOutboundPacketQueue = new ConcurrentHashMap<Long, ConcurrentLinkedQueue<Packet>>();
	private List<RealmDecoratorBase> realmDecorators = new ArrayList<>();
	private List<EnemyScriptBase> enemyScripts = new ArrayList<>();
	private List<UseableItemScriptBase> itemScripts = new ArrayList<>();
	private Semaphore realmLock = new Semaphore(1);
	private int currentTickCount = 0;
	private long tickSampleTime = 0;

	// Tiered update rate tick counter (increments every tick, wraps at 64)
	private int tickCounter = 0;

	// Tick rate divisors for tiered packet transmission (at 64 ticks/sec):
	// Two-tier movement: inner zone (50% radius) at 32Hz, full viewport at 16Hz.
	// This halves entity count on most ticks since screen edges are less important.
	// LoadPacket: 16Hz - entity spawns/despawns aren't time-critical
	// UpdatePacket: 16Hz - stats/inventory/effects change slowly
	// LoadMapPacket: 4Hz - terrain barely changes
	// EnemyUpdatePacket: 16Hz - enemy health bars
	private static final int MOVE_TICK_DIVISOR = 1;      // Inner zone movement at 64Hz (matches Java client)
	private static final int MOVE_FULL_TICK_DIVISOR = 2;  // Full viewport movement at 32Hz
	private static final int LOAD_TICK_DIVISOR = 2;       // Entity spawn/despawn at 32Hz (loot needs fast sync)
	private static final int UPDATE_TICK_DIVISOR = 4;
	private static final int LOADMAP_TICK_DIVISOR = 16;
	private static final int ENEMY_UPDATE_TICK_DIVISOR = 4;

	private boolean  isSetup = false;
	
	private long lastWriteSampleTime = Instant.now().toEpochMilli();
	private long bytesWritten = 0;
	private Map<String, Long> bytesWrittenByPacketType = new HashMap<>();
	
	public RealmManagerServer() {
		// Probably dont want to auto start the server so migrating
		// this to be invoked from somewhere else (GameLauncher.class)
//		this.doRunServer();
	}
	
	public void doRunServer() {
		// TODO: Make the trade manager a class variable so we dont
		// have to do this whacky static assignment
		ServerTradeManager.mgr = this;
		
		// Spawn initial realm and add the global
		// save player shutdown hook to the Runtime
		this.doSetup();
		
		// Two core threads, the inbound connection listener
		// and the actual realm manager thread to handle game processing
		WorkerThread.submitAndForkRun(this.server);
		WorkerThread.submitAndForkRun(this);
	}
	
	private void doSetup() {
		if(this.isSetup) {
			log.warn("[SERVER] Server is already setup, ignoring extra call");
			return;
		}
		// Start listening for connections
		this.server = new NioServer(2222);

		// Start WebSocket server for browser-based clients
		try {
			final com.jrealm.net.server.WebSocketGameServer wsServer =
				new com.jrealm.net.server.WebSocketGameServer(2223, this.server);
			wsServer.start();
			log.info("[SERVER] WebSocket server started on port 2223");
		} catch (Exception e) {
			log.error("[SERVER] Failed to start WebSocket server: {}", e.getMessage());
		}

		this.registerRealmDecorators();
		this.registerEnemyScripts();
		this.registerPacketCallbacks();
		this.registerPacketCallbacksReflection();
		this.registerItemScripts();
		this.registerCommandHandlersReflection();
		this.beginPlayerSync();
		
		final com.jrealm.game.model.DungeonGraphNode entryNode = GameDataManager.getEntryNode();
		final Realm realm;
		if (entryNode != null) {
			realm = new Realm(true, entryNode.getMapId(), 0, entryNode.getNodeId());
			RealmManagerServer.log.info("[SERVER] Starting realm at graph node: {} ({})", entryNode.getNodeId(), entryNode.getDisplayName());
		} else {
			realm = new Realm(true, 2);
			RealmManagerServer.log.warn("[SERVER] No dungeon graph entry node found, falling back to mapId=2");
		}
		realm.spawnRandomEnemies(realm.getMapId());

		// Initialize Overseer AI for the main realm (ecosystem management, boss events, taunts)
		realm.setOverseer(new RealmOverseer(realm, this));

		// Place set piece structures (ruins, graveyards, etc.) after terrain generation
		TerrainGenerationParameters terrainParams = null;
		if (GameDataManager.TERRAINS != null) {
			// Try to get terrain params from the map's terrainId
			final var mapModel = GameDataManager.MAPS.get(realm.getMapId());
			if (mapModel != null && mapModel.getTerrainId() >= 0) {
				terrainParams = GameDataManager.TERRAINS.get(mapModel.getTerrainId());
			}
			// Fallback to terrain 0 (overworld)
			if (terrainParams == null) {
				terrainParams = GameDataManager.TERRAINS.get(0);
			}
		}
		if (terrainParams != null && terrainParams.getSetPieces() != null) {
			log.info("[SERVER] Placing set pieces for terrain '{}' ({} types defined)",
				terrainParams.getName(), terrainParams.getSetPieces().size());
			realm.placeSetPieces(terrainParams);
		} else {
			log.info("[SERVER] No set pieces to place (terrainParams={}, setPieces={})",
				terrainParams != null ? terrainParams.getName() : "null",
				terrainParams != null && terrainParams.getSetPieces() != null ? terrainParams.getSetPieces().size() : "null");
		}
		this.addRealm(realm);
		
		Runtime.getRuntime().addShutdownHook(this.shutdownHook());
		
		this.isSetup = true;
	}

	// Adds a specified amount of random headless players
	public void spawnTestPlayers(final long realmId, final int count, final Vector2f pos) {
		final Realm targetRealm = this.realms.get(realmId);
		final Runnable spawnTestPlayers = () -> {
			final Random random = Realm.RANDOM;
			for (int i = 0; i < count; i++) {
				final CharacterClass classToSpawn = CharacterClass.getCharacterClasses()
						.get(random.nextInt(CharacterClass.getCharacterClasses().size()));
				try {
					final Vector2f spawnPos = pos.clone(50, 50);
					final Player player = new Player(Realm.RANDOM.nextLong(), spawnPos, GlobalConstants.PLAYER_SIZE,
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
						// player.setUp(true);
						player.setDy(-random.nextFloat());
					} else {
						// player.setDown(true);
						player.setDy(random.nextFloat());
					}
					if (right) {
						// player.setRight(true);
						player.setDx(random.nextFloat());
					} else {
						// player.setLeft(true);
						player.setDx(-random.nextFloat());
					}
					Thread.sleep(100);
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
		RealmManagerServer.log.info("[SERVER] Starting JRealm Server");
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
			this.processServerPackets();
			this.enqueueGameData();
			this.sendGameData();

			// Tick all realm overseers (ecosystem management)
			for (Realm realm : this.realms.values()) {
				if (realm.getOverseer() != null) {
					realm.getOverseer().tick();
				}
			}

			if (Instant.now().toEpochMilli() - this.tickSampleTime > 1000) {
				this.tickSampleTime = Instant.now().toEpochMilli();
				log.info("[SERVER] ticks this second: {}", this.currentTickCount);
				this.currentTickCount = 0;
			} else {
				this.currentTickCount++;
			}
		} catch (Exception e) {
			RealmManagerServer.log.error("Failed to process server tick. Reason: {}", e);
		}
	}



	private void sendGameData() {
		long startNanos = System.nanoTime();
		final List<Packet> packetsToBroadcast = new ArrayList<>();
		// TODO: Possibly rework this queue as we dont usually send stuff globally
		while (!this.outboundPacketQueue.isEmpty()) {
			packetsToBroadcast.add(this.outboundPacketQueue.remove());
		}

		// Detect stale sessions
		final List<Map.Entry<String, ClientSession>> staleSessions = new ArrayList<>();
		for (final Map.Entry<String, ClientSession> client : this.server.getClients().entrySet()) {
			if (!client.getValue().isConnected() || client.getValue().isShutdownProcessing()) {
				staleSessions.add(client);
			}
		}
		staleSessions.forEach(entry -> {
			try {
				entry.getValue().setShutdownProcessing(true);
				// Remove the player from the realm before removing the client session
				final Long dcPlayerId = this.remoteAddresses.get(entry.getKey());
				if (dcPlayerId != null) {
					final Realm playerRealm = this.findPlayerRealm(dcPlayerId);
					if (playerRealm != null) {
						final Player dcPlayer = playerRealm.getPlayer(dcPlayerId);
						if (dcPlayer != null) {
							this.persistPlayerAsync(dcPlayer);
							playerRealm.getExpiredPlayers().add(dcPlayerId);
							playerRealm.removePlayer(dcPlayer);
							log.info("[SERVER] Removed disconnected player {} from realm", dcPlayer.getName());
						}
					}
					this.clearPlayerState(dcPlayerId);
					this.remoteAddresses.remove(entry.getKey());
				}
				entry.getValue().close();
				this.server.getClients().remove(entry.getKey());
			} catch (Exception e) {
				log.error("[SERVER] Failed to remove stale session. Reason:  {}", e);
			}
		});

		// Pre-serialize broadcast packets once
		final List<byte[]> broadcastFrames = new ArrayList<>();
		for (final Packet packet : packetsToBroadcast) {
			try {
				final byte[] frame = packet.serializeToBytes();
				broadcastFrames.add(frame);
				this.bytesWritten += frame.length;
			} catch (Exception e) {
				log.error("[SERVER] Failed to serialize broadcast packet. Reason: {}", e);
			}
		}

		for (final Map.Entry<String, ClientSession> client : this.server.getClients().entrySet()) {
			try {
				final ClientSession session = client.getValue();
				final Player player = this.getPlayerByRemoteAddress(client.getKey());
				if (player == null) {
					log.debug("[SERVER] Player {} has not yet completed login, skipping broadcast", client.getKey());
					continue;
				}

				// Enqueue broadcast frames (already serialized)
				for (final byte[] frame : broadcastFrames) {
					session.enqueueWrite(frame);
				}

				// Dequeue and send any player specific packets
				final ConcurrentLinkedQueue<Packet> playerPacketsToSend = this.playerOutboundPacketQueue
						.get(player.getId());

				while ((playerPacketsToSend != null) && !playerPacketsToSend.isEmpty()) {
					final Packet packet = playerPacketsToSend.remove();
					try {
						final byte[] frame = packet.serializeToBytes();
						session.enqueueWrite(frame);
						this.bytesWritten += frame.length;
						this.bytesWrittenByPacketType.merge(packet.getClass().getSimpleName(), (long) frame.length, Long::sum);
					} catch (Exception e) {
						log.error("[SERVER] Failed to serialize player packet. Reason: {}", e);
					}
				}
			} catch (Exception e) {
				//RealmManagerServer.log.error("[SERVER] Failed to enqueue data to Client. Reason: {}", e);
			}
		}

		// Print server write rate to all connected clients (kbit/s)
		if (Instant.now().toEpochMilli() - this.lastWriteSampleTime > 1000) {
			this.lastWriteSampleTime = Instant.now().toEpochMilli();
			RealmManagerServer.log.info("[SERVER] current write rate = {} kbit/s",
					(float) (this.bytesWritten / 1024.0f) * 8.0f);
			// Log per-packet-type bandwidth breakdown
			final StringBuilder sb = new StringBuilder("[SERVER] Bandwidth by packet type: ");
			for (Map.Entry<String, Long> entry : this.bytesWrittenByPacketType.entrySet()) {
				sb.append(entry.getKey()).append("=")
				  .append(String.format("%.1f", (entry.getValue() / 1024.0f) * 8.0f))
				  .append("kbit/s ");
			}
			RealmManagerServer.log.info(sb.toString());
			this.bytesWrittenByPacketType.clear();
			this.bytesWritten = 0;
		}
		long nanosDiff = System.nanoTime() - startNanos;
		log.debug("Game data broadcast in {} nanos ({}ms}", nanosDiff, ((double) nanosDiff / (double) 1000000l));
	}

	// Enqueues outbound game packets every tick using:
	// - Spatial hash grid for O(1) neighbor lookups
	// - Tiered update rates (movement=64Hz, load=32Hz, update=16Hz, map=4Hz)
	// - Per-cell packet sharing (players in same cell share entity queries)
	public void enqueueGameData() {
		try {
			long startNanos = System.nanoTime();
			this.acquireRealmLock();
			this.tickCounter++;

			final boolean doMovement = (this.tickCounter % MOVE_TICK_DIVISOR) == 0;
			final boolean doFullMovement = (this.tickCounter % MOVE_FULL_TICK_DIVISOR) == 0;
			final boolean doLoad = (this.tickCounter % LOAD_TICK_DIVISOR) == 0;
			final boolean doUpdate = (this.tickCounter % UPDATE_TICK_DIVISOR) == 0;
			final boolean doLoadMap = (this.tickCounter % LOADMAP_TICK_DIVISOR) == 0;
			final boolean doEnemyUpdate = (this.tickCounter % ENEMY_UPDATE_TICK_DIVISOR) == 0;

			for (final Map.Entry<Long, Realm> realmEntry : this.realms.entrySet()) {
				Realm realm = realmEntry.getValue();

				// Update spatial grid positions once per tick for this realm
				realm.updateSpatialGrid();

				// Per-cell cache: share expensive LoadPacket/ObjectMovePacket between
				// players in the same spatial cell (they see ~the same entities)
				final Map<Long, LoadPacket> cellLoadCache = new HashMap<>();
				final Map<Long, ObjectMovePacket> cellMoveCache = new HashMap<>();

				final List<Player> toRemove = new ArrayList<>();
				final float viewportRadius = 10 * com.jrealm.game.contants.GlobalConstants.BASE_TILE_SIZE;

				for (final Map.Entry<Long, Player> player : realm.getPlayers().entrySet()) {
					if (player.getValue().isHeadless()) {
						continue;
					}
					try {
						realm = this.findPlayerRealm(player.getKey());
						final Vector2f playerCenter = player.getValue().getPos();
						final long cellKey = realm.getSpatialCellKey(playerCenter.x, playerCenter.y);

						// --- LoadMapPacket (4 Hz) ---
						if (doLoadMap) {
							final NetTile[] netTilesForPlayer = realm.getTileManager().getLoadMapTiles(player.getValue());
							final LoadMapPacket newLoadMapPacket = LoadMapPacket.from(realm.getRealmId(),
									(short) realm.getMapId(), realm.getTileManager().getMapWidth(),
									realm.getTileManager().getMapHeight(), netTilesForPlayer);
							if (this.playerLoadMapState.get(player.getKey()) == null) {
								this.playerLoadMapState.put(player.getKey(), newLoadMapPacket);
								this.enqueueServerPacket(player.getValue(), newLoadMapPacket);
							} else {
								final LoadMapPacket oldLoadMapPacket = this.playerLoadMapState.get(player.getKey());
								if (!oldLoadMapPacket.equals(newLoadMapPacket)) {
									final LoadMapPacket loadMapDiff = oldLoadMapPacket.difference(newLoadMapPacket);
									this.playerLoadMapState.put(player.getKey(), newLoadMapPacket);
									if (loadMapDiff != null) {
										this.enqueueServerPacket(player.getValue(), loadMapDiff);
									}
								}
							}
						}

						// --- Self UpdatePacket ---
						// Full rate (16Hz) for real changes (inventory, effects, stats, XP).
						// Throttled (4Hz) for HP/MP-only changes (regen tick noise).
						if (doUpdate) {
							final UpdatePacket updatePacket = realm.getPlayerAsPacket(player.getValue().getId());
							final UpdatePacket oldUpdate = this.playerUpdateState.get(player.getKey());
							if (oldUpdate == null) {
								this.playerUpdateState.put(player.getKey(), updatePacket);
								this.enqueueServerPacket(player.getValue(), updatePacket);
							} else if (!oldUpdate.equals(updatePacket, false)) {
								// Check if only stats/hp/mp changed (no inventory, effects, xp)
								boolean onlyStats = oldUpdate.getExperience() == updatePacket.getExperience()
									&& !updatePacket.inventoryChanged(oldUpdate)
									&& java.util.Arrays.equals(oldUpdate.getEffectIds(), updatePacket.getEffectIds());
								// Stats-only changes: throttle to 4Hz (every 16th tick)
								if (onlyStats && (this.tickCounter % 16) != 0) {
									// Skip this tick — will send on next 4Hz boundary
								} else {
									this.playerUpdateState.put(player.getKey(), updatePacket);
									this.enqueueServerPacket(player.getValue(), updatePacket);
								}
							}

							// Nearby other players' UpdatePackets (uses spatial grid)
							final Player[] otherPlayers = realm.getPlayersInRadiusFast(playerCenter, viewportRadius);
							for (Player other : otherPlayers) {
								if (other.getId() == player.getKey()) continue;
								try {
									final UpdatePacket otherUpdate = realm.getPlayerAsPacket(other.getId());
									if (otherUpdate != null) {
										final UpdatePacket cachedOther = this.playerUpdateState.get(other.getId());
										if (cachedOther == null || !cachedOther.equals(otherUpdate, false)) {
											this.playerUpdateState.put(other.getId(), otherUpdate);
											this.enqueueServerPacket(player.getValue(), otherUpdate);
										}
									}
								} catch (Exception ex) {
									log.error("[SERVER] Failed to build other player UpdatePacket. Reason: {}", ex);
								}
							}
						}

						// --- LoadPacket (32 Hz) - uses per-cell cache + spatial grid ---
						if (doLoad) {
							// Reuse LoadPacket if another player in the same cell already built it
							LoadPacket loadPacket = cellLoadCache.get(cellKey);
							if (loadPacket == null) {
								loadPacket = realm.getLoadPacketCircularFast(playerCenter, viewportRadius);
								cellLoadCache.put(cellKey, loadPacket);
							}
							if (this.playerLoadState.get(player.getKey()) == null) {
								this.playerLoadState.put(player.getKey(), loadPacket);
								this.enqueueServerPacket(player.getValue(), loadPacket);
							} else {
								final LoadPacket oldLoad = this.playerLoadState.get(player.getKey());
								if (!oldLoad.equals(loadPacket)) {
									final LoadPacket toSend = oldLoad.combine(loadPacket);
									this.playerLoadState.put(player.getKey(), loadPacket);
									if (!toSend.isEmpty()) {
										this.enqueueServerPacket(player.getValue(), toSend);
									}
									final UnloadPacket unloadDelta = oldLoad.difference(loadPacket);
									if (unloadDelta.isNotEmpty()) {
										this.enqueueServerPacket(player.getValue(), unloadDelta);
										for (long unloadedEnemy : unloadDelta.getEnemies()) {
											this.enemyUpdateState.remove(unloadedEnemy);
										}
									}
								}
							}
						}

						// --- ObjectMovePacket: inner zone at 32Hz, full viewport at 16Hz ---
						if (doMovement) {
							// Two-tier: use 50% radius on inner-only ticks, full radius on full ticks
							final float moveRadius = doFullMovement ? viewportRadius : viewportRadius * 0.5f;
							// Cache key includes radius tier to avoid mixing
							final long moveCacheKey = doFullMovement ? cellKey : (cellKey ^ 0xDEADBEEFL);
							ObjectMovePacket movePacket = cellMoveCache.get(moveCacheKey);
							if (movePacket == null) {
								movePacket = realm.getGameObjectsAsPacketsCircularFast(playerCenter, moveRadius);
								cellMoveCache.put(moveCacheKey, movePacket);
							}
							if (this.playerObjectMoveState.get(player.getKey()) == null && movePacket != null) {
								this.playerObjectMoveState.put(player.getKey(), movePacket);
								this.enqueueServerPacket(player.getValue(), movePacket);
							} else if (movePacket != null) {
								final ObjectMovePacket oldMove = this.playerObjectMoveState.get(player.getKey());
								if (oldMove != null) {
									final ObjectMovePacket moveDiff = oldMove.getMoveDiff(movePacket);
									if (moveDiff != null) {
										this.playerObjectMoveState.put(player.getKey(), movePacket);
										this.enqueueServerPacket(player.getValue(), moveDiff);
									}
								}
							}

							// Enemy UpdatePackets (16 Hz) - extract enemy IDs from move packet
							if (doEnemyUpdate && movePacket != null) {
								final Set<Long> nearEnemyIds = new HashSet<>();
								for (NetObjectMovement m : movePacket.getMovements()) {
									if (m.getEntityType() == EntityType.ENEMY.getEntityTypeId()) {
										nearEnemyIds.add(m.getEntityId());
									}
								}
								for (Long enemyId : nearEnemyIds) {
									final UpdatePacket updatePacket0 = realm.getEnemyAsPacket(enemyId);
									final UpdatePacket oldState = this.enemyUpdateState.get(enemyId);
									boolean doSend = false;
									if (oldState == null) {
										this.enemyUpdateState.put(enemyId, updatePacket0);
										doSend = true;
									} else if (!oldState.equals(updatePacket0, true)) {
										this.enemyUpdateState.put(enemyId, updatePacket0);
										doSend = true;
									}
									if (doSend) {
										this.enqueueServerPacket(player.getValue(), updatePacket0);
									}
								}
							}
						}

						// Heartbeat timeout check (every tick)
						final Long playerLastHeartbeatTime = this.playerLastHeartbeatTime.get(player.getKey());
						if (playerLastHeartbeatTime != null
								&& ((Instant.now().toEpochMilli() - playerLastHeartbeatTime) > 5000)) {
							toRemove.add(player.getValue());
						}

					} catch (Exception e) {
						RealmManagerServer.log.error("[SERVER] Failed to build game data for Player {}. Reason: {}",
								player.getKey(), e);
					}
				}

				for (Player player : toRemove) {
					this.disconnectPlayer(player);
				}

				// Reset contentsChanged AFTER all players processed
				// (was inside player loop before — caused Player B to miss updates)
				for (LootContainer lc : realm.getLoot().values()) {
					lc.setContentsChanged(false);
				}
			}

			long nanosDiff = System.nanoTime() - startNanos;
			log.debug("[SERVER] Game data enqueued in {} nanos ({}ms)", nanosDiff,
					((double) nanosDiff / (double) 1000000l));
			this.releaseRealmLock();
		} catch (Exception e) {
			log.error("[SERVER] Failed to enqueue game data. Reason: {}", e);
		}
	}

	// For each connected client, dequeue all pending packets
	// pass the packet and RealmManager context to the handler
	// script
	public void processServerPackets() {
		for (final Map.Entry<String, ClientSession> entry : this.getServer().getClients().entrySet()) {
			if (!entry.getValue().isShutdownProcessing()) {
				// Read all packets from the ClientSession queue
				while (!entry.getValue().getPacketQueue().isEmpty()) {
					final Packet packet = entry.getValue().getPacketQueue().remove();
					try {
						Packet created = packet;
						created.setSrcIp(packet.getSrcIp());
						// Invoke packet callback
						final List<MethodHandle> packetHandles = this.userPacketCallbacksServer.get(packet.getId());
						long start = System.nanoTime();
						if (packetHandles != null) {
							for (MethodHandle handler : packetHandles) {
								try {
									handler.invokeExact(this, created);
								} catch (Throwable e) {
									log.error("[SERVER] Failed to invoke packet callback. Reason: {}", e);
								}
							}
							log.info("[SERVER] Invoked {} packet callbacks for PacketType {} using reflection in {} nanos",
									packetHandles.size(), PacketType.valueOf(created.getId()),
									(System.nanoTime() - start));
						}
						start = System.nanoTime();
						if (this.packetCallbacksServer.get(created.getClass()) == null) {
							final List<MethodHandle> callBacHandles = this.userPacketCallbacksServer.get(created.getId());
							if (callBacHandles != null) {
								callBacHandles.forEach(callBack -> {
									try {
										callBack.invokeExact(this, created);
									} catch (Throwable e) {
										log.error(
												"[SERVER] Failed to invoke user server packet callback for packet id {}. Callback: {}. Reason: {}",
												created.getId(), callBack, e.getMessage());
									}
								});
							}
						} else {
							this.packetCallbacksServer.get(created.getClass()).accept(this, created);
						}
						log.debug("[SERVER] Invoked callback for PacketType {} using map in {} nanos",
								PacketType.valueOf(created.getId()), (System.nanoTime() - start));
					} catch (Exception e) {
						RealmManagerServer.log.error("Failed to process server packets {}", e);
						entry.getValue().setShutdownProcessing(true);
					}
				}
			} else {
				// Player Disconnect routine
				final Long dcPlayerId = this.getRemoteAddresses().get(entry.getKey());
				if (dcPlayerId == null) {
					entry.getValue().setShutdownProcessing(true);
					return;
				}
				final Realm playerLocation = this.findPlayerRealm(dcPlayerId);
				if (playerLocation != null) {
					final Player dcPlayer = playerLocation.getPlayer(dcPlayerId);
					this.persistPlayerAsync(dcPlayer);
					playerLocation.getExpiredPlayers().add(dcPlayerId);
					playerLocation.getPlayers().remove(dcPlayerId);
				}
				entry.getValue().close();
				this.server.getClients().remove(entry.getKey());
			}
		}
	}

	public Map.Entry<String, ClientSession> getPlayerSessionEntry(Player player) {
		Map.Entry<String, ClientSession> result = null;
		for (final Map.Entry<String, ClientSession> client : this.server.getClients().entrySet()) {
			if (this.remoteAddresses.get(client.getKey()) == player.getId()) {
				result = client;
			}
		}
		return result;
	}

	public Player getPlayerByRemoteAddress(String remoteAddr) {
		final Long playerId = this.remoteAddresses.get(remoteAddr);
		if (playerId == null) {
			return null;
		}
		final Player found = this.searchRealmsForPlayer(playerId);
		return found;
	}

	public ClientSession getPlayerSession(Player player) {
		return getPlayerSessionEntry(player).getValue();
	}

	public String getPlayerRemoteAddress(Player player) {
		return getPlayerSessionEntry(player).getKey();
	}

	public void disconnectPlayer(Player player) {
		try {
			log.info("[SERVER] Disconnecting Player {}", player.getName());
			final Realm playerRealm = this.findPlayerRealm(player.getId());
			if (playerRealm != null) {
				playerRealm.getExpiredPlayers().add(player.getId());
				playerRealm.removePlayer(player);
			}
			this.clearPlayerState(player.getId());
			final Map.Entry<String, ClientSession> sessionEntry = this.getPlayerSessionEntry(player);
			if (sessionEntry != null) {
				sessionEntry.getValue().setShutdownProcessing(true);
				sessionEntry.getValue().close();
				this.server.getClients().remove(sessionEntry.getKey());
				this.remoteAddresses.remove(sessionEntry.getKey());
			}
		} catch (Exception e) {
			log.error("[SERVER] Failed to disconnect player. Reason:  {}", e);
		}
	}

	public Realm getTopRealm() {
		Realm result = null;
		for (final Realm realm : this.realms.values()) {
			if (realm.getDepth() == 0 && realm.getMapId() != 1) {
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
			if (itemScript.handles(itemId)) {
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
		final Set<Class<? extends RealmDecoratorBase>> subclasses = this.classPathScanner
				.getSubTypesOf(RealmDecoratorBase.class);
		for (Class<? extends RealmDecoratorBase> clazz : subclasses) {
			try {
				final RealmDecoratorBase realmDecoratorInstance = clazz.getDeclaredConstructor(RealmManagerServer.class)
						.newInstance(this);
				this.realmDecorators.add(realmDecoratorInstance);
			} catch (Exception e) {
				log.error("[SERVER] Failed to register realm decorator for script {}. Reason: {}", clazz, e.getMessage());
			}
		}
	}

	private void registerEnemyScripts() {
		this.registerEnemyScriptsReflection();
	}

	private void registerEnemyScriptsReflection() {
		final Set<Class<? extends EnemyScriptBase>> subclasses = this.classPathScanner
				.getSubTypesOf(EnemyScriptBase.class);
		for (Class<? extends EnemyScriptBase> clazz : subclasses) {
			try {
				final EnemyScriptBase realmDecoratorInstance = clazz.getDeclaredConstructor(RealmManagerServer.class)
						.newInstance(this);
				this.enemyScripts.add(realmDecoratorInstance);
			} catch (Exception e) {
				log.error("[SERVER] Failed to register enemy script for script {}. Reason: {}", clazz, e.getMessage());
			}
		}
	}

	private void registerItemScripts() {
		this.registerItemScriptsReflection();
	}

	private void registerItemScriptsReflection() {
		final Set<Class<? extends UseableItemScriptBase>> subclasses = this.classPathScanner
				.getSubTypesOf(UseableItemScriptBase.class);
		for (Class<? extends UseableItemScriptBase> clazz : subclasses) {
			try {
				final UseableItemScriptBase realmDecoratorInstance = clazz
						.getDeclaredConstructor(RealmManagerServer.class).newInstance(this);
				this.itemScripts.add(realmDecoratorInstance);
			} catch (Exception e) {
				log.error("[SERVER] Failed to register useable item script for script {}. Reason: {}", clazz, e.getMessage());
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
				final AdminRestrictedCommand isAdminRestricted = method
						.getDeclaredAnnotation(AdminRestrictedCommand.class);

				// Find the static method with given name in the target class
				MethodHandle handlerMethod = null;
				try {
					handlerMethod = this.publicLookup.findStatic(ServerCommandHandler.class, method.getName(), mt);
				} catch (Exception e) {
					handlerMethod = this.publicLookup.findStatic(ServerTradeManager.class, method.getName(), mt);
				}
				if (handlerMethod != null) {
					ServerCommandHandler.COMMAND_CALLBACKS.put(commandToHandle.value(), handlerMethod);
					ServerCommandHandler.COMMAND_DESCRIPTIONS.put(commandToHandle.value(), commandToHandle);
					log.info("[SERVER] Registered Command handler in {}. Method: {}{}", method.getDeclaringClass(),
							method.getName(), mt.toString());
					if (isAdminRestricted != null) {
						ServerCommandHandler.ADMIN_RESTRICTED_COMMANDS.add(commandToHandle.value());
						log.info("[SERVER] Command {} registered as Admin Restricted", commandToHandle.value());
					}
				}
			} catch (Exception e) {
				log.error("[SERVER] Failed to get MethodHandle to method {}. Reason: {}", method.getName(), e);
			}
		}
	}

	// Registers any user defined packet callbacks with the server
	private void registerPacketCallbacksReflection() {
		log.info("[SERVER] Registering packet handlers using reflection");
		final MethodType mt = MethodType.methodType(void.class, RealmManagerServer.class, Packet.class);

		final Set<Method> subclasses = this.classPathScanner.getMethodsAnnotatedWith(PacketHandlerServer.class);
		for (final Method method : subclasses) {
			try {
				final PacketHandlerServer packetToHandle = method.getDeclaredAnnotation(PacketHandlerServer.class);
				MethodHandle handleToHandler = null;
				try {
					handleToHandler = this.publicLookup.findStatic(ServerGameLogic.class, method.getName(), mt);
				} catch (Exception e) {
					handleToHandler = this.publicLookup.findStatic(ServerTradeManager.class, method.getName(), mt);
				}

				if (handleToHandler != null) {
					final Entry<Byte, Class<? extends Packet>> targetPacketType = PacketType.valueOf(packetToHandle.value());
					List<MethodHandle> existing = this.userPacketCallbacksServer.get(targetPacketType.getKey());
					if (existing == null) {
						existing = new ArrayList<>();
					}
					existing.add(handleToHandler);
					log.info("[SERVER] Added new packet handler for packet {}. Handler method: {}", targetPacketType,
							handleToHandler.toString());
					this.userPacketCallbacksServer.put(targetPacketType.getKey(), existing);
				}
			} catch (Exception e) {
				log.error("[SERVER] Failed to get MethodHandle to method {}. Reason: {}", method.getName(), e);
			}
		}
	}

	// For packet callbacks requiring high performance we will invoke them in a
	// functional manner using hashmap to store the references.
	// The server operator is encouraged to add auxiliary packet handling
	// functionality using the @PacketHandler annotation
	private void registerPacketCallbacks() {
		this.registerPacketCallback(PlayerMovePacket.class, ServerGameLogic::handlePlayerMoveServer);
		this.registerPacketCallback(PlayerShootPacket.class, ServerGameLogic::handlePlayerShootServer);
		this.registerPacketCallback(HeartbeatPacket.class, ServerGameLogic::handleHeartbeatServer);
		this.registerPacketCallback(TextPacket.class, ServerGameLogic::handleTextServer);
		this.registerPacketCallback(CommandPacket.class, ServerGameLogic::handleCommandServer);
		// this.registerPacketCallback(PacketType.LOAD_MAP.getPacketId(),
		// ServerGameLogic::handleLoadMapServer);
		this.registerPacketCallback(UseAbilityPacket.class, ServerGameLogic::handleUseAbilityServer);
		this.registerPacketCallback(MoveItemPacket.class, ServerGameLogic::handleMoveItemServer);
		this.registerPacketCallback(UsePortalPacket.class, ServerGameLogic::handleUsePortalServer);
	}

	private void registerPacketCallback(final Class<? extends Packet> packetId, final BiConsumer<RealmManagerServer, Packet> callback) {
		this.packetCallbacksServer.put(packetId, callback);
	}

	// Updates all game objects on the server
	public void update(double time) {
		// For each world on the server
		for (final Map.Entry<Long, Realm> realmEntry : this.realms.entrySet()) {
			final Realm realm = realmEntry.getValue();
			// Update player specific game objects — run inline, these are fast per-player ops
			for (final Map.Entry<Long, Player> player : realm.getPlayers().entrySet()) {
				final Player p = realm.getPlayer(player.getValue().getId());
				if (p == null) {
					continue;
				}
				this.processBulletHit(realm.getRealmId(), p);
				p.update(time);
				p.removeExpiredEffects();
				this.movePlayer(realm.getRealmId(), p);
			}
			// Once per tick update all non player game objects (bullets, enemies)
			final GameObject[] gameObject = realm.getAllGameObjects();
			for (int i = 0; i < gameObject.length; i++) {
				if (gameObject[i] instanceof Enemy || gameObject[i] instanceof Monster) {
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
		}

		this.removeExpiredBullets();
		this.removeExpiredLootContainers();
		this.removeExpiredPortals();
		this.processPoisonDots();
	}

	private void movePlayer(final long realmId, final Player p) {
		// If the player is paralyzed, stop them and return.
        if (p.hasEffect(ProjectileEffectType.PARALYZED)) {
            p.setUp(false);
            p.setDown(false);
            p.setRight(false);
            p.setLeft(false);
            return;
        }

		final Realm targetRealm = this.realms.get(realmId);
		float dxToUse = p.getDx();
		float dyToUse = p.getDy();

		// if the player has the 'speedy' status effect (1.5x dex, spd)
		if (p.hasEffect(ProjectileEffectType.SPEEDY)) {
			dxToUse = dxToUse * 1.5f;
			dyToUse = dyToUse * 1.5f;
		}

		final float slow = targetRealm.getTileManager().collidesSlowTile(p) ? 3.0f : 1.0f;
		final float effDx = dxToUse / slow;
		final float effDy = dyToUse / slow;

		// Save original position BEFORE any axis movement so both checks
		// use the pre-move position. This prevents diagonal wall clipping
		// where X movement shifts the player into Y collision range.
		final float origX = p.getPos().x;
		final float origY = p.getPos().y;

		// Check X collision from original position
		boolean xBlocked = targetRealm.getTileManager().collisionTile(p, effDx, 0)
				|| targetRealm.getTileManager().collidesXLimit(p, effDx)
				|| targetRealm.getTileManager().isVoidTile(p.getPos().clone(p.getSize() / 2, p.getSize() / 2), effDx, 0);

		// Check Y collision from original position (not the X-moved position)
		boolean yBlocked = targetRealm.getTileManager().collisionTile(p, 0, effDy)
				|| targetRealm.getTileManager().collidesYLimit(p, effDy)
				|| targetRealm.getTileManager().isVoidTile(p.getPos().clone(p.getSize() / 2, p.getSize() / 2), 0, effDy);

		// If both axes are free, also check the diagonal to prevent corner cutting
		if (!xBlocked && !yBlocked && effDx != 0 && effDy != 0) {
			boolean diagBlocked = targetRealm.getTileManager().collisionTile(p, effDx, effDy)
					|| targetRealm.getTileManager().isVoidTile(p.getPos().clone(p.getSize() / 2, p.getSize() / 2), effDx, effDy);
			if (diagBlocked) {
				// Block the lesser axis to prevent corner cutting
				if (Math.abs(effDx) >= Math.abs(effDy)) yBlocked = true;
				else xBlocked = true;
			}
		}

		if (!xBlocked) {
			p.xCol = false;
			p.getPos().x = origX + effDx;
		} else {
			p.xCol = true;
		}

		if (!yBlocked) {
			p.yCol = false;
			p.getPos().y = origY + effDy;
		} else {
			p.yCol = true;
		}

		// Calculate if we should apply ground damage
		if (targetRealm.getTileManager().collidesDamagingTile(p)) {
			final Long lastDamageTime = this.playerGroundDamageState.get(p.getId());
			if (lastDamageTime == null || (Instant.now().toEpochMilli() - lastDamageTime) > 450) {
				int damageToInflict = 30 + Realm.RANDOM.nextInt(15);
				this.sendTextEffectToPlayer(p, TextEffect.DAMAGE, "-" + damageToInflict);
				p.setHealth(p.getHealth() - damageToInflict);
				this.playerGroundDamageState.put(p.getId(), Instant.now().toEpochMilli());
			}
		}
	}

	// Invokes an ability usage server side for the given player at the
	// desired location if applicable
	public void useAbility(final long realmId, final long playerId, final Vector2f pos) {
		final Realm targetRealm = this.realms.get(realmId);

		final Player player = targetRealm.getPlayer(playerId);
		if (player == null || player.getAbility() == null)
			return;
		final GameItem abilityItem = GameDataManager.GAME_ITEMS.get(player.getAbility().getItemId());
		if ((abilityItem == null))
			return;
		final Effect effect = abilityItem.getEffect();
		final Long lastAbilityUsage = this.playerAbilityState.get(playerId);
		if (lastAbilityUsage == null
				|| (Instant.now().toEpochMilli() - lastAbilityUsage >= effect.getCooldownDuration())) {
			this.playerAbilityState.put(playerId, Instant.now().toEpochMilli());
		} else {
			log.info("Ability {} is on cooldown", abilityItem);
			return;
		}
		if (player.getMana() < effect.getMpCost())
			return;
		player.setMana(player.getMana() - effect.getMpCost());
		// If the ability is damaging (knight stun, archer arrow, wizard spell)
		// Resolve the projectile group if the item has a damage definition with a valid projectileGroupId.
		// Script-only abilities (e.g., scepter chain lightning, necromancer skull) may have no damage
		// or a damage with projectileGroupId -1, meaning no projectiles should be created.
		final ProjectileGroup group = (abilityItem.getDamage() != null)
				? GameDataManager.PROJECTILE_GROUPS.get(abilityItem.getDamage().getProjectileGroupId())
				: null;

		if (((abilityItem.getDamage() != null) && (abilityItem.getEffect() != null) && (group != null))) {

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
				Bullet ab1 = this.addProjectile(realmId, 0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
						p.getProjectileId(), source.clone(-offset, -offset), angle + Float.parseFloat(p.getAngle()),
						p.getSize(), p.getMagnitude(), p.getRange(), rolledDamage, false, p.getFlags(),
						p.getAmplitude(), p.getFrequency(), player.getId());
				if (ab1 != null && p.getEffects() != null) ab1.setEffects(p.getEffects());
			}
			// Apply self-effect if present (e.g., warrior helmet SPEEDY buff)
			if (effect.isSelf()) {
				player.addEffect(effect.getEffectId(), effect.getDuration());
			}

		} else if ((abilityItem.getDamage() != null) && (group != null)) {
			final Vector2f dest = new Vector2f(pos.x, pos.y);
			for (final Projectile p : group.getProjectiles()) {

				final short offset = (short) (p.getSize() / (short) 2);
				short rolledDamage = player.getInventory()[1].getDamage().getInRange();
				rolledDamage += player.getComputedStats().getAtt();
				Bullet ab2 = this.addProjectile(realmId, 0l, player.getId(), abilityItem.getDamage().getProjectileGroupId(),
						p.getProjectileId(), dest.clone(-offset, -offset), Float.parseFloat(p.getAngle()), p.getSize(),
						p.getMagnitude(), p.getRange(), rolledDamage, false, p.getFlags(), p.getAmplitude(),
						p.getFrequency(), player.getId());
				if (ab2 != null && p.getEffects() != null) ab2.setEffects(p.getEffects());
			}

			// If the ability is non damaging or script-only (rogue cloak, priest tome, sorcerer scepter)
		} else if (abilityItem.getEffect() != null) {
			// Special case for teleporting
			if (abilityItem.getEffect().getEffectId().equals(ProjectileEffectType.TELEPORT) && !targetRealm.getTileManager().isCollisionTile(pos)) {
				player.setPos(pos);
			} else {
				player.addEffect(abilityItem.getEffect().getEffectId(), abilityItem.getEffect().getDuration());
			}
		}
		// Invoke any item specific scripts
		final UseableItemScriptBase script = this.getItemScript(abilityItem.getItemId());
		if (script != null) {
			script.invokeItemAbility(targetRealm, player, abilityItem, pos);
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
			// fight me for using both kinds of loops
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
		final Player player = targetRealm.getPlayer(p.getId());
		if (player == null) return;

		// Use spatial grid for O(cells) instead of O(all_entities) brute-force
		final float collisionRadius = 10 * com.jrealm.game.contants.GlobalConstants.BASE_TILE_SIZE;
		final Vector2f center = player.getPos();

		// Collect bullets and enemies near this player using spatial grid
		final List<Bullet> nearbyBullets = new ArrayList<>();
		final List<Enemy> nearbyEnemies = new ArrayList<>();

		if (targetRealm.getSpatialGrid() != null) {
			final float radiusSq = collisionRadius * collisionRadius;
			final List<Long> candidates = targetRealm.getSpatialGrid().queryRadius(center.x, center.y, collisionRadius);
			for (int i = 0; i < candidates.size(); i++) {
				final long id = candidates.get(i);
				final Bullet b = targetRealm.getBullets().get(id);
				if (b != null) {
					float dx = b.getPos().x - center.x, dy = b.getPos().y - center.y;
					if (dx * dx + dy * dy <= radiusSq) nearbyBullets.add(b);
					continue;
				}
				final Enemy e = targetRealm.getEnemies().get(id);
				if (e != null) {
					float dx = e.getPos().x - center.x, dy = e.getPos().y - center.y;
					if (dx * dx + dy * dy <= radiusSq) nearbyEnemies.add(e);
				}
			}
		} else {
			// Fallback: brute-force (only when no spatial grid)
			final com.jrealm.game.math.Rectangle viewport = targetRealm.getTileManager().getRenderViewPort(player);
			for (final Bullet b : targetRealm.getBullets().values()) {
				if (b.getBounds().intersect(viewport)) nearbyBullets.add(b);
			}
			for (final Enemy e : targetRealm.getEnemies().values()) {
				if (e.getBounds().intersect(viewport)) nearbyEnemies.add(e);
			}
		}

		// Terrain hit FIRST: destroy bullets that enter walls before they can
		// hit entities on the other side
		this.proccessTerrainHit(realmId, p);

		// Player-bullet collision (enemy bullets hitting player)
		if (!player.hasEffect(ProjectileEffectType.INVINCIBLE)) {
			for (final Bullet b : nearbyBullets) {
				this.processPlayerHit(realmId, b, player);
			}
		}

		// Bullet-enemy collision (player bullets hitting enemies)
		for (final Enemy enemy : nearbyEnemies) {
			for (final Bullet b : nearbyBullets) {
				this.proccessEnemyHit(realmId, b, enemy);
			}
		}
	}

	
	public void enqueueServerPacket(final Packet packet) {
		// Synchronize access to the outbound packet queue
		// To prevent multiple threads enqueuing packets at the same time
		synchronized (this.outboundPacketQueue) {
			this.outboundPacketQueue.add(packet);
		}
	}

	public void enqueueServerPacket(final Player player, final Packet packet) {
		if (player == null || packet == null)
			return;
		
		// Synchronize access to the player's outbound packet queue
		// To prevent multiple threads enqueuing packets at the same time
		synchronized (this.playerOutboundPacketQueue) {
			if (this.playerOutboundPacketQueue.get(player.getId()) == null) {
				final ConcurrentLinkedQueue<Packet> packets = new ConcurrentLinkedQueue<>();
				packets.add(packet);
				this.playerOutboundPacketQueue.put(player.getId(), packets);
			} else {
				this.playerOutboundPacketQueue.get(player.getId()).add(packet);
			}
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
			// Apply on-hit status effects from projectile's effects list
			if (b.getEffects() != null) {
				for (final com.jrealm.game.model.ProjectileEffect pe : b.getEffects()) {
					final ProjectileEffectType effectType = ProjectileEffectType.valueOf(pe.getEffectId());
					if (effectType != null && !p.hasEffect(effectType)) {
						p.addEffect(effectType, pe.getDuration());
						if (effectType.equals(ProjectileEffectType.PARALYZED)) {
							p.setDx(0); p.setDy(0);
						}
						this.sendTextEffectToPlayer(player, TextEffect.DAMAGE, effectType.name());
					}
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
		// Enemies in STASIS are invulnerable — all damage is nullified
		if (e.hasEffect(ProjectileEffectType.STASIS))
			return;
		if (b.getBounds().collides(0, 0, e.getBounds()) && !b.isEnemy()) {
			final short minDmg = (short) (b.getDamage() * 0.15);
			short dmgToInflict = (short) (b.getDamage() - model.getStats().getDef());
			if (dmgToInflict < minDmg) {
				dmgToInflict = minDmg;
			}

			// Track damage for loot credit
			if (b.getSrcEntityId() != 0L && targetRealm.getOverseer() != null) {
				targetRealm.getOverseer().trackDamage(e.getId(), b.getSrcEntityId(), dmgToInflict);
			}

			if(b.getSrcEntityId() != 0l) {
				final Player fromPlayer = this.getPlayerById(b.getSrcEntityId());
				if(fromPlayer!=null &&  fromPlayer.hasEffect(ProjectileEffectType.DAMAGING)) {
					dmgToInflict = (short)(dmgToInflict * 1.5);
				}
			}
			// CURSED enemies take 25% more damage from all sources
			if (e.hasEffect(ProjectileEffectType.CURSED)) {
				dmgToInflict = (short)(dmgToInflict * 1.25);
			}

			targetRealm.hitEnemy(b.getId(), e.getId());
			e.setHealth(e.getHealth() - dmgToInflict);
			int maxHealth = model.getHealth() * e.getHealthMultiplier();
			e.setHealthpercent((float) e.getHealth() / (float) maxHealth);
			if (b.hasFlag(ProjectileEffectType.PLAYER_PROJECTILE) && !b.isEnemyHit()) {
				b.setEnemyHit(true);
			} else if (b.remove()) {
				targetRealm.getExpiredBullets().add(b.getId());
				targetRealm.removeBullet(b);
			} else {
				targetRealm.getExpiredBullets().add(b.getId());
				targetRealm.removeBullet(b);
			}
			// Apply on-hit status effects from projectile's effects list (data-driven durations)
			if (b.getEffects() != null) {
				for (final com.jrealm.game.model.ProjectileEffect pe : b.getEffects()) {
					final ProjectileEffectType effectType = ProjectileEffectType.valueOf(pe.getEffectId());
					if (effectType != null && !e.hasEffect(effectType)) {
						e.addEffect(effectType, pe.getDuration());
						this.broadcastTextEffect(EntityType.ENEMY, e, TextEffect.DAMAGE, effectType.name());
					}
				}
			}
			
			this.broadcastTextEffect(EntityType.ENEMY, e, TextEffect.DAMAGE, "-" + dmgToInflict);
			if (e.getDeath()) {
				targetRealm.getExpiredBullets().add(b.getId());
				this.enemyDeath(targetRealm, e);
			}
		}
	}

	public Bullet addProjectile(final long realmId, final long id, final long targetPlayerId, final int projectileId,
			final int projectileGroupId, final Vector2f src, final Vector2f dest, final short size,
			final float magnitude, final float range, short damage, final boolean isEnemy, final List<Short> flags, long srcEntityId) {
		final Realm targetRealm = this.realms.get(realmId);
		final Player player = targetRealm.getPlayer(targetPlayerId);
		if (player == null)
			return null;

		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}

		final long idToUse = id == 0l ? Realm.RANDOM.nextLong() : id;
		final Bullet b = new Bullet(idToUse, projectileId, src, dest, size, magnitude, range, damage, isEnemy);
		b.setSrcEntityId(srcEntityId);
		b.setFlags(flags);
		targetRealm.addBullet(b);
		return b;
	}

	public Bullet addProjectile(final long realmId, final long id, final long targetPlayerId, final int projectileId,
			final int projectileGroupId, final Vector2f src, final float angle, final short size, final float magnitude,
			final float range, short damage, final boolean isEnemy, final List<Short> flags, final short amplitude,
			final short frequency, long srcEntityId) {
		final Realm targetRealm = this.realms.get(realmId);
		final Player player = targetRealm.getPlayer(targetPlayerId);
		if (player == null)
			return null;
		final ProjectileGroup pg = GameDataManager.PROJECTILE_GROUPS.get(projectileGroupId);
		if (!isEnemy) {
			damage = (short) (damage + player.getStats().getAtt());
		}

		final long idToUse = id == 0l ? Realm.RANDOM.nextLong() : id;
		final Bullet b = new Bullet(idToUse, projectileId, src, angle, size, magnitude, range, damage, isEnemy);
		b.setSrcEntityId(srcEntityId);
		b.setAmplitude(amplitude);
		b.setFrequency(frequency);
		b.setFlags(flags);
		targetRealm.addBullet(b);
		return b;
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

	// Invoked upon enemy death. Public so item scripts (e.g., Necromancer skull,
	// Sorcerer scepter) can trigger proper death handling with loot/XP.
	public void enemyDeath(final Realm targetRealm, final Enemy enemy) {
		final EnemyModel model = GameDataManager.ENEMIES.get(enemy.getEnemyId());
		try {
			// Get players in the viewport of this enemy and increment their experience
			for (final Player player : targetRealm
					.getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(enemy))) {
				final int xpToGive = model.getXp() * (targetRealm.getDepth() == 0 ? 1 : targetRealm.getDepth() + 1);
				player.incrementExperience(xpToGive);
				try {
					this.enqueueServerPacket(player, TextEffectPacket.from(EntityType.PLAYER, player.getId(),
							TextEffect.PLAYER_INFO, xpToGive + "xp"));
				} catch (Exception ex) {
					RealmManagerServer.log.error("[SERVER] Failed to create player experience text effect. Reason: {}", ex);
				}
			}

			// Notify the overseer of the kill (handles taunts, event spawning, damage credit)
			if (targetRealm.getOverseer() != null) {
				long killerId = targetRealm.getOverseer().getTopDamageDealer(enemy.getId());
				targetRealm.getOverseer().onEnemyKilled(enemy, killerId);
				targetRealm.getOverseer().clearDamageTracking(enemy.getId());
			}

			targetRealm.getExpiredEnemies().add(enemy.getId());
			targetRealm.clearHitMap();
			// Overseer handles repopulation now — skip legacy spawnRandomEnemy for overworld
			targetRealm.removeEnemy(enemy);

			// Try to get the loot model mapped by this enemyId
			final LootTableModel lootTable = GameDataManager.LOOT_TABLES.get(enemy.getEnemyId());
			if (lootTable == null) {
				log.warn("[SERVER] No loot table registered for enemy {}", enemy.getEnemyId());
				return;
			}

			// Get a random loot bag drop based on this enemies loot table
			final List<GameItem> lootToDrop = lootTable.getLootDrop();
			if (lootToDrop.size() > 0) {
				final LootContainer dropsBag = new LootContainer(LootTier.BLUE, enemy.getPos().withNoise(64, 64),
						lootToDrop.toArray(new GameItem[0]));
				targetRealm.addLootContainer(dropsBag);
			}

			// Portal drops: use dungeon graph if this realm has a nodeId
			final String currentNodeId = targetRealm.getNodeId();
			final com.jrealm.game.model.DungeonGraphNode currentNode = (currentNodeId != null && GameDataManager.DUNGEON_GRAPH != null)
					? GameDataManager.DUNGEON_GRAPH.get(currentNodeId) : null;

			log.info("[SERVER] enemyDeath: enemy={} nodeId={} currentNode={} portalDrops={}",
					enemy.getEnemyId(), currentNodeId, currentNode != null ? currentNode.getDisplayName() : "null",
					lootTable.getPortalDrops());

			if (currentNode != null && currentNode.getPortalDropNodeMap() != null
					&& !currentNode.getPortalDropNodeMap().isEmpty()) {
				// Graph-based portal drops: drop portals to child nodes
				if (lootTable.getPortalDrops() != null) {
					final List<Integer> rolledPortals = lootTable.getPortalDrop();
					log.info("[SERVER] enemyDeath: rolled portals={} from drops={}", rolledPortals, lootTable.getPortalDrops());
					for (int portalId : rolledPortals) {
						// Find which child node this portalId leads to from the current node
						String targetNodeId = null;
						for (Map.Entry<String, Integer> entry : currentNode.getPortalDropNodeMap().entrySet()) {
							if (entry.getValue() == portalId) {
								targetNodeId = entry.getKey();
								break;
							}
						}
						if (targetNodeId == null) {
							log.info("[SERVER] enemyDeath: portalId {} not in node's portalDropNodeMap {}, skipping",
									portalId, currentNode.getPortalDropNodeMap());
							continue;
						}

						PortalModel portalModel = GameDataManager.PORTALS.get(portalId);
						if (portalModel == null) continue;

						Portal portal = new Portal(Realm.RANDOM.nextLong(),
								(short) portalModel.getPortalId(), enemy.getPos().withNoise(64, 64));

						// Check if a realm for this node already exists
						Optional<Realm> existingRealm = this.findRealmForNode(targetNodeId);
						if (existingRealm.isPresent()) {
							portal.linkPortal(targetRealm, existingRealm.get());
						} else {
							portal.linkPortal(targetRealm, null);
						}
						// Store target node info on the portal for lazy realm creation
						portal.setTargetNodeId(targetNodeId);
						targetRealm.addPortal(portal);
						log.info("[SERVER] enemyDeath: SPAWNED portal {} -> node {} at ({}, {})",
								portalId, targetNodeId, portal.getPos().x, portal.getPos().y);
					}
				}
			} else {
				// Legacy depth-based portal drops (fallback for realms without graph nodes)
				if (lootTable.getPortalDrops() != null) {
					for (int portalId : lootTable.getPortalDrop()) {
						PortalModel portalModel = GameDataManager.PORTALS.get(portalId);
						if (portalModel == null) continue;

						Portal portal = new Portal(Realm.RANDOM.nextLong(),
								(short) portalModel.getPortalId(), enemy.getPos().withNoise(64, 64));
						if (portalModel.getTargetRealmDepth() >= 999) {
							portal.linkPortal(targetRealm, null);
						} else {
							Optional<Realm> realmAtDepth = this.findRealmAtDepth(portalModel.getTargetRealmDepth() - 1);
							if (realmAtDepth.isEmpty()) {
								portal.linkPortal(targetRealm, null);
							} else {
								portal.linkPortal(targetRealm, realmAtDepth.get());
							}
						}
						targetRealm.addPortal(portal);
					}
				}
			}
		} catch (Exception e) {
			RealmManagerServer.log.error("[SERVER] Failed to handle dead Enemy {}. Reason: {}", enemy.getId(), e);
		}
	}

	// Invoked upon player death
	private void playerDeath(final Realm targetRealm, final Player player) {
		try {
			final String remoteAddrDeath = this.getRemoteAddressMapReversed().get(player.getId());
			final LootContainer graveLoot = new LootContainer(LootTier.GRAVE, player.getPos().clone(),
					player.getSlots(0, 12));
			targetRealm.addLootContainer(graveLoot);
			targetRealm.getExpiredPlayers().add(player.getId());
			if (player.isHeadless() || player.isBot()) {
				targetRealm.removePlayer(player);
				this.clearPlayerState(player.getId());
				// Close the bot's session so NIO server stops iterating it
				if (remoteAddrDeath != null) {
					this.remoteAddresses.remove(remoteAddrDeath);
					final ClientSession botSession = this.server.getClients().get(remoteAddrDeath);
					if (botSession != null) {
						botSession.setShutdownProcessing(true);
						botSession.close();
						this.server.getClients().remove(remoteAddrDeath);
					}
				}
				return;
			}
			this.enqueueServerPacket(player, PlayerDeathPacket.from(player.getId()));
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
			RealmManagerServer.log.error("[SERVER] Failed to Remove dead Player {}. Reason: {}", e);
		}
	}

	public void clearPlayerState(long playerId) {
		this.playerLoadState.remove(playerId);
		this.playerUpdateState.remove(playerId);
		this.playerUnloadState.remove(playerId);
		this.playerObjectMoveState.remove(playerId);
		this.playerAbilityState.remove(playerId);
		this.playerLoadMapState.remove(playerId);
		this.playerLastHeartbeatTime.remove(playerId);
		this.playerGroundDamageState.remove(playerId);
	}

	public PortalModel getPortalToDepth(int targetDepth) {
		return GameDataManager.PORTALS.values().stream().filter(portal -> portal.getTargetRealmDepth() == targetDepth)
				.findAny().get();
	}

	public Map<Long, String> getRemoteAddressMapReversed() {
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

	public Optional<Realm> findRealmForNode(String nodeId) {
		if (nodeId == null) return Optional.empty();
		return this.getRealms().values().stream()
				.filter(realm -> nodeId.equals(realm.getNodeId()))
				.findAny();
	}

	public void enqueChunkedText(Player target, List<String> textLines) {
		for (String line : textLines) {
			TextPacket textPacket;
			try {
				textPacket = TextPacket.from("SYSTEM", target.getName(), line);
				this.enqueueServerPacket(target, textPacket);
			} catch (Exception e) {
				log.error("[SERVER] Failed to send text line {} to player {}. Reason: {}", line, target.getName(), e);
			}
		}
	}

	public Player findPlayerByName(String name) {
		Player result = null;
		for (Realm realm : this.getRealms().values()) {
			for (Player player : realm.getPlayers().values()) {
				if (player.getName().equalsIgnoreCase(name)) {
					result = player;
					break;
				}
			}
			if (result != null) {
				break;
			}
		}
		return result;
	}

	public Player searchRealmsForPlayer(String playerName) {
		Player found = null;
		final List<Player> allPlayers = this.getPlayers();
		for (Player player : allPlayers) {
			if (player.getName() != null && player.getName().equalsIgnoreCase(playerName)) {
				found = player;
			}
		}
		if (found == null) {
			log.info("[SERVER] searchRealmsForPlayer('{}') not found. Online players: {}",
				playerName, allPlayers.stream().map(p -> p.getName()).collect(java.util.stream.Collectors.toList()));
		}
		return found;
	}

	public Player searchRealmsForPlayer(long playerId) {
		Player found = null;
		for (Player player : this.getPlayers()) {
			if (player.getId() == playerId) {
				found = player;
			}

		}
		return found;
	}

	// Background thread for persisting player data to DB
	private void beginPlayerSync() {
		final Runnable playerSync = () -> {
			try {
				while (!this.shutdown) {
					Thread.sleep(12000);
					RealmManagerServer.log.info("[SERVER] Performing asynchronous player data sync.");
					this.persistsPlayersAsync();
				}
			} catch (Exception e) {
				RealmManagerServer.log.error("[SERVER] Failed to perform player data sync. Reason: {}", e.getMessage());
			}
		};
		WorkerThread.submitAndForkRun(playerSync);
	}

	// Server shutdown task. Attempts to save all player data before JVM exit
	public Thread shutdownHook() {
		final Runnable shutdownTask = () -> {
			RealmManagerServer.log.info("[SERVER] Performing pre-shutdown player sync...");
			this.persistsPlayersAsync();
			RealmManagerServer.log.info("[SERVER] Shutdown player sync complete");
		};
		return new Thread(shutdownTask);
	}

	public void persistsPlayersAsync() {
		final Runnable persist = () -> {
			for (Player player : this.getPlayers()) {
				this.persistPlayer(player);
			}
		};
		WorkerThread.doAsync(persist);
	}

	public List<Player> getPlayers() {
		final List<Player> players = new ArrayList<>();
		for (final Map.Entry<Long, Realm> realm : this.realms.entrySet()) {
			for (final Player player : realm.getValue().getPlayers().values()) {
				players.add(player);
			}
		}
		return players;
	}

	public Player getPlayerById(long playerId) {
		return this.getPlayers().stream().filter(p -> p.getId() == playerId).findAny().orElse(null);
	}

	public void safeRemoveRealm(final Realm realm) {
		this.safeRemoveRealm(realm.getRealmId());
	}

	public void safeRemoveRealm(final long realmId) {
		this.acquireRealmLock();
		final Realm realm = this.realms.remove(realmId);
		realm.setShutdown(true);
		this.releaseRealmLock();
	}

	private void persistPlayerAsync(final Player player) {
		final Runnable persist = () -> {
			this.persistPlayer(player);
		};
		WorkerThread.doAsync(persist);
	}

	private boolean persistPlayer(final Player player) {
		if (player.isHeadless() || player.isBot())
			return false;
		// Extra safety: never persist bot accounts even if flag wasn't set
		if (player.getAccountUuid() == null || player.getName() == null || player.getName().startsWith("Bot_"))
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
				character.setItems(newItems);
				character.setStats(newStats);
				final CharacterDto savedStats = ServerGameLogic.DATA_SERVICE.executePost(
						"/data/account/character/" + character.getCharacterUuid(), character, CharacterDto.class);
				RealmManagerServer.log.info("[SERVER] Succesfully persisted user account {}",
						account.getAccountEmail());
			}
		} catch (Exception e) {
			RealmManagerServer.log.error("[SERVER] Failed to get player account. Reason: {}", e);
		}
		return true;
	}

	private void sendTextEffectToPlayer(final Player player, final TextEffect effect, final String text) {
		try {
			this.enqueueServerPacket(player, TextEffectPacket.from(EntityType.PLAYER, player.getId(), effect, text));
		} catch (Exception e) {
			RealmManagerServer.log.error("[SERVER] Failed to send TextEffect Packet to Player {}. Reason: {}",
					player.getId(), e);
		}
	}

	public void broadcastTextEffect(final EntityType entityType, final GameObject entity, final TextEffect effect,
			final String text) {
		try {
			this.enqueueServerPacket(TextEffectPacket.from(entityType, entity.getId(), effect, text));
		} catch (Exception e) {
			RealmManagerServer.log.error("[SERVER] Failed to broadcast TextEffect Packet for Entity {}. Reason: {}",
					entity.getId(), e);
		}
	}

	/**
	 * Register a poison damage-over-time effect on an enemy.
	 * If the enemy is already poisoned, the stronger poison replaces the weaker one.
	 */
	public void registerPoisonDot(long realmId, long enemyId, int totalDamage, long duration, long sourcePlayerId) {
		synchronized (this.activePoisonDots) {
			// Remove existing poison on same enemy if weaker
			this.activePoisonDots.removeIf(dot ->
					dot.enemyId == enemyId && dot.realmId == realmId && dot.totalDamage <= totalDamage);
			// Only add if no stronger poison exists
			boolean hasStronger = this.activePoisonDots.stream()
					.anyMatch(dot -> dot.enemyId == enemyId && dot.realmId == realmId);
			if (!hasStronger) {
				this.activePoisonDots.add(new PoisonDotState(realmId, enemyId, totalDamage, duration, sourcePlayerId));
			}
		}
	}

	/**
	 * Process all active poison DoTs. Called every server tick.
	 * Poison ignores defense (matches RotMG behavior).
	 */
	private void processPoisonDots() {
		synchronized (this.activePoisonDots) {
			final java.util.Iterator<PoisonDotState> it = this.activePoisonDots.iterator();
			while (it.hasNext()) {
				final PoisonDotState dot = it.next();
				final Realm realm = this.realms.get(dot.realmId);
				if (realm == null) { it.remove(); continue; }
				final Enemy enemy = realm.getEnemy(dot.enemyId);
				if (enemy == null || enemy.getDeath()) { it.remove(); continue; }
				if (dot.isExpired()) { it.remove(); continue; }

				// Calculate per-tick damage: totalDamage spread over (duration / tickInterval) ticks
				// Server runs at 64 tps, poison ticks every 4 ticks (~62ms) to avoid spam
				long elapsed = java.time.Instant.now().toEpochMilli() - dot.startTime;
				int expectedDamage = (int) ((float) elapsed / dot.duration * dot.totalDamage);
				int tickDamage = expectedDamage - dot.damageApplied;
				if (tickDamage <= 0) continue;

				dot.damageApplied += tickDamage;
				enemy.setHealth(enemy.getHealth() - tickDamage);
				this.broadcastTextEffect(com.jrealm.game.contants.EntityType.ENEMY, enemy,
						com.jrealm.game.contants.TextEffect.DAMAGE, "-" + tickDamage);

				if (enemy.getDeath()) {
					this.enemyDeath(realm, enemy);
					it.remove();
				}
			}
		}
	}

	public void acquireRealmLock() {
		try {
			this.realmLock.acquire();
		} catch (Exception e) {
			log.error("[SERVER] Failed to acquire the realm lock. Reason: {}", e.getMessage());
		}
	}

	public void releaseRealmLock() {
		try {
			this.realmLock.release();
		} catch (Exception e) {
			log.error("[SERVER] Failed to release the realm lock. Reason: {}", e.getMessage());
		}
	}
}
