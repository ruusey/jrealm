package com.openrealm.net.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.openrealm.game.data.GameDataManager;
import com.openrealm.game.entity.Player;
import com.openrealm.net.realm.RealmManagerServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import lombok.extern.slf4j.Slf4j;

/**
 * Tiny JDK-only admin listener that lets the data service trigger a live
 * {@link GameDataManager} reload without bouncing the game server.
 *
 * Endpoints:
 *   GET  /admin/ping              — health check, no auth required
 *   GET  /admin/playerCount       — current online player count
 *                                   (non-headless players in any realm),
 *                                   returned as {"count":N}. No auth — the
 *                                   data service aggregates this for the
 *                                   public /player-count endpoint.
 *   POST /admin/reloadGameData    — token-protected, calls
 *                                   {@link GameDataManager#loadGameData(boolean)}
 *
 * Auth is a single shared secret in the {@code OPENREALM_RELOAD_TOKEN} env
 * var (or {@code openrealm.reloadToken} system property). Callers must send
 * the same value in the {@code X-Reload-Token} header. If the env var is
 * unset, every reload request is rejected — fail-closed by design so a
 * misconfigured server doesn't accidentally accept anonymous reloads.
 *
 * Listens on the port from {@code OPENREALM_ADMIN_PORT} (default 8088).
 * The deploy script must open this port in the SG / firewall to the data
 * service IP only.
 */
@Slf4j
public class AdminHttpServer {

	private final HttpServer http;
	private final String expectedToken;
	private final RealmManagerServer realmManager;

	public AdminHttpServer(int port, RealmManagerServer realmManager) throws IOException {
		this.expectedToken = resolveToken();
		this.realmManager = realmManager;
		this.http = HttpServer.create(new InetSocketAddress(port), 0);
		this.http.createContext("/admin/ping", this::handlePing);
		this.http.createContext("/admin/playerCount", this::handlePlayerCount);
		this.http.createContext("/admin/reloadGameData", this::handleReload);
		this.http.setExecutor(null); // default executor — single-threaded is fine
	}

	public void start() {
		http.start();
		log.info("AdminHttpServer listening on :{} (token configured: {})",
			http.getAddress().getPort(), expectedToken != null);
		if (expectedToken == null) {
			log.warn("OPENREALM_RELOAD_TOKEN is unset — all /admin/reloadGameData requests will be rejected");
		}
	}

	private void handlePing(HttpExchange ex) throws IOException {
		String body = "{\"ok\":true,\"version\":\"" + ServerGameLogic.GAME_VERSION + "\"}";
		send(ex, 200, body);
	}

	/** Current online player count — REAL players across all realms.
	 *  Excludes headless (server-internal NPCs) and bots
	 *  (StressTestClient connections from /spawnbots) so the public
	 *  stat reflects actual humans. Returned as {@code {"count":N}}. */
	private void handlePlayerCount(HttpExchange ex) throws IOException {
		int count = 0;
		try {
			if (this.realmManager != null) {
				for (final Player p : this.realmManager.getPlayers()) {
					if (p == null || p.isHeadless() || p.isBot()) continue;
					count++;
				}
			}
		} catch (Exception e) {
			log.warn("playerCount lookup failed: {}", e.getMessage());
		}
		send(ex, 200, "{\"count\":" + count + "}");
	}

	private void handleReload(HttpExchange ex) throws IOException {
		if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
			send(ex, 405, "{\"ok\":false,\"reason\":\"method not allowed\"}");
			return;
		}
		String provided = ex.getRequestHeaders().getFirst("X-Reload-Token");
		if (expectedToken == null || provided == null || !constantTimeEquals(provided, expectedToken)) {
			log.warn("Rejected /admin/reloadGameData (bad or missing token) from {}", ex.getRemoteAddress());
			send(ex, 401, "{\"ok\":false,\"reason\":\"unauthorized\"}");
			return;
		}
		long t0 = System.currentTimeMillis();
		try {
			GameDataManager.loadGameData(true);
			long ms = System.currentTimeMillis() - t0;
			log.info("Reloaded game data in {} ms", ms);
			send(ex, 200, "{\"ok\":true,\"durationMs\":" + ms + "}");
		} catch (Exception e) {
			log.error("Reload failed", e);
			send(ex, 500, "{\"ok\":false,\"reason\":\"" + safe(e.getMessage()) + "\"}");
		}
	}

	private static void send(HttpExchange ex, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().add("Content-Type", "application/json");
		ex.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static String resolveToken() {
		String t = System.getProperty("openrealm.reloadToken");
		if (t == null || t.isBlank()) t = System.getenv("OPENREALM_RELOAD_TOKEN");
		return (t == null || t.isBlank()) ? null : t;
	}

	/** Length-equal constant-time compare to avoid token-length leaks. */
	private static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null || a.length() != b.length()) return false;
		int diff = 0;
		for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
		return diff == 0;
	}

	private static String safe(String s) {
		return s == null ? "" : s.replace("\"", "'").replace("\n", " ");
	}
}
