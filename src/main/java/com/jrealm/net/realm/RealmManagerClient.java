package com.jrealm.net.realm;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.state.PlayState;
import com.jrealm.game.util.TimedWorkerThread;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.Packet;
import com.jrealm.net.client.ClientGameLogic;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.MoveItemPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class RealmManagerClient implements Runnable {
    private SocketClient client;
    private PlayState state;
    private Realm realm;
    private boolean shutdown = false;
    private final Map<Byte, BiConsumer<RealmManagerClient, Packet>> packetCallbacksClient = new HashMap<>();
    private long currentPlayerId;
    private TimedWorkerThread workerThread;

    public RealmManagerClient(PlayState state, Realm realm) {
        this.registerPacketCallbacks();
        this.realm = realm;
        this.client = new SocketClient(SocketClient.SERVER_ADDR, 2222);
        this.state = state;
        WorkerThread.submitAndForkRun(this.client);
    }

    @Override
    public void run() {
        RealmManagerClient.log.info("Starting JRealm Client");

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
            RealmManagerClient.log.error("Failed to sleep");
        }
    }

    public void processClientPackets() {
        while (!this.getClient().getInboundPacketQueue().isEmpty()) {
            Packet toProcess = this.getClient().getInboundPacketQueue().remove();
            try {
				Packet created = toProcess;
				//log.info("[CLIENT] Processing client packet {} ", created);
                created.setSrcIp(toProcess.getSrcIp());
                this.packetCallbacksClient.get(created.getId()).accept(this, created);
            } catch (Exception e) {
                RealmManagerClient.log.error("Failed to process client packets {}", e);
            }
        }
    }

    private void registerPacketCallbacks() {
        this.registerPacketCallback(PacketType.UPDATE.getPacketId(), ClientGameLogic::handleUpdateClient);
        this.registerPacketCallback(PacketType.OBJECT_MOVE.getPacketId(), ClientGameLogic::handleObjectMoveClient);
        this.registerPacketCallback(PacketType.TEXT.getPacketId(), ClientGameLogic::handleTextClient);
        this.registerPacketCallback(PacketType.COMMAND.getPacketId(), ClientGameLogic::handleCommandClient);
        this.registerPacketCallback(PacketType.LOAD.getPacketId(), ClientGameLogic::handleLoadClient);
        this.registerPacketCallback(PacketType.LOAD_MAP.getPacketId(), ClientGameLogic::handleLoadMapClient);
        this.registerPacketCallback(PacketType.UNLOAD.getPacketId(), ClientGameLogic::handleUnloadClient);
        this.registerPacketCallback(PacketType.TEXT_EFFECT.getPacketId(), ClientGameLogic::handleTextEffectClient);
        this.registerPacketCallback(PacketType.PLAYER_DEATH.getPacketId(), ClientGameLogic::handlePlayerDeathClient);
    }

    private void registerPacketCallback(byte packetId, BiConsumer<RealmManagerClient, Packet> callback) {
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
            RealmManagerClient.log.error("Failed to send MoveItem packet. Reason: {}", e);
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
