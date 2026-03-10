package com.jrealm.net.test;

import java.util.ArrayList;
import java.util.List;

import com.jrealm.net.core.IOService;
import com.jrealm.util.WorkerThread;

import lombok.extern.slf4j.Slf4j;

/**
 * Network stress test harness that spawns N clients connecting to the game server.
 * Each client logs in with real account credentials, then simulates gameplay
 * (movement + shooting) to stress test server bandwidth under load.
 *
 * Usage:
 *   java NetworkStressTest <host> <port> <numClients> <email> <password> <characterUuid> [durationSec]
 *
 * Example (50 clients, 60 second test):
 *   java NetworkStressTest 127.0.0.1 2222 50 test@test.com pass123 char-uuid-here 60
 *
 * For testing with multiple distinct accounts, modify the email/characterUuid per client
 * or extend this class to read from a CSV file of test accounts.
 *
 * What it measures:
 *   - Aggregate inbound bandwidth (server -> all clients) in Mbit/s
 *   - Per-client bandwidth in kbit/s
 *   - Total packets received/sent across all clients
 *   - Connection success rate and login success rate
 */
@Slf4j
public class NetworkStressTest {

    private final String host;
    private final int port;
    private final int numClients;
    private final String email;
    private final String password;
    private final String characterUuid;
    private final int durationSeconds;

    private final List<StressTestClient> clients = new ArrayList<>();

    public NetworkStressTest(String host, int port, int numClients, String email, String password,
            String characterUuid, int durationSeconds) {
        this.host = host;
        this.port = port;
        this.numClients = numClients;
        this.email = email;
        this.password = password;
        this.characterUuid = characterUuid;
        this.durationSeconds = durationSeconds;
    }

    public void run() throws Exception {
        IOService.mapSerializableData();
        log.info("=== JRealm Network Stress Test ===");
        log.info("Target: {}:{}", this.host, this.port);
        log.info("Clients: {}", this.numClients);
        log.info("Duration: {}s", this.durationSeconds);
        log.info("==================================");

        // Spawn clients with staggered connections to avoid thundering herd
        log.info("Spawning {} clients (staggered 100ms apart)...", this.numClients);
        for (int i = 0; i < this.numClients; i++) {
            // Use indexed email to allow per-client accounts: test+0@test.com, test+1@test.com, etc.
            // If your account service doesn't support +aliases, all clients share the same account
            String clientEmail = this.email;
            String clientCharUuid = this.characterUuid;

            StressTestClient client = new StressTestClient(i, this.host, this.port,
                    clientEmail, this.password, clientCharUuid);
            this.clients.add(client);
            WorkerThread.submitAndForkRun(client);

            // Stagger connections to avoid SYN flood
            Thread.sleep(100);
        }

        // Wait for logins to complete
        log.info("Waiting for logins to complete...");
        Thread.sleep(5000);

        int loggedIn = 0;
        int failed = 0;
        for (StressTestClient c : this.clients) {
            if (c.isLoggedIn()) loggedIn++;
            else if (c.isShutdown()) failed++;
        }
        log.info("Login results: {} logged in, {} failed, {} pending",
                loggedIn, failed, this.numClients - loggedIn - failed);

        // Run bandwidth monitoring for the test duration
        log.info("Starting bandwidth measurement for {}s...", this.durationSeconds);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (this.durationSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            Thread.sleep(2000);

            long totalBytesThisInterval = 0;
            int activeClients = 0;
            long totalPacketsRx = 0;
            long totalPacketsTx = 0;

            for (StressTestClient c : this.clients) {
                if (!c.isShutdown()) {
                    activeClients++;
                    totalBytesThisInterval += c.resetBytesReceived();
                    totalPacketsRx += c.getTotalPacketsReceived();
                    totalPacketsTx += c.getTotalPacketsSent();
                }
            }

            // Bytes received over 2 second interval
            float kbitsPerSec = (totalBytesThisInterval / 1024.0f) * 8.0f / 2.0f;
            float mbitsPerSec = kbitsPerSec / 1024.0f;
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;

            log.info("[t={}s] Active: {} | Bandwidth: {} kbit/s ({} Mbit/s) | Packets RX: {} TX: {}",
                    elapsed, activeClients,
                    String.format("%.1f", kbitsPerSec), String.format("%.2f", mbitsPerSec),
                    totalPacketsRx, totalPacketsTx);
        }

        // Print final summary
        log.info("=== STRESS TEST COMPLETE ===");
        long grandTotalRx = 0;
        long grandTotalTx = 0;
        long grandTotalPktsRx = 0;
        long grandTotalPktsTx = 0;
        int finalActive = 0;

        for (StressTestClient c : this.clients) {
            grandTotalRx += c.getTotalBytesReceived();
            grandTotalTx += c.getTotalBytesSent();
            grandTotalPktsRx += c.getTotalPacketsReceived();
            grandTotalPktsTx += c.getTotalPacketsSent();
            if (!c.isShutdown()) finalActive++;
        }

        log.info("Total data received: {} MB", grandTotalRx / (1024 * 1024));
        log.info("Total data sent:     {} MB", grandTotalTx / (1024 * 1024));
        log.info("Total packets RX:    {}", grandTotalPktsRx);
        log.info("Total packets TX:    {}", grandTotalPktsTx);
        log.info("Clients survived:    {}/{}", finalActive, this.numClients);
        float avgBandwidth = (grandTotalRx / 1024.0f) * 8.0f / this.durationSeconds;
        log.info("Avg aggregate bandwidth: {} kbit/s ({} Mbit/s)",
                String.format("%.1f", avgBandwidth),
                String.format("%.2f", avgBandwidth / 1024.0f));

        // Shutdown all clients
        log.info("Shutting down clients...");
        for (StressTestClient c : this.clients) {
            c.shutdown();
        }
        log.info("Done.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.out.println("Usage: NetworkStressTest <host> <port> <numClients> <email> <password> <characterUuid> [durationSec]");
            System.out.println("Example: NetworkStressTest 127.0.0.1 2222 50 test@test.com pass123 my-char-uuid 60");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int numClients = Integer.parseInt(args[2]);
        String email = args[3];
        String password = args[4];
        String characterUuid = args[5];
        int duration = args.length > 6 ? Integer.parseInt(args[6]) : 30;

        NetworkStressTest test = new NetworkStressTest(host, port, numClients, email, password, characterUuid, duration);
        test.run();
    }
}
