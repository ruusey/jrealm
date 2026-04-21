package com.openrealm.net.client.packet;

import java.util.ArrayList;
import java.util.List;

import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.entity.NetCompactMovement;
import com.openrealm.net.entity.NetObjectMovement;
import com.openrealm.net.realm.ShortIdAllocator;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Bandwidth-efficient movement packet using 2-byte short entity IDs and
 * quantized velocity (15 bytes/entity vs 26 bytes in ObjectMovePacket).
 * <p>
 * Clients resolve short IDs to long IDs using the mapping established
 * in LoadPacket (NetPlayer.shortId / NetEnemy.shortId).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Streamable
@NoArgsConstructor
@PacketId(packetId = (byte) 25)
public class CompactMovePacket extends Packet {

    @SerializableField(order = 0, type = NetCompactMovement.class, isCollection = true)
    private NetCompactMovement[] movements;

    /**
     * Build a CompactMovePacket from a list of dead reckoning corrections,
     * mapping long entity IDs to short IDs via the allocator.
     */
    public static CompactMovePacket from(List<NetObjectMovement> corrections, ShortIdAllocator allocator) throws Exception {
        final List<NetCompactMovement> compact = new ArrayList<>();
        for (final NetObjectMovement m : corrections) {
            short shortId = allocator.toShort(m.getEntityId());
            if (shortId != 0) {
                compact.add(new NetCompactMovement(shortId, m));
            }
        }
        final CompactMovePacket packet = new CompactMovePacket();
        packet.setMovements(compact.toArray(new NetCompactMovement[0]));
        return packet;
    }
}
