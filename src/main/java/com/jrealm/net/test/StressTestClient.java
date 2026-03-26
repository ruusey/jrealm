package com.jrealm.net.test;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.core.IOService;
import com.jrealm.net.messaging.CommandType;
import com.jrealm.net.messaging.LoginRequestMessage;
import com.jrealm.net.messaging.LoginResponseMessage;
import com.jrealm.net.messaging.ServerCommandMessage;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.util.Cardinality;
import com.jrealm.util.TimedWorkerThread;
import com.jrealm.util.WorkerThread;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A stress test client that connects to the game server, performs login,
 * and then simulates gameplay by sending movement + shoot packets at high rates
 * to generate bandwidth from the server (entity updates, map loads, etc.).
 */
@Slf4j
public class StressTestClient implements Runnable {
    private static final int BUFFER_CAPACITY = 65536 * 10;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Random RANDOM = new Random();

    private final String host;
    private final int port;
    private final String email;
    private final String password;
    private final String characterUuid;
    private final int clientIndex;

    private Socket clientSocket;
    private volatile boolean shutdown = false;
    private byte[] remoteBuffer = new byte[BUFFER_CAPACITY];
    private int remoteBufferIndex = 0;

    @Getter
    private volatile long totalBytesReceived = 0;
    @Getter
    private volatile long totalBytesSent = 0;
    @Getter
    private volatile long totalPacketsReceived = 0;
    @Getter
    private volatile long totalPacketsSent = 0;
    @Getter
    private volatile long currentBytesReceived = 0;

    private volatile long assignedPlayerId = -1;
    private volatile boolean loggedIn = false;
    private volatile float spawnX = -1;
    private volatile float spawnY = -1;

    private volatile Queue<Packet> inboundPacketQueue = new ConcurrentLinkedQueue<>();
    private volatile Queue<Packet> outboundPacketQueue = new ConcurrentLinkedQueue<>();
    private TimedWorkerThread sendPacketThread = null;
    private TimedWorkerThread readPacketThread = null;
    private TimedWorkerThread gameplayThread = null;

    public StressTestClient(int clientIndex, String host, int port, String email, String password,
            String characterUuid) {
        this.clientIndex = clientIndex;
        this.host = host;
        this.port = port;
        this.email = email;
        this.password = password;
        this.characterUuid = characterUuid;
    }

    @Override
    public void run() {
        try {
            this.clientSocket = new Socket(this.host, this.port);
            this.clientSocket.setTcpNoDelay(true);
            log.info("[Client-{}] Connected to {}:{}", this.clientIndex, this.host, this.port);
        } catch (Exception e) {
            log.error("[Client-{}] Failed to connect: {}", this.clientIndex, e.getMessage());
            return;
        }

        final Runnable readPackets = () -> {
            if (!this.shutdown) this.readPackets();
        };
        final Runnable sendPackets = () -> {
            if (!this.shutdown) this.sendPackets();
        };

        this.sendPacketThread = new TimedWorkerThread(sendPackets, 64);
        this.readPacketThread = new TimedWorkerThread(readPackets, 64);
        WorkerThread.submitAndForkRun(this.readPacketThread, this.sendPacketThread);

        // Send login immediately - the 1750ms handshake timeout is tight
        WorkerThread.runLater(() -> {
            this.sendLogin();
        }, 100);

        // Start gameplay simulation once logged in
        final Runnable simulateGameplay = () -> {
            if (!this.shutdown && this.loggedIn) {
                this.simulateGameplay();
            }
        };
        // Simulate gameplay at ~20 ticks/sec (movement + shooting)
        this.gameplayThread = new TimedWorkerThread(simulateGameplay, 50);
        WorkerThread.submitAndForkRun(this.gameplayThread);

        // Process inbound packets (handle login response, drain queue)
        final Runnable processInbound = () -> {
            while (!this.shutdown) {
                try {
                    this.processInboundPackets();
                    Thread.sleep(16);
                } catch (Exception e) {
                    // ignore
                }
            }
        };
        WorkerThread.submitAndForkRun(processInbound);
    }

    private void sendLogin() {
        try {
            LoginRequestMessage loginReq = LoginRequestMessage.builder()
                    .email(this.email)
                    .password(this.password)
                    .characterUuid(this.characterUuid)
                    .build();
            CommandPacket loginPacket = CommandPacket.from(CommandType.LOGIN_REQUEST, loginReq);
            this.outboundPacketQueue.add(loginPacket);
            log.info("[Client-{}] Login request sent for {}", this.clientIndex, this.email);
        } catch (Exception e) {
            log.error("[Client-{}] Failed to send login: {}", this.clientIndex, e.getMessage());
        }
    }

