package com.jrealm.net.client.packet;

import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.PacketId;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.entity.NetPlayerPosition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@Streamable
@AllArgsConstructor
@NoArgsConstructor
@PacketId(packetId = (byte) 23)
public class GlobalPlayerPositionPacket extends Packet {
    @SerializableField(order = 0, type = NetPlayerPosition.class, isCollection = true)
    private NetPlayerPosition[] players;

    public static GlobalPlayerPositionPacket from(NetPlayerPosition[] players) {
        return new GlobalPlayerPositionPacket(players);
    }
}
