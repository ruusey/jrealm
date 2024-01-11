package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

public enum EntityType {
	PLAYER((byte)0), ENEMY((byte)1), BULLET((byte)2);
	
	private static Map<Byte, EntityType> map = new HashMap<>();
	
	private byte entityTypeId;
	
	static {
		for(EntityType et : EntityType.values()) {
			map.put(et.getEntityTypeId(), et);
		}
	}
	private EntityType(byte entityTypeId) {
		this.entityTypeId = entityTypeId;
	}
	
	public byte getEntityTypeId() {
		return this.entityTypeId;
	}
	
	public static EntityType valueOf(byte value) {
		return map.get(Byte.valueOf(value));
	}
	
	public static EntityType valueOf(int value) {
		return map.get(Byte.valueOf((byte)value));
	}
}
