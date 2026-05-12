package com.openrealm.net.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

/**
 * Phase 4 — Party MVP.
 *
 * Tracks party membership and pending invites in memory. Not persisted across
 * server restarts (intentional for MVP: party state is short-lived and the
 * 4-player cap is small enough that re-invites on restart are fine).
 *
 * Concurrency: every public method is safe to call from the server tick
 * loop and from chat-command handler threads. Internal maps use
 * ConcurrentHashMap; the per-party roster is wrapped in a synchronized set so
 * roster snapshots are consistent.
 */
@Slf4j
public class PartyManager {
    /** Maximum members per party (including the leader). */
    public static final int MAX_PARTY_SIZE = 4;
    /** Pending invites expire after this many ms. */
    public static final long INVITE_TIMEOUT_MS = 60_000L;

    private final AtomicLong nextPartyId = new AtomicLong(1L);

    /** partyId → set of player ids in that party. */
    private final Map<Long, Set<Long>> rosters = new ConcurrentHashMap<>();
    /** playerId → partyId. 0 = not in a party. */
    private final Map<Long, Long> playerParty = new ConcurrentHashMap<>();
    /** invitedPlayerId → {inviterPlayerId, expiresAtMs}. */
    private final Map<Long, long[]> pendingInvites = new ConcurrentHashMap<>();

    /** Returns the partyId for {@code playerId}, or 0 if not in a party. */
    public long getPartyId(long playerId) {
        final Long v = playerParty.get(playerId);
        return v == null ? 0L : v;
    }

    /** Returns a snapshot of party member ids (including the caller), or
     *  an empty list if the player isn't in a party. */
    public List<Long> getPartyMembers(long playerId) {
        final long pid = getPartyId(playerId);
        if (pid == 0L) return Collections.emptyList();
        final Set<Long> roster = rosters.get(pid);
        if (roster == null) return Collections.emptyList();
        synchronized (roster) {
            return new ArrayList<>(roster);
        }
    }

    /** True if both players are in the same party. */
    public boolean inSameParty(long a, long b) {
        if (a == b) return true;
        final long pa = getPartyId(a);
        return pa != 0L && pa == getPartyId(b);
    }

    /**
     * Record a pending invite from {@code inviterId} to {@code inviteeId}.
     * Returns null on success, or a short error reason. Auto-creates a party
     * for the inviter if they aren't in one yet (one-person "lobby").
     */
    public String invite(long inviterId, long inviteeId) {
        if (inviterId == inviteeId) return "you cannot invite yourself";
        // Already in same party
        if (inSameParty(inviterId, inviteeId)) return "already in your party";
        // Invitee already in some other party
        if (getPartyId(inviteeId) != 0L) return "that player is already in a party";
        // Bootstrap a party for the inviter if needed
        long pid = getPartyId(inviterId);
        if (pid == 0L) {
            pid = nextPartyId.getAndIncrement();
            final Set<Long> roster = Collections.synchronizedSet(new HashSet<>());
            roster.add(inviterId);
            rosters.put(pid, roster);
            playerParty.put(inviterId, pid);
            log.info("[PARTY] auto-create partyId={} for {}", pid, inviterId);
        }
        // Check capacity
        final Set<Long> roster = rosters.get(pid);
        synchronized (roster) {
            if (roster.size() >= MAX_PARTY_SIZE) return "party is full (" + MAX_PARTY_SIZE + ")";
        }
        pendingInvites.put(inviteeId, new long[]{ inviterId, System.currentTimeMillis() + INVITE_TIMEOUT_MS });
        log.info("[PARTY] invite from {} to {} (pending)", inviterId, inviteeId);
        return null;
    }

    /**
     * Accept the most-recent invite. Returns the partyId on success, or 0 if
     * there's no pending invite or the inviter has since gone offline / left
     * their party.
     */
    public long accept(long inviteeId) {
        final long[] inv = pendingInvites.remove(inviteeId);
        if (inv == null) return 0L;
        if (System.currentTimeMillis() > inv[1]) return 0L;  // expired
        final long inviterId = inv[0];
        final long pid = getPartyId(inviterId);
        if (pid == 0L) return 0L;
        final Set<Long> roster = rosters.get(pid);
        if (roster == null) return 0L;
        synchronized (roster) {
            if (roster.size() >= MAX_PARTY_SIZE) return 0L;
            roster.add(inviteeId);
        }
        playerParty.put(inviteeId, pid);
        log.info("[PARTY] {} accepted invite from {} -> partyId={}", inviteeId, inviterId, pid);
        return pid;
    }

    /**
     * Decline a pending invite. Returns the inviter id (so the server can
     * notify them), or 0 if no invite existed.
     */
    public long decline(long inviteeId) {
        final long[] inv = pendingInvites.remove(inviteeId);
        if (inv == null) return 0L;
        log.info("[PARTY] {} declined invite from {}", inviteeId, inv[0]);
        return inv[0];
    }

    /**
     * Remove {@code playerId} from their party. Auto-disbands single-member
     * parties. Returns the partyId they left (0 if they weren't in one).
     */
    public long leave(long playerId) {
        final Long pid = playerParty.remove(playerId);
        if (pid == null || pid == 0L) return 0L;
        final Set<Long> roster = rosters.get(pid);
        if (roster != null) {
            synchronized (roster) {
                roster.remove(playerId);
                if (roster.isEmpty()) {
                    rosters.remove(pid);
                    log.info("[PARTY] partyId={} disbanded (last member left)", pid);
                }
            }
        }
        return pid;
    }

    /** Drop expired pending invites — called periodically by the tick loop. */
    public void evictExpiredInvites() {
        final long now = System.currentTimeMillis();
        for (Map.Entry<Long, long[]> e : new HashMap<>(pendingInvites).entrySet()) {
            if (now > e.getValue()[1]) pendingInvites.remove(e.getKey());
        }
    }
}
