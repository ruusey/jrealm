package com.jrealm.game.util;

import java.util.HashMap;
import java.util.Map;

public enum Cardinality {
	NORTH((byte) 0), SOUTH((byte) 1), EAST((byte) 2), WEST((byte) 3), NONE((byte) 4);

	public static final Map<Byte, Cardinality> map = new HashMap<>();

	static {
		for (Cardinality c : Cardinality.values()) {
			map.put(c.getCardinalityId(), c);
		}
	}
	public byte cardinalityId;

	Cardinality(byte cardinalityId) {
		this.cardinalityId = cardinalityId;
	}

	public byte getCardinalityId() {
		return this.cardinalityId;
	}

	public static Cardinality valueOf(byte cardinalityId) {
		return map.get(cardinalityId);
	}

	public boolean equals(Cardinality other) {
		return this.ordinal() == other.ordinal();
	}
}
