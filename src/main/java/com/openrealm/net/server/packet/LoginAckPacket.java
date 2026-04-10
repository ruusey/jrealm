package com.openrealm.net.server.packet;

import com.openrealm.net.Packet;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.PacketId;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Streamable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@PacketId(packetId = (byte) 22)
public class LoginAckPacket extends Packet {
    @SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;

    public static LoginAckPacket from(long playerId) {
        return new LoginAckPacket(playerId);
    }
}