    private void processInboundPackets() {
        while (!this.inboundPacketQueue.isEmpty()) {
            Packet packet = this.inboundPacketQueue.poll();
            if (packet == null) continue;

            // Check for login response
            if (packet instanceof CommandPacket) {
                CommandPacket cmd = (CommandPacket) packet;
                if (cmd.getCommandId() == CommandType.LOGIN_RESPONSE.getCommandId()) {
                    try {
                        LoginResponseMessage resp = cmd.messageAs(LoginResponseMessage.class);
                        if (resp.isSuccess()) {
                            this.assignedPlayerId = resp.getPlayerId();
                            this.loggedIn = true;
                            log.info("[Client-{}] Logged in! PlayerId={}", this.clientIndex, this.assignedPlayerId);

                            // Teleport to spawn position if set
                            if (this.spawnX >= 0 && this.spawnY >= 0) {
                                try {
                                    float offsetX = this.spawnX + (RANDOM.nextFloat() * 60 - 30);
                                    float offsetY = this.spawnY + (RANDOM.nextFloat() * 60 - 30);
                                    ServerCommandMessage tpCmd = ServerCommandMessage.parseFromInput(
                                            "/tp " + offsetX + " " + offsetY);
                                    CommandPacket tpPacket = CommandPacket.from(CommandType.SERVER_COMMAND, tpCmd);
                                    tpPacket.setPlayerId(this.assignedPlayerId);
                                    this.outboundPacketQueue.add(tpPacket);
                                    log.info("[Client-{}] Teleporting to ({}, {})", this.clientIndex, offsetX, offsetY);
                                } catch (Exception ex) {
                                    log.error("[Client-{}] Failed to send tp command: {}", this.clientIndex, ex.getMessage());
                                }
                            }
                        } else {
                            log.error("[Client-{}] Login failed", this.clientIndex);
                        }
                    } catch (Exception e) {
                        log.error("[Client-{}] Failed to parse login response: {}", this.clientIndex, e.getMessage());
                    }
                }
            }
            // Handle death - shutdown this bot
            if (packet.getId() == com.jrealm.game.contants.PacketType.getPacketId(
                    com.jrealm.net.client.packet.PlayerDeathPacket.class)) {
                log.info("[Client-{}] Bot died, shutting down", this.clientIndex);
                this.shutdown();
                return;
            }
            // All other packets are just consumed (counted in bandwidth stats)
        }
    }

    /**
     * Simulates gameplay by sending movement and shoot packets.
     * Movement in random directions causes the server to send back ObjectMovePackets,
     * UpdatePackets, LoadPackets, and LoadMapPackets — all high-bandwidth.
     * Shooting creates projectiles which further amplify server broadcasts.
     */
    private int moveTick = 0;
    private int movePhase = 0;

    private void simulateGameplay() {
        try {
            // Each bot gets a unique movement pattern offset by clientIndex
            // Bots cycle through directions every ~40 ticks (2 sec at 20 ticks/sec)
            this.moveTick++;
            if (this.moveTick % 40 == 0) {
                this.movePhase = (this.movePhase + 1) % 4;
            }
            // Offset by clientIndex so each bot goes a different direction
            int dirIdx = (this.movePhase + this.clientIndex) % 4;
            Cardinality[] dirs = { Cardinality.NORTH, Cardinality.EAST, Cardinality.SOUTH, Cardinality.WEST };
            Cardinality dir = dirs[dirIdx];
            PlayerMovePacket movePacket = new PlayerMovePacket(this.assignedPlayerId, dir.cardinalityId, true);
            this.outboundPacketQueue.add(movePacket);

            // Send shoot packet every other tick to create projectiles (more server broadcast data)
            if (RANDOM.nextBoolean()) {
                float angle = RANDOM.nextFloat() * 360f;
                float destX = (float) (Math.cos(Math.toRadians(angle)) * 200);
                float destY = (float) (Math.sin(Math.toRadians(angle)) * 200);
                PlayerShootPacket shootPacket = new PlayerShootPacket(
                        RANDOM.nextLong(), this.assignedPlayerId, 0, destX, destY, 0, 0);
                this.outboundPacketQueue.add(shootPacket);
            }

            // Send heartbeat periodically
            if (RANDOM.nextInt(20) == 0) {
                HeartbeatPacket heartbeat = HeartbeatPacket.from(this.assignedPlayerId, System.currentTimeMillis());
                this.outboundPacketQueue.add(heartbeat);
            }
        } catch (Exception e) {
            log.error("[Client-{}] Gameplay sim error: {}", this.clientIndex, e.getMessage());
        }
    }

