package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.EntityType;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
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
public class ObjectMovement extends SerializableFieldType<ObjectMovement> {
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

    public ObjectMovement(float posX, float posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public ObjectMovement(GameObject obj) {
        this.entityId = obj.getId();
        if (obj instanceof Enemy) {
            this.entityType = EntityType.ENEMY.getEntityTypeId();
        } else if (obj instanceof Player) {
            this.entityType = EntityType.PLAYER.getEntityTypeId();
        } else if (obj instanceof Bullet) {
            this.entityType = EntityType.BULLET.getEntityTypeId();
        }
        this.posX = obj.getPos().x;
        this.posY = obj.getPos().y;

        this.velX = obj.getDx();
        this.velY = obj.getDy();
    }

    public EntityType getTargetEntityType() {
    	final EntityType type = EntityType.valueOf(entityType);
        return type;
    }

    @Override
    public int write(ObjectMovement value, DataOutputStream stream) throws Exception {
        return IOService.writeStream(value, stream);
    }

    @Override
    public ObjectMovement read(DataInputStream stream) throws Exception {
       return IOService.readStream(getClass(), stream);
    }

    public boolean equals(ObjectMovement other) {
        return this.entityId == other.getEntityId() && this.entityType == other.getEntityType()
                && this.posX == other.getPosX() && this.posY == other.getPosY() && this.velX == other.getVelX()
                && this.getVelY() == other.getVelY();
    }
}
