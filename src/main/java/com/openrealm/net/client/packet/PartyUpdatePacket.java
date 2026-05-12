package com.openrealm.net.client.packet;

import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.nettypes.SerializableLong;
import com.openrealm.net.entity.NetPartyMember;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Phase 4 — Party MVP. Server → every party member, ~2Hz and on roster change.
 *
 * Carries the FULL roster snapshot every time (members are bounded at 4, so
 * the wire cost is negligible). The recipient identifies themselves by
 * matching {@code playerId} on a member entry and renders the others as the
 * teammate UI rows.
 *
 * {@code partyId == 0} signals "you are no longer in a party" — the client
 * uses this to tear down the party UI when the player leaves / is kicked /
 * the party disbands.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Streamable
@NoArgsConstructor
@PacketId(packetId = (byte) 41)
public class PartyUpdatePacket extends Packet {
    @SerializableField(order = 0, type = SerializableLong.class)
    private long partyId;
    @SerializableField(order = 1, type = NetPartyMember.class, isCollection = true)
    private NetPartyMember[] members;
}