    private void readPackets() {
        try {
            final InputStream stream = this.clientSocket.getInputStream();
            final int bytesRead = stream.read(this.remoteBuffer, this.remoteBufferIndex,
                    this.remoteBuffer.length - this.remoteBufferIndex);
            if (bytesRead == -1) throw new SocketException("end of stream");
            if (bytesRead > 0) {
                this.remoteBufferIndex += bytesRead;
                this.currentBytesReceived += bytesRead;
                this.totalBytesReceived += bytesRead;

                while (this.remoteBufferIndex >= 5) {
                    int packetLength = ((this.remoteBuffer[1] & 0xFF) << 24)
                            | ((this.remoteBuffer[2] & 0xFF) << 16)
                            | ((this.remoteBuffer[3] & 0xFF) << 8)
                            | (this.remoteBuffer[4] & 0xFF);
                    if (this.remoteBufferIndex < packetLength) break;

                    byte packetId = this.remoteBuffer[0];
                    int dataLength = packetLength - 5;
                    byte[] packetBytes = new byte[dataLength];
                    System.arraycopy(this.remoteBuffer, 5, packetBytes, 0, dataLength);
                    if (this.remoteBufferIndex > packetLength) {
                        System.arraycopy(this.remoteBuffer, packetLength, this.remoteBuffer, 0,
                                this.remoteBufferIndex - packetLength);
                    }
                    this.remoteBufferIndex -= packetLength;
                    this.totalPacketsReceived++;

                    try {
                        // Handle compressed packets (high bit set on packetId)
                        if (com.jrealm.net.core.PacketCompression.isCompressed(packetId)) {
                            packetId = com.jrealm.net.core.PacketCompression.getRealPacketId(packetId);
                            packetBytes = com.jrealm.net.core.PacketCompression.decompressPayload(packetBytes);
                        }
                        final Class<? extends Packet> packetClass = PacketType.valueOf(packetId);
                        if (packetClass == null) continue;
                        final Packet newPacket = IOService.readStream(packetClass, packetBytes);
                        newPacket.setSrcIp(this.clientSocket.getInetAddress().getHostAddress());
                        newPacket.setId(packetId);
                        this.inboundPacketQueue.add(newPacket);
                    } catch (Exception e) {
                        // Skip packets we can't deserialize — keep going
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Client-{}] Read error: {}", this.clientIndex, e.getMessage());
            this.shutdown();
        }
    }

    private void sendPackets() {
        OutputStream stream;
        try {
            stream = this.clientSocket.getOutputStream();
        } catch (Exception e) {
            return;
        }
        final DataOutputStream dos = new DataOutputStream(stream);
        while (!this.outboundPacketQueue.isEmpty() && !this.shutdown) {
            final Packet toSend = this.outboundPacketQueue.poll();
            if (toSend == null) continue;
            try {
                toSend.serializeWrite(dos);
                this.totalPacketsSent++;
            } catch (Exception e) {
                log.error("[Client-{}] Send error: {}", this.clientIndex, e.getMessage());
            }
        }
    }

    public void shutdown() {
        this.shutdown = true;
        if (this.sendPacketThread != null) this.sendPacketThread.setShutdown(true);
        if (this.readPacketThread != null) this.readPacketThread.setShutdown(true);
        if (this.gameplayThread != null) this.gameplayThread.setShutdown(true);
        try {
            if (this.clientSocket != null && !this.clientSocket.isClosed()) {
                this.clientSocket.close();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public void setSpawnNear(float x, float y) {
        this.spawnX = x;
        this.spawnY = y;
    }

    public boolean isLoggedIn() {
        return this.loggedIn;
    }

    public boolean isShutdown() {
        return this.shutdown;
    }

    public long resetBytesReceived() {
        long val = this.currentBytesReceived;
        this.currentBytesReceived = 0;
        return val;
    }
}
