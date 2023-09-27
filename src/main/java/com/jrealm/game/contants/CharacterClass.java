package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

public enum CharacterClass {
	ROGUE(0),
	ARCHER(1),
	WIZARD(2),
	PRIEST(3),
	WARRIOR(4),
	KNIGHT(5),
	PALLADIN(6);

	public static Map<Integer, CharacterClass> map = new HashMap<>();
	static {
		for (CharacterClass cc : CharacterClass.values()) {
			CharacterClass.map.put(cc.classId, cc);
		}
	}
	public int classId;
	CharacterClass(int classId) {
		this.classId = classId;
	}

	public static CharacterClass valueOf(int classId) {
		return CharacterClass.map.get(classId);
	}

}
