package com.jrealm.net.entity;

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
        this.posX = obj.getPos().x;
        this.posY = obj.getPos().y;

        this.velX = obj.getDx();
        this.velY = obj.getDy();
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
}
