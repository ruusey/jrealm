package com.openrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.openrealm.game.contants.EntityType;
import com.openrealm.game.entity.Bullet;
import com.openrealm.game.entity.Enemy;
import com.openrealm.game.entity.Entity;
import com.openrealm.game.entity.GameObject;
import com.openrealm.game.entity.Player;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.SerializableFieldType;
import com.openrealm.net.core.nettypes.SerializableByte;
import com.openrealm.net.core.nettypes.SerializableFloat;
import com.openrealm.net.core.nettypes.SerializableLong;

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
	@SerializableField(order = 6, type = SerializableByte.class)
    private byte flags;

    /** Flag bit: entity is currently in an attack/shoot animation. */
    private static final byte FLAG_ATTACKING = 0x01;

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

        this.velX = Math.round(obj.getDx() * 8f) / 8f;  // 0.125px/tick velocity precision
        this.velY = Math.round(obj.getDy() * 8f) / 8f;

        // Pack boolean flags into a single byte
        this.flags = 0;
        if (obj instanceof Entity && ((Entity) obj).isAttacking()) this.flags |= FLAG_ATTACKING;
    }

    public boolean isAttackingFlag() {
        return (this.flags & FLAG_ATTACKING) != 0;
    }

    public EntityType getTargetEntityType() {
    	final EntityType type = EntityType.valueOf(entityType);
        return type;
    }

    public boolean equals(NetObjectMovement other) {
        return this.entityId == other.getEntityId() && this.entityType == other.getEntityType()
                && this.posX == other.getPosX() && this.posY == other.getPosY() && this.velX == other.getVelX()
                && this.getVelY() == other.getVelY() && this.flags == other.getFlags();
    }

    /** Hand-coded write: 26 bytes (8+1+4+4+4+4+1), bypasses reflection */
    @Override
    public int write(NetObjectMovement value, DataOutputStream stream) throws Exception {
        final NetObjectMovement v = (value == null) ? new NetObjectMovement() : value;
        stream.writeLong(v.entityId);
        stream.writeByte(v.entityType);
        stream.writeFloat(v.posX);
        stream.writeFloat(v.posY);
        stream.writeFloat(v.velX);
        stream.writeFloat(v.velY);
        stream.writeByte(v.flags);
        return 26;
    }

    /** Hand-coded read: 26 bytes, bypasses reflection */
    @Override
    public NetObjectMovement read(DataInputStream stream) throws Exception {
        final NetObjectMovement m = new NetObjectMovement();
        m.entityId = stream.readLong();
        m.entityType = stream.readByte();
        m.posX = stream.readFloat();
        m.posY = stream.readFloat();
        m.velX = stream.readFloat();
        m.velY = stream.readFloat();
        m.flags = stream.readByte();
        return m;
    }
}
