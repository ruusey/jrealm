package com.openrealm.net.client.packet;

import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.nettypes.SerializableByte;
import com.openrealm.net.core.nettypes.SerializableFloat;
import com.openrealm.net.core.nettypes.SerializableInt;
import com.openrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Server → clients in range when a player starts casting a non-instant ability.
 * Each visible client (including the caster) uses this to render a cast bar
 * over the player and disable input/movement for the cast duration. Cleared
 * when the cast resolves (no explicit "finish" packet — the projectile spawn
 * or status apply implicitly ends the cast on the client).
 *
 * Phase 2A: packet only — no consumers yet. Phase 2B's useAbility refactor
 * sends this when an ability's baseCastMs > 0.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Streamable
@AllArgsConstructor
@NoArgsConstructor
@PacketId(packetId = (byte) 38)
public class AbilityCastStartPacket extends Packet {
    @SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;
    @SerializableField(order = 1, type = SerializableInt.class)
    private int abilityId;
    @SerializableField(order = 2, type = SerializableByte.class)
    private byte slot;
    /** Cast duration in ms — already mutated by SPD-derived cast speed reduction. */
    @SerializableField(order = 3, type = SerializableInt.class)
    private int durationMs;
    @SerializableField(order = 4, type = SerializableFloat.class)
    private float worldTargetX;
    @SerializableField(order = 5, type = SerializableFloat.class)
    private float worldTargetY;
}
