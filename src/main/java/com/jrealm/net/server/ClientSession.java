package com.jrealm.net.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.core.IOService;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ClientSession {
    private static final int BUFFER_CAPACITY = 65536 * 10;
    private static final int READ_BUFFER_SIZE = 8192;

    private SocketChannel channel;
    private String clientKey;
    private boolean shutdownProcessing = false;
    private boolean handshakeComplete = false;
    private byte[] remoteBuffer = new byte[ClientSession.BUFFER_CAPACITY];
    private int remoteBufferIndex = 0;
    private ByteBuffer readBuffer = ByteBuffer.allocateDirect(READ_BUFFER_SIZE);
    private final Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<byte[]> writeQueue = new ConcurrentLinkedQueue<>();
    private ByteBuffer pendingWrite = null;

    public ClientSession(SocketChannel channel, String clientKey) {
        this.channel = channel;
        this.clientKey = clientKey;
    }

    public void readFromChannel() throws IOException {
        this.readBuffer.clear();
        int bytesRead = this.channel.read(this.readBuffer);
        if (bytesRead == -1) {
            this.shutdownProcessing = true;
            return;
        }
        if (bytesRead > 0) {
            this.readBuffer.flip();
            int remaining = this.readBuffer.remaining();
            if (this.remoteBufferIndex + remaining > this.remoteBuffer.length) {
                log.warn("[NIO] Buffer overflow for client {}, discarding data", this.clientKey);
                this.remoteBufferIndex = 0;
                return;
            }
            this.readBuffer.get(this.remoteBuffer, this.remoteBufferIndex, remaining);
            this.remoteBufferIndex += remaining;
            this.parsePackets();
        }
    }

    private void parsePackets() {
        while (this.remoteBufferIndex >= 5) {
            int packetLength = ((this.remoteBuffer[1] & 0xFF) << 24)
                             | ((this.remoteBuffer[2] & 0xFF) << 16)
                             | ((this.remoteBuffer[3] & 0xFF) << 8)
                             |  (this.remoteBuffer[4] & 0xFF);
            if (this.remoteBufferIndex < packetLength) {
                break;
            }
            byte packetId = this.remoteBuffer[0];
            byte[] packetBytes = new byte[packetLength];
            System.arraycopy(this.remoteBuffer, 5, packetBytes, 0, packetLength);
            if (this.remoteBufferIndex > packetLength) {
                System.arraycopy(this.remoteBuffer, packetLength, this.remoteBuffer, 0,
                        this.remoteBufferIndex - packetLength);
            }
            this.remoteBufferIndex -= packetLength;
            try {
                final Class<?> packetClass = PacketType.valueOf(packetId);
                final Packet nPacket = IOService.readStream(packetClass, packetBytes);
                nPacket.setId(packetId);
                nPacket.setSrcIp(this.clientKey);
                this.packetQueue.add(nPacket);
            } catch (Exception e) {
                log.error("[NIO] Failed to parse packet from client {}. Reason: {}", this.clientKey, e.getMessage());
            }
        }
    }

    public void enqueueWrite(byte[] frame) {
        this.writeQueue.add(frame);
    }

    public boolean flushWrites() {
        try {
            // First try to finish any pending partial write
            if (this.pendingWrite != null) {
                this.channel.write(this.pendingWrite);
                if (this.pendingWrite.hasRemaining()) {
                    return false;
                }
                this.pendingWrite = null;
            }

            // Drain the write queue
            byte[] frame;
            while ((frame = this.writeQueue.poll()) != null) {
                ByteBuffer buf = ByteBuffer.wrap(frame);
                this.channel.write(buf);
                if (buf.hasRemaining()) {
                    this.pendingWrite = buf;
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            log.error("[NIO] Write failed for client {}. Reason: {}", this.clientKey, e.getMessage());
            this.shutdownProcessing = true;
            return true;
        }
    }

    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen() && this.channel.isConnected();
    }

    public void close() {
        try {
            if (this.channel != null && this.channel.isOpen()) {
                this.channel.close();
            }
        } catch (IOException e) {
            log.error("[NIO] Failed to close channel for client {}. Reason: {}", this.clientKey, e.getMessage());
        }
        this.remoteBuffer = null;
        this.packetQueue.clear();
        this.writeQueue.clear();
        this.pendingWrite = null;
    }
}
