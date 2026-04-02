package com.jrealm.net.realm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assigns compact 16-bit short IDs to entities within a realm, allowing
 * movement packets to reference entities with 2 bytes instead of 8.
 * <p>
 * The server maintains one allocator per realm. When an entity spawns (and is
 * sent to clients via LoadPacket), it gets a short ID. The LoadPacket already
 * carries the full long ID, so the client can build the reverse mapping.
 * Movement packets then use the short ID only.
 * <p>
 * IDs are recycled when entities despawn. With 65,535 usable IDs (0 is reserved
 * as "no entity"), collisions are impossible since the server is the sole allocator
 * and a single realm will never have 65K simultaneous entities.
 */
public class ShortIdAllocator {
    // Next ID to try. Wraps around at 0xFFFF, skipping 0.
    private final AtomicInteger nextId = new AtomicInteger(1);

    // Bidirectional mappings
    private final Map<Long, Short> longToShort = new ConcurrentHashMap<>();
    private final Map<Short, Long> shortToLong = new ConcurrentHashMap<>();

    /**
     * Get or assign a short ID for the given long entity ID.
     * Thread-safe: concurrent calls for the same longId will return the same shortId.
     */
    public short getOrAssign(long longId) {
        Short existing = longToShort.get(longId);
        if (existing != null) {
            return existing;
        }
        return assign(longId);
    }

    private synchronized short assign(long longId) {
        // Double-check under lock
        Short existing = longToShort.get(longId);
        if (existing != null) {
            return existing;
        }

        // Find next available ID
        short shortId;
        int attempts = 0;
        do {
            int raw = nextId.getAndIncrement();
            // Wrap around, skip 0
            if (raw > 0xFFFF) {
                nextId.set(1);
                raw = nextId.getAndIncrement();
            }
            shortId = (short) (raw & 0xFFFF);
            attempts++;
            if (attempts > 0xFFFF) {
                throw new IllegalStateException("ShortIdAllocator exhausted: more than 65535 concurrent entities");
            }
        } while (shortId == 0 || shortToLong.containsKey(shortId));

        longToShort.put(longId, shortId);
        shortToLong.put(shortId, longId);
        return shortId;
    }

    /**
     * Release a short ID when an entity despawns, making it available for reuse.
     */
    public void release(long longId) {
        Short shortId = longToShort.remove(longId);
        if (shortId != null) {
            shortToLong.remove(shortId);
        }
    }

    /**
     * Look up the long ID for a given short ID. Returns -1 if not found.
     */
    public long toLong(short shortId) {
        Long longId = shortToLong.get(shortId);
        return longId != null ? longId : -1L;
    }

    /**
     * Look up the short ID for a given long ID. Returns 0 if not assigned.
     */
    public short toShort(long longId) {
        Short shortId = longToShort.get(longId);
        return shortId != null ? shortId : 0;
    }

    /**
     * Returns true if a short ID has been assigned for this long ID.
     */
    public boolean hasShortId(long longId) {
        return longToShort.containsKey(longId);
    }

    /**
     * Number of currently active mappings.
     */
    public int size() {
        return longToShort.size();
    }

    /**
     * Clear all mappings. Call when a realm is reset.
     */
    public void clear() {
        longToShort.clear();
        shortToLong.clear();
        nextId.set(1);
    }
}
