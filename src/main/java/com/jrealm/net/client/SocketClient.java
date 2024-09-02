package com.jrealm.net.client;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.game.util.TimedWorkerThread;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.BlankPacket;
import com.jrealm.net.Packet;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class SocketClient implements Runnable {
    private static final int BUFFER_CAPACITY = 65536 * 100;

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
        } catch (Exception e) {
            SocketClient.log.error("Failed to create ClientSocket, Reason: {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        this.startBandwidthMonitor();
        // this.monitorLastReceived();

        final Runnable readPackets = () -> {
            this.readPackets();
        };
        final Runnable sendPackets = () -> {
            this.sendPackets();
        };
        final TimedWorkerThread sendThread = new TimedWorkerThread(sendPackets, 64);
        final TimedWorkerThread readThread = new TimedWorkerThread(readPackets, 64);
        WorkerThread.submitAndForkRun(readThread, sendThread);
    }

    private void readPackets() {
        try {
            final InputStream stream = this.clientSocket.getInputStream();

            final int bytesRead = stream.read(this.remoteBuffer, this.remoteBufferIndex,
                    this.remoteBuffer.length - this.remoteBufferIndex);
            this.lastDataTime = System.currentTimeMillis();
            if (bytesRead == -1)
                throw new SocketException("end of stream");
            if (bytesRead > 0) {
                this.remoteBufferIndex += bytesRead;

                while (this.remoteBufferIndex >= 5) {
                    int packetLength = (ByteBuffer.allocate(4).put(this.remoteBuffer[1]).put(this.remoteBuffer[2])
                            .put(this.remoteBuffer[3]).put(this.remoteBuffer[4]).rewind()).getInt();
                    if (this.remoteBufferIndex < (packetLength)) {
                        break;
                    }
                    final byte packetId = this.remoteBuffer[0];
                    final byte[] packetBytes = new byte[packetLength];
                    System.arraycopy(this.remoteBuffer, 5, packetBytes, 0, packetLength);
                    if (this.remoteBufferIndex > packetLength) {
                        System.arraycopy(this.remoteBuffer, packetLength, this.remoteBuffer, 0,
                                this.remoteBufferIndex - packetLength);
                    }
                    this.currentBytesRecieved += packetLength;
                    this.remoteBufferIndex -= packetLength;
                    final BlankPacket newPacket = new BlankPacket(packetId, packetBytes);
                    newPacket.setSrcIp(this.clientSocket.getInetAddress().getHostAddress());
                    this.inboundPacketQueue.add(newPacket);
                }
            }
        } catch (Exception e) {
            SocketClient.log.error("Failed to parse client input. Reason {}", e);
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
        while (!this.outboundPacketQueue.isEmpty()) {
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
                    SocketClient.log.info("[CLIENT] current read rate = {} kbit/s",
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
