package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.EntityType;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableFloat;
import com.jrealm.net.core.nettypes.SerializableLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Streamable
public class NetObjectMovement extends SerializableFieldType<NetObjectMovement> {
	@SerializableField(order = 0, type = SerializableLong.class)
    private long entityId;
	@SerializableField(order = 1, type = SerializableByte.class)
    private byte entityType;
	@SerializableField(order = 2, type = SerializableFloat.class)
    private float posX;
	@SerializableField(order = 3, type = SerializableFloat.class)
    private float posY;
	@SerializableField(order = 4, type = SerializableFloat.class)
    private float velX;
	@SerializableField(order = 5, type = SerializableFloat.class)
    private float velY;

    public NetObjectMovement(float posX, float posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public NetObjectMovement(GameObject obj) {
        this.entityId = obj.getId();
        if (obj instanceof Enemy) {
            this.entityType = EntityType.ENEMY.getEntityTypeId();
        } else if (obj instanceof Player) {
            this.entityType = EntityType.PLAYER.getEntityTypeId();
        } else if (obj instanceof Bullet) {
            this.entityType = EntityType.BULLET.getEntityTypeId();
        }
        // Quantize positions to 0.5px precision — reduces diff churn for sub-pixel movement
        // and improves compression ratio. Invisible in a pixel art game.
        this.posX = Math.round(obj.getPos().x * 2f) / 2f;
        this.posY = Math.round(obj.getPos().y * 2f) / 2f;

        this.velX = Math.round(obj.getDx() * 4f) / 4f;  // 0.25px/tick velocity precision
        this.velY = Math.round(obj.getDy() * 4f) / 4f;
    }

    public EntityType getTargetEntityType() {
    	final EntityType type = EntityType.valueOf(entityType);
        return type;
    }

    public boolean equals(NetObjectMovement other) {
        return this.entityId == other.getEntityId() && this.entityType == other.getEntityType()
                && this.posX == other.getPosX() && this.posY == other.getPosY() && this.velX == other.getVelX()
                && this.getVelY() == other.getVelY();
    }

    /** Hand-coded write: 25 bytes (8+1+4+4+4+4), bypasses reflection */
    @Override
    public int write(NetObjectMovement value, DataOutputStream stream) throws Exception {
        final NetObjectMovement v = (value == null) ? new NetObjectMovement() : value;
        stream.writeLong(v.entityId);
        stream.writeByte(v.entityType);
        stream.writeFloat(v.posX);
        stream.writeFloat(v.posY);
        stream.writeFloat(v.velX);
        stream.writeFloat(v.velY);
        return 25;
    }

    /** Hand-coded read: 25 bytes, bypasses reflection */
    @Override
    public NetObjectMovement read(DataInputStream stream) throws Exception {
        final NetObjectMovement m = new NetObjectMovement();
        m.entityId = stream.readLong();
        m.entityType = stream.readByte();
        m.posX = stream.readFloat();
        m.posY = stream.readFloat();
        m.velX = stream.readFloat();
        m.velY = stream.readFloat();
        return m;
    }
}
