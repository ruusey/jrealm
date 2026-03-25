package com.jrealm.net.server;

import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketClientSession extends ClientSession {

    private final WebSocket wsConnection;

    public WebSocketClientSession(WebSocket ws, String clientKey) {
        super(null, clientKey); // No SocketChannel for WebSocket sessions
        this.wsConnection = ws;
    }

    @Override
    public boolean flushWrites() {
        try {
            byte[] frame;
            while ((frame = this.getWriteQueue().poll()) != null) {
                if (this.wsConnection.isOpen()) {
                    this.wsConnection.send(frame);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("[WS] Write failed for client {}. Reason: {}", this.getClientKey(), e.getMessage());
            this.setShutdownProcessing(true);
            return true;
        }
    }

    @Override
    public boolean isConnected() {
        return this.wsConnection != null && this.wsConnection.isOpen();
    }

    @Override
    public void close() {
        try {
            if (this.wsConnection != null && this.wsConnection.isOpen()) {
                this.wsConnection.close();
            }
        } catch (Exception e) {
            log.error("[WS] Failed to close WebSocket for client {}. Reason: {}", this.getClientKey(), e.getMessage());
        }
        this.getPacketQueue().clear();
        this.getWriteQueue().clear();
    }

    /**
     * Called when a binary WebSocket message arrives.
     * Injects raw packet frame bytes into the standard packet pipeline.
     */
    public void handleWebSocketMessage(ByteBuffer message) {
        byte[] data = new byte[message.remaining()];
        message.get(data);
        this.injectData(data, 0, data.length);
    }
}
