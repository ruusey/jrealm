package com.jrealm.net.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.SocketClient;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class SocketServer implements Runnable {
    public static final String LOCALHOST = "127.0.0.1";

    private ServerSocket serverSocket;
    private boolean shutdownSocketAccept = false;
    private boolean shutdownProcessing = false;

    private volatile Map<String, ProcessingThread> clients = new ConcurrentHashMap<>();

    public SocketServer(int port) {
        SocketServer.log.info("Creating local server at port {}", port);
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            SocketServer.log.error("Failed to create server socket. Reason: {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        Runnable socketAccept = () -> {
            SocketServer.log.info("Server now accepting inbound connections...");
            while (!this.shutdownSocketAccept) {
                try {
                    Socket socket = this.serverSocket.accept();
                    socket.setTcpNoDelay(true);

                    String remoteAddr = socket.getInetAddress().getHostAddress();
                    ProcessingThread processingThread = new ProcessingThread(this, socket);
                    this.clients.put(remoteAddr, processingThread);
                    processingThread.start();
                    SocketServer.log.info("Server accepted new connection from Remote Address {}", remoteAddr);
                } catch (Exception e) {
                    SocketServer.log.error("Failed to accept incoming socket connection, exiting...", e);
                }
            }
        };
        WorkerThread.submitAndForkRun(socketAccept);
    }
    
 
}
