package com.openrealm.net.client;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.openrealm.game.contants.PacketType;
import com.openrealm.net.NetConstants;
import com.openrealm.net.Packet;
import com.openrealm.net.core.IOService;
import com.openrealm.net.core.PacketCompression;
import com.openrealm.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class SocketClient implements Runnable {
	// 80 KB buffer for the client
    private static final int BUFFER_CAPACITY = 65536 * 10;

    public static String PLAYER_EMAIL = null;
    public static String SERVER_ADDR = null;
    public static String CHARACTER_UUID = null;
    public static String PLAYER_PASSWORD = null;

    private Socket clientSocket;
    private boolean shutdown = false;
    private byte[] remoteBuffer = new byte[SocketClient.BUFFER_CAPACITY];
    private int remoteBufferIndex = 0;
    private long lastUpdate = 0;
    private long previousTime = 0;
    private long lastDataTime = System.currentTimeMillis();
    private long currentBytesRecieved = 0;
    private volatile Queue<Packet> inboundPacketQueue = new ConcurrentLinkedQueue<>();
    private volatile Queue<Packet> outboundPacketQueue = new ConcurrentLinkedQueue<>();

    public SocketClient(String targetHost, int port) {
        try {
            this.clientSocket = new Socket(targetHost, port);
            this.clientSocket.setTcpNoDelay(true);
            // 100ms read timeout so the read loop can check the shutdown flag
            // and the profiler doesn't report it as 100% CPU
            this.clientSocket.setSoTimeout(100);
        } catch (Exception e) {
            SocketClient.log.error("Failed to create ClientSocket, Reason: {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        this.startBandwidthMonitor();

        // Read thread: blocks naturally on stream.read(), no polling needed
        final Runnable readLoop = () -> {
            while (!this.shutdown) {
                try {
                    this.readPackets();
                } catch (Exception e) {
                    this.shutdown = true;
                }
            }
        };

        // Send thread: drains outbound queue, sleeps briefly when empty
        final Runnable sendLoop = () -> {
            while (!this.shutdown) {
                try {
                    if (!this.outboundPacketQueue.isEmpty()) {
                        this.sendPackets();
                    } else {
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    this.shutdown = true;
                }
            }
        };

        WorkerThread.submitAndForkRun(readLoop, sendLoop);
    }

    private void readPackets() {
        try {
            final InputStream stream = this.clientSocket.getInputStream();

            final int bytesRead;
            try {
                bytesRead = stream.read(this.remoteBuffer, this.remoteBufferIndex,
                        this.remoteBuffer.length - this.remoteBufferIndex);
            } catch (java.net.SocketTimeoutException e) {
                return; // No data within timeout — normal, just retry
            }
            this.lastDataTime = Instant.now().toEpochMilli();
            if (bytesRead == -1)
                throw new SocketException("end of stream");
            if (bytesRead > 0) {
                this.remoteBufferIndex += bytesRead;

                while (this.remoteBufferIndex >= 5) {
                    int packetLength = ((this.remoteBuffer[1] & 0xFF) << 24)
                                     | ((this.remoteBuffer[2] & 0xFF) << 16)
                                     | ((this.remoteBuffer[3] & 0xFF) << 8)
                                     |  (this.remoteBuffer[4] & 0xFF);
                    if (this.remoteBufferIndex < (packetLength)) {
                        break;
                    }
                    byte packetId = this.remoteBuffer[0];
                    final int dataLength = packetLength - NetConstants.PACKET_HEADER_SIZE;
                    byte[] packetBytes = new byte[dataLength];
                    System.arraycopy(this.remoteBuffer, NetConstants.PACKET_HEADER_SIZE, packetBytes, 0, dataLength);
                    if (this.remoteBufferIndex > packetLength) {
                        System.arraycopy(this.remoteBuffer, packetLength, this.remoteBuffer, 0,
                                this.remoteBufferIndex - packetLength);
                    }
                    this.currentBytesRecieved += packetLength;
                    this.remoteBufferIndex -= packetLength;
                    // Decompress if compression flag is set
                    if (PacketCompression.isCompressed(packetId)) {
                        packetId = PacketCompression.getRealPacketId(packetId);
                        packetBytes = PacketCompression.decompressPayload(packetBytes);
                    }
                    final Class<? extends Packet> packetClass = PacketType.valueOf(packetId);
                    final Packet newPacket = IOService.readStream(packetClass, packetBytes);
                    newPacket.setSrcIp(this.clientSocket.getInetAddress().getHostAddress());
                    newPacket.setId(packetId);
                    this.inboundPacketQueue.add(newPacket);
                }
            }
        } catch (Exception e) {
            this.shutdown = true;
            SocketClient.log.error("Failed to parse client input. Reason {}", e.getMessage());
        }
    }

    private void sendPackets() {
        OutputStream stream = null;
        try {
            stream = this.clientSocket.getOutputStream();
        } catch (Exception e) {
            return;
        }
        final DataOutputStream dos = new DataOutputStream(stream);
        while (!this.outboundPacketQueue.isEmpty() && !this.shutdown) {
            final Packet toSend = this.outboundPacketQueue.remove();
            try {
                toSend.serializeWrite(dos);
            } catch (Exception e) {
                final String remoteAddr = this.clientSocket.getInetAddress().getHostAddress();
                SocketClient.log.error("Failed to send Packet to remote addr {}, Reason: {}", remoteAddr, e);
            }
        }
    }

    /**
     * Enqueues a Packet instance to be sent to the remote during the next
     * processing cycle
     * 
     * @param packet Packet to send to remote server
     * @throws Exception
     */
    public void sendRemote(Packet packet) throws Exception {
        if (this.clientSocket == null)
            throw new Exception("Client socket is null/not yet established");

        this.outboundPacketQueue.add(packet);
    }

    @SuppressWarnings("unused")
    private void monitorLastReceived() {
        final Runnable monitorLastRecieved = () -> {
            while (!this.shutdown) {
                try {
                    if ((System.currentTimeMillis() - this.lastDataTime) > 5000) {
                        this.shutdown = true;
                    }
                } catch (Exception e) {
                    log.error("Failed to wait during last recieved monitor. Reason: {}", e);
                }
            }
        };
        WorkerThread.submitAndForkRun(monitorLastRecieved);
    }

    public void startBandwidthMonitor() {
        final Runnable run = () -> {
            while (!this.shutdown) {
                try {
                    final long bytesRead = this.currentBytesRecieved;
                    SocketClient.log.debug("[CLIENT] current read rate = {} kbit/s",
                            (float) (bytesRead / 1024.0f) * 8.0f);
                    this.currentBytesRecieved = 0;
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.error("Failed to monitor bandwidth usage. Reason: {}", e);
                }
            }
        };
        WorkerThread.submitAndForkRun(run);
    }

    public void close() {
        this.shutdown = true;
        try {
            if (this.clientSocket != null && !this.clientSocket.isClosed()) {
                this.clientSocket.close();
            }
        } catch (Exception e) {
            SocketClient.log.error("Failed to close client socket. Reason: {}", e.getMessage());
        }
    }

    public static String getLocalAddr() throws Exception {
        final String[] split = InetAddress.getLocalHost().toString().split("/");
        String addr = null;
        if (split.length > 1) {
            addr = split[1];
        } else {
            addr = split[0];
        }
        return addr;
    }
}
