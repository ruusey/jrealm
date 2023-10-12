package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

import com.jrealm.game.entity.Player;

public enum CharacterClass {
	ROGUE(0),
	ARCHER(1),
	WIZARD(2),
	PRIEST(3),
	WARRIOR(4),
	KNIGHT(5),
	PALLADIN(6),

	ROBE(-1),
	LEATHER(-2),
	HEAVY(-3),
	ALL(-4);

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

	public static boolean isRobeClass(CharacterClass c) {
		return c.equals(CharacterClass.WIZARD) || c.equals(CharacterClass.PRIEST);
	}

	public static boolean isLeatherClass(CharacterClass c) {
		return c.equals(CharacterClass.ARCHER) || c.equals(CharacterClass.ROGUE);
	}

	public static boolean isHeavyClass(CharacterClass c) {
		return c.equals(CharacterClass.WARRIOR) || c.equals(CharacterClass.PALLADIN) || c.equals(CharacterClass.KNIGHT);
	}

	public static boolean isPlayerHeavyClass(Player p) {
		return CharacterClass.isHeavyClass(CharacterClass.valueOf(p.getClassId()));
	}

	public static boolean isPlayerLeatherClass(Player p) {
		return CharacterClass.isLeatherClass(CharacterClass.valueOf(p.getClassId()));
	}

	public static boolean isPlayerRobeClass(Player p) {
		return CharacterClass.isRobeClass(CharacterClass.valueOf(p.getClassId()));
	}

	public static CharacterClass getPlayerCharacterClass(Player p) {
		return CharacterClass.valueOf(p.getClassId());
	}

	public static boolean isValidUser(Player p, byte requiredClass) {
		CharacterClass req = CharacterClass.valueOf(requiredClass);
		boolean result = false;
		switch(req) {
		case ROBE:
			result = CharacterClass.isPlayerRobeClass(p);
			break;
		case LEATHER:
			result = CharacterClass.isPlayerLeatherClass(p);
			break;
		case HEAVY:
			result = CharacterClass.isPlayerHeavyClass(p);
			break;
		case ALL:
			result = true;
			break;
		default:
			result = req.equals(CharacterClass.getPlayerCharacterClass(p));
		}
		return result;
	}

	public static CharacterClass valueOf(int classId) {
		return CharacterClass.map.get(classId);
	}

}
