package com.jrealm.net.entity;

import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.jrealm.net.core.nettypes.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Streamable
public class NetPlayer extends SerializableFieldType<NetPlayer>{
	@SerializableField(order = 0, type = SerializableLong.class)
	private long id;
	@SerializableField(order = 1, type = SerializableString.class)
	private String name;
	@SerializableField(order = 2, type = SerializableString.class)
	private String accountUuid;
	@SerializableField(order = 3, type = SerializableString.class)
	private String characterUuid;
	@SerializableField(order = 4, type = SerializableInt.class)
	private int classId;
	@SerializableField(order = 5, type = SerializableShort.class)
	private short size;
	@SerializableField(order = 6, type = Vector2f.class)
	private Vector2f pos;
	@SerializableField(order = 7, type = SerializableFloat.class)
	private float dX;
	@SerializableField(order = 8, type = SerializableFloat.class)
	private float dY;
	
	public Player toPlayer() {
		Player p = new Player();
		p.setId(this.id);
		p.setName(this.name);
		p.setAccountUuid(this.accountUuid);
		p.setCharacterUuid(this.characterUuid);
		p.setClassId(this.classId);
		p.setSize(this.size);
		p.setPos(this.pos);
		p.setDx(this.dX);
		p.setDy(this.dY);
		return p;
	}
}
