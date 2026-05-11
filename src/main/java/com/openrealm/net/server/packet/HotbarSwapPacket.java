package com.openrealm.net.server.packet;

import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.nettypes.SerializableByte;
import com.openrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Client → server when the player presses Shift+N to hotswap the ability in
 * hotbar slot {@code slot} forward through the class's pool to the next
 * unbound id. Server validates that the new binding is in the class's
 * AbilityTree.pool and not currently bound to another slot, then echoes the
 * change back via the next UpdatePacket (or a dedicated thin update — TBD).
 *
 * Phase 2A: packet only — no server handler yet. Phase 2C wires the input
 * path on both clients and the server-side mutation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Streamable
@AllArgsConstructor
@NoArgsConstructor
@PacketId(packetId = (byte) 39)
public class HotbarSwapPacket extends Packet {
    @SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;
    /** Hotbar slot to rotate (0..3). */
    @SerializableField(order = 1, type = SerializableByte.class)
    private byte slot;
    /**
     * Direction of cycle: +1 for forward (Shift+N), -1 for backward
     * (Shift+Alt+N or equivalent if we add it). Server clamps to {+1,-1}.
     */
    @SerializableField(order = 2, type = SerializableByte.class)
    private byte direction;
}
