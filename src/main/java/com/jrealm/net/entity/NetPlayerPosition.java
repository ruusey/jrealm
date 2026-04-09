package com.jrealm.net.entity;

import com.jrealm.game.contants.StatusEffectType;
import com.jrealm.game.entity.Player;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableFloat;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableString;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Streamable
@AllArgsConstructor
@NoArgsConstructor
public class NetPlayerPosition extends SerializableFieldType<NetPlayerPosition> {
    @SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;
    @SerializableField(order = 1, type = SerializableString.class)
    private String name;
    @SerializableField(order = 2, type = SerializableFloat.class)
    private float x;
    @SerializableField(order = 3, type = SerializableFloat.class)
    private float y;
    @SerializableField(order = 4, type = SerializableBoolean.class)
    private boolean teleportable;

    public static NetPlayerPosition from(Player player) {
        boolean canTeleport = !player.hasEffect(StatusEffectType.INVISIBLE)
                && !player.hasEffect(StatusEffectType.STASIS);
        return new NetPlayerPosition(
                player.getId(),
                player.getName() != null ? player.getName() : "Player",
                player.getPos().x,
                player.getPos().y,
                canTeleport);
    }
}
