package com.jrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.graphics.Sprite;

public enum LootTier {
    CHEST(-1), BROWN(0), PURPLE(1), CYAN(2), BLUE(3), WHITE(4), GRAVE(5);

    public static Map<Byte, LootTier> map = new HashMap<>();
    static {
	for (LootTier cc : LootTier.values()) {
	    LootTier.map.put(cc.tierId, cc);
	}
    }

    public byte tierId;

    LootTier(int classId) {
	this.tierId = (byte) classId;
    }

    public static LootTier valueOf(byte tier) {
	return LootTier.map.get(tier);
    }

    public static Sprite getLootSprite(byte tier) {
	if (LootTier.valueOf(tier).equals(GRAVE))
	    return GameDataManager.getGraveSprite();
	if (LootTier.valueOf(tier).equals(CHEST))
	    return GameDataManager.getChestSprite();
	return GameDataManager.getLootSprite(tier);
    }
}
