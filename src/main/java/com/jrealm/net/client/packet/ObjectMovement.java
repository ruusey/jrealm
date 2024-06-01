package com.jrealm.net.client.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.EntityType;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.net.Streamable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectMovement implements Streamable<ObjectMovement> {
    private long entityId;
    private byte entityType;
    private float posX;
    private float posY;
    private float velX;
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
	EntityType type = EntityType.valueOf(entityType);
	return type;
    }

    @Override
    public void write(DataOutputStream stream) throws Exception {
	stream.writeLong(this.entityId);
	stream.writeByte(this.entityType);
	stream.writeFloat(this.posX);
	stream.writeFloat(this.posY);
	stream.writeFloat(this.velY);
	stream.writeFloat(this.velX);
    }

    @Override
    public ObjectMovement read(DataInputStream stream) throws Exception {
	final long id = stream.readLong();
	final byte entityType = stream.readByte();
	final float posX = stream.readFloat();
	final float posY = stream.readFloat();
	final float velX = stream.readFloat();
	final float velY = stream.readFloat();
	return new ObjectMovement(id, entityType, posX, posY, velX, velY);
    }

    public boolean equals(ObjectMovement other) {
	return this.entityId == other.getEntityId() && this.entityType == other.getEntityType()
		&& this.posX == other.getPosX() && this.posY == other.getPosY() && this.velX == other.getVelX()
		&& this.getVelY() == other.getVelY();
    }
}
