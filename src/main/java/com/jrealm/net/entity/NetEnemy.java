package com.jrealm.net.entity;

import com.jrealm.game.entity.Enemy;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableFloat;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableShort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Streamable
@AllArgsConstructor
@NoArgsConstructor
public class NetEnemy extends SerializableFieldType<NetEnemy> {
	
	@SerializableField(order = 0, type = SerializableLong.class)
	private long id;
	@SerializableField(order = 1, type = SerializableInt.class)
	private int enemyId;
	@SerializableField(order = 2, type = SerializableInt.class)
	private int weaponId;
	@SerializableField(order = 3, type = SerializableShort.class)
	private short size;
	@SerializableField(order = 4, type = Vector2f.class)
	private Vector2f pos;
	@SerializableField(order = 5, type = SerializableFloat.class)
	private float dX;
	@SerializableField(order = 6, type = SerializableFloat.class)
	private float dY;
	
	public Enemy asEnemy() {
		final Enemy e = new Enemy();
		e.setId(this.getId());
		e.setEnemyId(this.getEnemyId());
		e.setWeaponId(this.getWeaponId());
		e.setSize(this.getSize());
		e.setPos(this.getPos());
		e.setDx(this.getDX());
		e.setDy(this.getDY());
		return e;
	}
}
