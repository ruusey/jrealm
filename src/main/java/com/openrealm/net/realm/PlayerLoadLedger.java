package com.openrealm.net.realm;

import java.util.HashSet;
import java.util.Set;

/**
 * Per-player authoritative record of which entity IDs the server has
 * successfully delivered to a client and not yet unloaded.
 *
 * Replaces the previous snapshot-diff sync (which compared the
 * last-sent LoadPacket against the current spatial query and ran the
 * result through a realm-existence filter to suppress cap-trim flicker).
 * That model conflated two distinct conditions — "entity left
 * viewport" vs. "entity got cap-trimmed but still exists" — and silently
 * stripped legitimate unloads for entities that left the viewport while
 * still being alive in the realm, producing ghost enemies and stuck
 * loot bags on the client.
 *
 * The ledger model decouples the two: the server tracks exactly what
 * the client has, applies caps only to NEW loads (cap-trimmed IDs
 * simply aren't recorded as loaded and get picked up on a future tick),
 * and derives unloads from {@code ledger ∖ desired} directly. No filter,
 * no flicker, no orphaned entities.
 *
 * All access happens on the tick thread; no synchronization needed.
 */
public final class PlayerLoadLedger {
    public final Set<Long> players    = new HashSet<>();
    public final Set<Long> enemies    = new HashSet<>();
    public final Set<Long> bullets    = new HashSet<>();
    public final Set<Long> containers = new HashSet<>();
    public final Set<Long> portals    = new HashSet<>();

    public void clear() {
        this.players.clear();
        this.enemies.clear();
        this.bullets.clear();
        this.containers.clear();
        this.portals.clear();
    }
}
