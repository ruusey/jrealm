package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

public enum EffectType {
	INVISIBLE((short) 0), HEALING((short) 1), PARALYZED((short) 2);

	public static Map<Short, EffectType> map = new HashMap<>();
	static {
		for (EffectType e : EffectType.values()) {
			EffectType.map.put((short) e.effectId, e);
		}
	}
	public short effectId;

	EffectType(short effectId) {
		this.effectId = effectId;
	}

	public static EffectType valueOf(short classId) {
		return EffectType.map.get(classId);
	}

}
