package com.jrealm.net.realm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.state.PlayState;
import com.jrealm.net.Packet;
import com.jrealm.net.client.ClientGameLogic;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.client.packet.AcceptTradeRequestPacket;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.PlayerDeathPacket;
import com.jrealm.net.client.packet.RequestTradePacket;
import com.jrealm.net.client.packet.TextEffectPacket;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.ServerGameLogic;
import com.jrealm.net.server.ServerTradeManager;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.MoveItemPacket;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.util.PacketHandlerClient;
import com.jrealm.util.PacketHandlerServer;
import com.jrealm.util.TimedWorkerThread;
import com.jrealm.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class RealmManagerClient implements Runnable {
	private Reflections classPathScanner = new Reflections("com.jrealm", Scanners.SubTypes, Scanners.MethodsAnnotated);
	private MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
	private final Map<Byte, List<MethodHandle>> userPacketCallbacksClient = new HashMap<>();

    private SocketClient client;
    private PlayState state;
    private Realm realm;
    private boolean shutdown = false;
    private final Map<Class<? extends Packet>, BiConsumer<RealmManagerClient, Packet>> packetCallbacksClient = new HashMap<>();
    private long currentPlayerId;
    private TimedWorkerThread workerThread;

    public RealmManagerClient(PlayState state, Realm realm) {
        this.registerPacketCallbacks();
        this.registerPacketCallbacksReflection();
        this.realm = realm;
        this.client = new SocketClient(SocketClient.SERVER_ADDR, 2222);
        this.state = state;
        WorkerThread.submitAndForkRun(this.client);
    }

    @Override
    public void run() {
        RealmManagerClient.log.info("[CLIENT] Starting JRealm Client");

        final Runnable tick = () -> {
            this.tick();
            this.update(0);
        };

        this.workerThread = new TimedWorkerThread(tick, 64);
        WorkerThread.submitAndForkRun(this.workerThread);

        RealmManagerClient.log.info("[CLIENT] RealmManagerClient exiting run().");
    }

    private void tick() {
        try {
            final Runnable processClientPackets = () -> {
                this.processClientPackets();
            };

            WorkerThread.submitAndRun(processClientPackets);
        } catch (Exception e) {
            RealmManagerClient.log.error("[CLIENT] Failed to sleep");
        }
    }

    public void processClientPackets() {
        while (!this.getClient().getInboundPacketQueue().isEmpty()) {
            Packet toProcess = this.getClient().getInboundPacketQueue().remove();
            try {
				Packet created = toProcess;
				//log.info("[CLIENT] Processing client packet {} ", created);
                created.setSrcIp(toProcess.getSrcIp());
                created.setId(toProcess.getId());
                BiConsumer<RealmManagerClient, Packet> consumer = this.packetCallbacksClient.get(created.getClass());
                if(consumer!=null) {
                	consumer.accept(this, created);
                }
                final List<MethodHandle> packetHandles = this.userPacketCallbacksClient.get(created.getId());
				long start = System.nanoTime();
				if (packetHandles != null) {

					for (MethodHandle handler : packetHandles) {
						try {
							handler.invokeExact(this, created);
						} catch (Throwable e) {
							log.error("Failed to invoke packet callback. Reason: {}", e);
						}
					}
					log.info("[CLIENT] Invoked {} packet callbacks for PacketType {} using reflection in {} nanos",
							packetHandles.size(), PacketType.valueOf(created.getId()),
							(System.nanoTime() - start));
				}
            } catch (Exception e) {
                RealmManagerClient.log.error("[CLIENT] Failed to process client packets {}", e);
            }
        }
    }

    private void registerPacketCallbacks() {
        this.registerPacketCallback(UpdatePacket.class, ClientGameLogic::handleUpdateClient);
        this.registerPacketCallback(ObjectMovePacket.class, ClientGameLogic::handleObjectMoveClient);
        this.registerPacketCallback(TextPacket.class, ClientGameLogic::handleTextClient);
        this.registerPacketCallback(CommandPacket.class, ClientGameLogic::handleCommandClient);
        this.registerPacketCallback(LoadPacket.class, ClientGameLogic::handleLoadClient);
        this.registerPacketCallback(LoadMapPacket.class, ClientGameLogic::handleLoadMapClient);
        this.registerPacketCallback(UnloadPacket.class, ClientGameLogic::handleUnloadClient);
        this.registerPacketCallback(TextEffectPacket.class, ClientGameLogic::handleTextEffectClient);
        this.registerPacketCallback(PlayerDeathPacket.class, ClientGameLogic::handlePlayerDeathClient);
//        this.registerPacketCallback(RequestTradePacket.class, ClientGameLogic::handleTradeRequestClient);
//        this.registerPacketCallback(AcceptTradeRequestPacket.class, ClientGameLogic::handleAcceptTrade);

    }
    
    private void registerPacketCallbacksReflection() {
		log.info("[CLIENT] Registering packet handlers using reflection");
		final MethodType mt = MethodType.methodType(void.class, RealmManagerClient.class, Packet.class);

		final Set<Method> subclasses = this.classPathScanner.getMethodsAnnotatedWith(PacketHandlerClient.class);
		for (final Method method : subclasses) {
			try {
				final PacketHandlerClient packetToHandle = method.getDeclaredAnnotation(PacketHandlerClient.class);
				MethodHandle handlerMethod = null;
				
					handlerMethod = this.publicLookup.findStatic(ClientGameLogic.class, method.getName(), mt);
				

				if (handlerMethod != null) {
					final Entry<Byte, Class<? extends Packet>> targetPacketType = PacketType.valueOf(packetToHandle.value());
					List<MethodHandle> existing = this.userPacketCallbacksClient.get(targetPacketType.getKey());
					if (existing == null) {
						existing = new ArrayList<>();
					}
					existing.add(handlerMethod);
					log.info("[CLIENT] Added new packet handler for packet {}. Handler method: {}", targetPacketType.getKey(),
							handlerMethod.toString());
					this.userPacketCallbacksClient.put(targetPacketType.getKey(), existing);
				}
			} catch (Exception e) {
				log.error("[CLIENT] Failed to get MethodHandle to method {}. Reason: {}", method.getName(), e);
			}
		}
	}

    private void registerPacketCallback(Class<? extends Packet> packetId, BiConsumer<RealmManagerClient, Packet> callback) {
        this.packetCallbacksClient.put(packetId, callback);
    }

    public void update(double time) {
        this.state.update(time);
    }

    public void startHeartbeatThread() {
        Runnable sendHeartbeat = () -> {
            while (!this.shutdown) {
                try {
                    long currentTime = Instant.now().toEpochMilli();
                    long playerId = this.currentPlayerId;

                    HeartbeatPacket pack = HeartbeatPacket.from(playerId, currentTime);
                    this.client.sendRemote(pack);
                    Thread.sleep(1000);
                } catch (Exception e) {
                    RealmManagerClient.log.error("Failed to send Heartbeat packet. Reason: {}", e);
                }
            }
        };
        WorkerThread.submitAndForkRun(sendHeartbeat);
    }

    public void moveItem(int toSlotIndex, int fromSlotIndex, boolean drop, boolean consume) {
        try {
            MoveItemPacket moveItem = MoveItemPacket.from(this.state.getPlayer().getId(), (byte) toSlotIndex,
                    (byte) fromSlotIndex, drop, consume);
            this.getClient().sendRemote(moveItem);
        } catch (Exception e) {
            RealmManagerClient.log.error("[CLIENT] Failed to send MoveItem packet. Reason: {}", e);
        }
    }

    public Player getClosestPlayer(final Vector2f pos, final float limit) {
        float best = Float.MAX_VALUE;
        Player bestPlayer = null;
        final Realm targetRealm = this.realm;
        for (final Player player : targetRealm.getPlayers().values()) {
            final float dist = player.getPos().distanceTo(pos);
            if ((dist < best) && (dist <= limit)) {
                best = dist;
                bestPlayer = player;
            }
        }
        return bestPlayer;
    }

    public LootContainer getClosestLootContainer(final Vector2f pos, final float limit) {
        float best = Float.MAX_VALUE;
        LootContainer bestLoot = null;
        final Realm targetRealm = this.realm;
        for (final LootContainer lootContainer : targetRealm.getLoot().values()) {
            float dist = lootContainer.getPos().distanceTo(pos);
            if ((dist < best) && (dist <= limit)) {
                best = dist;
                bestLoot = lootContainer;
            }
        }
        return bestLoot;
    }
}
