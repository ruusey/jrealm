package com.openrealm.net.entity;

import com.openrealm.game.entity.Player;
import com.openrealm.game.math.Vector2f;
import com.openrealm.net.Streamable;
import com.openrealm.net.core.SerializableField;
import com.openrealm.net.core.SerializableFieldType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.openrealm.net.core.nettypes.*;

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
	// Compact short ID for bandwidth-efficient movement packets.
	// Assigned by ShortIdAllocator when entity enters a realm.
	@SerializableField(order = 9, type = SerializableShort.class)
	private short shortId;
	@SerializableField(order = 10, type = SerializableString.class)
	private String chatRole;

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
		p.setChatRole(this.chatRole);
		return p;
	}
}
