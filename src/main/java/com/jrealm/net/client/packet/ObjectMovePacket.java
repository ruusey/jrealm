package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.net.EntityType;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper=true)
@Slf4j
public class ObjectMovePacket extends Packet {

	private long entityId;
	private byte entityType;
	private float posX;
	private float posY;
	private float velX;
	private float velY;

	public ObjectMovePacket() {
		
	}
	
	public ObjectMovePacket(final byte id, final byte[] data) {
		super(id, data);
		try {
			this.readData(data);
		} catch (Exception e) {
			log.error("Failed to parse ObjectMove packet, Reason: {}", e);
		}
	}

	public ObjectMovePacket(GameObject obj, long objectId) {
		this.entityId = objectId;
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

	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if (this.getId() < 1 || this.getData() == null || this.getData().length < 5)
			throw new IllegalStateException("No Packet data available to write to DataOutputStream");

		this.addHeader(stream);
		stream.writeLong(this.entityId);
		stream.writeByte(this.entityType);
		stream.writeFloat(this.posX);
		stream.writeFloat(this.posY);
		stream.writeFloat(this.velX);
		stream.writeFloat(this.velY);
	}

	@Override
	public void readData(byte[] data) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		if (dis == null || dis.available() < 5)
			throw new IllegalStateException("No Packet data available to read from DataInputStream");

		this.entityId = dis.readLong();
		this.entityType = dis.readByte();
		this.posX = dis.readFloat();
		this.posY = dis.readFloat();
		this.velX = dis.readFloat();
		this.velY = dis.readFloat();
	}

	public EntityType getTargetEntityType() {
		return EntityType.valueOf(entityType);
	}

	public ObjectMovePacket fromGameObject(GameObject obj) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		DataOutputStream stream = new DataOutputStream(byteStream);

		stream.writeLong((long) obj.getId());
		if (obj instanceof Enemy) {
			stream.writeByte(EntityType.ENEMY.getEntityTypeId());
		} else if (obj instanceof Player) {
			stream.writeByte(EntityType.PLAYER.getEntityTypeId());
		} else if (obj instanceof Bullet) {
			stream.writeByte(EntityType.BULLET.getEntityTypeId());
		}		
		stream.writeFloat(obj.getPos().x);
		stream.writeFloat(obj.getPos().y);
		stream.writeFloat(obj.getDx());
		stream.writeFloat(obj.getDy());

		return new ObjectMovePacket(PacketType.OBJECT_MOVE.getPacketId(), byteStream.toByteArray());
	}
}
