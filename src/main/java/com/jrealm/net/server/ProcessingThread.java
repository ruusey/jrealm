package com.jrealm.net.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private boolean shutdownProcessing = false;
    private byte[] remoteBuffer = new byte[ProcessingThread.BUFFER_CAPACITY];
    private int remoteBufferIndex = 0;
    private long lastUpdate = 0;
    private long previousTime = 0;
    private long lastDataTime = System.currentTimeMillis();
    private final Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();
    private boolean handshakeComplete = false;

    public ProcessingThread(SocketServer server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
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
					nPacket.setSrcIp(this.clientSocket.getInetAddress().getHostAddress());
					this.packetQueue.add(nPacket);
                }
            }
        } catch (Exception e) {
            this.shutdownProcessing = true;
            ProcessingThread.log.error("Failed to parse client input {}", e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void monitorLastReceived() {
        Runnable monitorLastRecieved = () -> {
            while (!this.shutdownProcessing) {
                try {
                    if ((System.currentTimeMillis() - this.lastDataTime) > 5000) {
                        this.server.getClients().remove(this.clientSocket.getInetAddress().getHostAddress());
                        this.shutdownProcessing = true;
                    }
                } catch (Exception e) {

                }

            }
        };
        WorkerThread.submitAndForkRun(monitorLastRecieved);
    }
}
