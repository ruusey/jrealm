package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

import com.jrealm.game.entity.Bullet;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.*;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Streamable
public class NetBullet extends SerializableFieldType<NetBullet> {
	@SerializableField(order = 0, type = SerializableLong.class)
	private long id;
	@SerializableField(order = 1, type = SerializableInt.class)
	private int projectileId;
	@SerializableField(order = 2, type = SerializableShort.class)
	private short size;
	@SerializableField(order = 3, type = Vector2f.class)
	private Vector2f pos;
	@SerializableField(order = 4, type = SerializableFloat.class)
	private float dX;
	@SerializableField(order = 5, type = SerializableFloat.class)
	private float dY;
	@SerializableField(order = 6, type = SerializableFloat.class)
	private float angle;
	@SerializableField(order = 7, type = SerializableFloat.class)
	private float magnitude;
	@SerializableField(order = 8, type = SerializableFloat.class)
	private float range;
	@SerializableField(order = 9, type = SerializableShort.class)
	private short damage;
	@SerializableField(order = 10, type = SerializableShort.class, isCollection=true)
	private Short[] flags;
	@SerializableField(order = 11, type = SerializableBoolean.class)
	private boolean invert;
	@SerializableField(order = 12, type = SerializableLong.class)
	private long timeStep;
	@SerializableField(order = 13, type = SerializableShort.class)
	private short amplitude;
	@SerializableField(order = 14, type = SerializableShort.class)
	private short frequency;
	@SerializableField(order = 15, type = SerializableLong.class)
	private long createdTime;
	
	@Override
	public NetBullet read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetBullet value, DataOutputStream stream) throws Exception {
		IOService.writeStream(value, stream);
	}
	
	public Bullet asBullet() {
		Bullet bullet = new Bullet();
		bullet.setId(this.id);
		bullet.setProjectileId(this.projectileId);
		bullet.setSize(this.size);
		bullet.setPos(this.pos);
		bullet.setDx(this.dX);
		bullet.setDy(this.dY);
		bullet.setAngle(this.angle);
		bullet.setMagnitude(this.magnitude);
		bullet.setRange(this.range);
		bullet.setDamage(this.damage);
		bullet.setFlags(Arrays.asList(this.flags));
		bullet.setInvert(this.invert);
		bullet.setTimeStep(this.timeStep);
		bullet.setAmplitude(this.amplitude);
		bullet.setFrequency(this.frequency);
		bullet.setCreatedTime(this.createdTime);
		return bullet;
	}
}
