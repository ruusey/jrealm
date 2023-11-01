package com.jrealm.game.util;

import java.util.HashMap;
import java.util.Map;

public enum Cardinality {
	NORTH((byte) 0), SOUTH((byte) 1), EAST((byte) 2), WEST((byte) 3);

	public byte cardinalityId;

	public static final Map<Byte, Cardinality> map = new HashMap<>();

	Cardinality(byte cardinalityId) {
		this.cardinalityId = cardinalityId;
	}

	public static Cardinality valueOf(byte cardinalityId) {
		return map.get(cardinalityId);
	}
}
