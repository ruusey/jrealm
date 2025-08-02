package com.jrealm.net.server.packet;

import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Packet;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.nettypes.SerializableFloat;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Streamable
@NoArgsConstructor
@AllArgsConstructor
public class PlayerShootPacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
    private long projectileId;
	@SerializableField(order = 1, type = SerializableLong.class)
    private long entityId;
	@SerializableField(order = 2, type = SerializableInt.class)
    private int projectileGroupId;
	@SerializableField(order = 3, type = SerializableFloat.class)
    private float destX;
	@SerializableField(order = 4, type = SerializableFloat.class)
    private float destY;
	@SerializableField(order = 5, type = SerializableFloat.class)
    private float srcX;
	@SerializableField(order = 6, type = SerializableFloat.class)
    private float srcY;

    public static PlayerShootPacket from(long newProjectileId, Player p, Vector2f dest) throws Exception {
    	final PlayerShootPacket data = new PlayerShootPacket();
    	data.projectileId = newProjectileId;
    	data.entityId  = p.getId();
    	data.projectileGroupId = p.getWeaponId();
    	if (dest == null) {
    		data.destX = -1;
    		data.destY = -1;
        } else {
            data.destX = dest.x;
            data.destY = dest.y;
        }
    	
    	data.srcX = p.getPos().x;
    	data.srcY = p.getPos().y;
    	return data;
    }

	@Override
	public byte getPacketId() {
		// TODO Auto-generated method stub
		return (byte) 6;
	}
}
