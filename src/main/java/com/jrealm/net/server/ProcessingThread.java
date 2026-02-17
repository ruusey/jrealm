package com.jrealm.net.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;

import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.io.LittleEndianDataInputStream;
import com.jrealm.game.contants.PacketType;
import com.jrealm.net.Packet;
import com.jrealm.net.core.IOService;
import com.jrealm.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessingThread extends Thread {
	// 80 KB buffer per client
    private static final int BUFFER_CAPACITY = 65536 * 10;

    private Socket clientSocket;
    private SocketServer server;
    private String clientKey;
    private boolean shutdownProcessing = false;
    private byte[] remoteBuffer = new byte[ProcessingThread.BUFFER_CAPACITY];
    private int remoteBufferIndex = 0;
    private long lastUpdate = 0;
    private long previousTime = 0;
    private long lastDataTime = System.currentTimeMillis();
    private final Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();
    private boolean handshakeComplete = false;

    public ProcessingThread(SocketServer server, Socket clientSocket, String clientKey) {
        this.server = server;
        this.clientSocket = clientSocket;
        this.clientKey = clientKey;
    }

    @Override
    public void run() {
        // this.monitorLastReceived();
        while (!this.shutdownProcessing) {
            try {
                this.enqueueClientPackets();
            } catch (Exception e) {
                ProcessingThread.log.error("Failed to parse client input {}", e.getMessage());
            }
        }
        try {
            this.clientSocket.close();
            this.remoteBuffer = null;
            this.packetQueue.clear();
            log.info("Client socket succesfully closed");
        } catch (IOException e1) {
            log.error("Failed to close client socket");
        }
        log.info("Processing thread for user shutdown.");
    }

    private void enqueueClientPackets() {
        try {
            InputStream stream = this.clientSocket.getInputStream();
            int bytesRead = stream.read(this.remoteBuffer, this.remoteBufferIndex,
                    this.remoteBuffer.length - this.remoteBufferIndex);
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
                    byte[] packetBytes = new byte[packetLength];
                    System.arraycopy(this.remoteBuffer, 5, packetBytes, 0, packetLength);
                    if (this.remoteBufferIndex > packetLength) {
                        System.arraycopy(this.remoteBuffer, packetLength, this.remoteBuffer, 0,
                                this.remoteBufferIndex - packetLength);
                    }
					this.remoteBufferIndex -= packetLength;
					final Class<?> packetClass = PacketType.valueOf(packetId);
					final Packet nPacket = IOService.readStream(packetClass, packetBytes);
					nPacket.setId(packetId);
					nPacket.setSrcIp(this.clientKey);
					this.packetQueue.add(nPacket);
                }
            }
        } catch (Exception e) {
            //this.shutdownProcessing = true;
            //ProcessingThread.log.error("Failed to parse client input {}", e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void monitorLastReceived() {
        Runnable monitorLastRecieved = () -> {
            while (!this.shutdownProcessing) {
                try {
                    if ((Instant.now().toEpochMilli() - this.lastDataTime) > 5000) {
                        this.server.getClients().remove(this.clientKey);
                        this.shutdownProcessing = true;
                    }
                } catch (Exception e) {

                }

            }
        };
        WorkerThread.submitAndForkRun(monitorLastRecieved);
    }
}
