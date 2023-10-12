package com.jrealm.net.packet.client.temp;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.Player;
import com.jrealm.net.server.temp.EntityType;
import com.jrealm.net.server.temp.Packet;

public class ObjectMove extends Packet{
	
	private long entityId;
	private byte entityType;
	private float posX;
	private float posY;
	private float velX;
	private float velY;
	
	public ObjectMove(final byte id, final byte[] data) {
		super(id, data);
		try {
			final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
			this.readData(dis);
		}catch(Exception e) {
			//log.error("Failed to parse ObjectMove packet, Reason: {}", e.getMessage());
		}
	}
	
	public ObjectMove(GameObject obj, long objectId) {
		this.entityId = objectId;
		if(obj instanceof Enemy) {
			this.entityType = EntityType.ENEMY.getEntityTypeId();
		}else if (obj instanceof Player) {
			this.entityType = EntityType.PLAYER.getEntityTypeId();
		}else if(obj instanceof Bullet) {
			this.entityType = EntityType.BULLET.getEntityTypeId();
		}
		this.posX = obj.getPos().x;
		this.posY = obj.getPos().y;
		
		this.velX = obj.getDx();
		this.velY = obj.getDy();

	}
		
	@Override
	public void serializeWrite(DataOutputStream stream) throws Exception {
		if(this.getId()<1 || this.getData()== null || this.getData().length<5) throw new IllegalStateException("No Packet data available to write to DataOutputStream");
		
		this.addHeader(stream);
		stream.writeLong(this.entityId);
		stream.writeByte(this.entityType);
		stream.writeFloat(this.posX);
		stream.writeFloat(this.posY);
		stream.writeFloat(this.velX);
		stream.writeFloat(this.velY);
	}

	@Override
	public void readData(DataInputStream stream) throws Exception {
		if(stream==null || stream.available()<5) throw new IllegalStateException("No Packet data available to read from DataInputStream");

		this.entityId = stream.readLong();
		this.entityType = stream.readByte();
		this.posX = stream.readFloat();
		this.posY = stream.readFloat();
		this.velX = stream.readFloat();
		this.velY = stream.readFloat();
	}
	
	public EntityType getTargetEntityType() {
		return EntityType.valueOf(entityType);
	}
}
