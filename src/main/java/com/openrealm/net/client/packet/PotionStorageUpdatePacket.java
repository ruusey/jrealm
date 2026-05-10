package com.openrealm.net.client.packet;

import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.nettypes.SerializableLong;
import com.openrealm.net.entity.NetGameItem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Server -> client. Authoritative refresh of the player's potion-storage
 * contents after each successful move. Mirrors the role of UpdatePacket for
 * inventory but scoped to the 32-slot potion-storage container.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Streamable
@NoArgsConstructor
@AllArgsConstructor
@PacketId(packetId = (byte) 35)
public class PotionStorageUpdatePacket extends Packet {
    @SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;
    @SerializableField(order = 1, type = NetGameItem.class, isCollection = true)
    private NetGameItem[] items;
}
