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
 * Server -> client acknowledgement that the player can open their Potion
 * Storage UI. Sent in response to InteractTilePacket on a tile whose
 * interactionType == "potion_storage". Carries the current 32-slot contents
 * so the client can render them immediately without a follow-up fetch.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Streamable
@NoArgsConstructor
@AllArgsConstructor
@PacketId(packetId = (byte) 34)
public class OpenPotionStoragePacket extends Packet {
    @SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;
    @SerializableField(order = 1, type = NetGameItem.class, isCollection = true)
    private NetGameItem[] items;
}
