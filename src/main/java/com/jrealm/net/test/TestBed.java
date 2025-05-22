package com.jrealm.net.test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.jrealm.net.core.IOService;
import com.jrealm.util.WorkerThread;

public class TestBed {
    private static Boolean runServer = true;
    private static Boolean runClient = true;
    // Only one client will be in here but this is how to handle multple
    private static volatile Map<String, Socket> clients = new ConcurrentHashMap<>();

    // Thread pool to run stuff in other threads
    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10, Executors.privilegedThreadFactory());
    private static final Integer PORT = 4199;

    public static void main(String[] args) throws Exception {
    	IOService.mapSerializableData();
    	ServerConnectionManager mgr = new ServerConnectionManager(12345);
    	WorkerThread.submitAndForkRun(mgr);
    	TestClient cli = new TestClient("127.0.0.1", 12345);
    	WorkerThread.submitAndForkRun(cli);

//        try {
//        	final ServerConnectionManager accepter = new ServerConnectionManager(PORT);
//            final ServerSocket server = getServerSocket(PORT);
//            runServerSocket(server);
//            runServerRoutine(server);
//            final Socket client = getClientSocket("127.0.0.1", PORT);
//            runClientRoutine(client);
//        } catch (Exception e) {
//            e.printStackTrace();
//            shutDown();
//        }

    }

    private static void shutDown() {
        runServer = false;
        runClient = false;
    }

    // Attempts to accept a inbound socket connection and put it in the client map
    private static void runServerSocket(final ServerSocket server) {
        final Runnable runServerSocketSeparateThread = () -> {
            System.out.println("Begin ServerSocket accept");
            while (runServer) {
                try {
                    final Socket socket = server.accept();
                    socket.setTcpNoDelay(true);
                    final String remoteAddr = socket.getInetAddress().getHostAddress();
                    clients.put(remoteAddr, socket);
                    System.out.println("Server accepted new connection from " + remoteAddr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        executor.execute(runServerSocketSeparateThread);
    }

    // For all clients connected read the inbound data and response
    private static void runServerRoutine(final ServerSocket server) {
        final Runnable runServerSocketSeparateThread = () -> {
            while (runServer) {
                try {
                    for (Entry<String, Socket> entry : clients.entrySet()) {
                        final Socket clientSocket = entry.getValue();
                        final InputStream inputStreamFromClient = clientSocket.getInputStream();
                        if (inputStreamFromClient.available() > 0) {
                            final int avail = inputStreamFromClient.available();
                            final byte[] readBytes = new byte[avail];
                            final int numBytesRead = inputStreamFromClient.read(readBytes);
                            final String inputContent = new String(readBytes, StandardCharsets.UTF_8);
                            System.out.println("Server recieved message(" + numBytesRead + " bytes)" + " from client " + entry.getKey());
                            final OutputStream outputStreamToClient = clientSocket.getOutputStream();
                            byte[] responseToWrite = (inputContent + " FROM SERVER!").getBytes();
                            outputStreamToClient.write(responseToWrite);
                            outputStreamToClient.flush();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        executor.execute(runServerSocketSeparateThread);
    }

    // Periodically attempt to send a message to a clients output stream
    private static void runClientRoutine(final Socket client) {
        final Runnable runServerSocketSeparateThread = () -> {
            while (runClient) {
                try {
                    final byte[] bytesToWrite = (UUID.randomUUID().toString()+", "+UUID.randomUUID().toString()).getBytes();
                    final OutputStream outputStream = client.getOutputStream();
                    // No stream established yet
                    if (outputStream == null)
                        continue;
                    final InputStream inputStream = client.getInputStream();
                    outputStream.write(bytesToWrite);
                    outputStream.flush();

                    if (inputStream.available() > 0) {
                        int avail = inputStream.available();
                        final byte[] readBytes = new byte[avail];
                        final int numBytesRead = inputStream.read(readBytes);
                        final String inputContent = new String(readBytes, StandardCharsets.UTF_8);
                        System.out.println("Client recieved message (" + numBytesRead + " bytes)" + inputContent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        executor.execute(runServerSocketSeparateThread);
    }

    private static Socket getClientSocket(String targetHost, Integer targetPort) throws Exception {
        final Socket socket = new Socket(targetHost, targetPort);
        socket.setTcpNoDelay(true);
        return socket;
    }

    private static ServerSocket getServerSocket(Integer hostPort) throws Exception {
        final ServerSocket server = new ServerSocket(hostPort);
        return server;
    }
}
