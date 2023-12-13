package com.jrealm.game.tiles.blocks;

import lombok.Data;

@Data
public class TileData {
	private float rarity;
	private int spriteIndex;
	private int[] vary;
	private byte hasCollision;
	private byte slows;
	private byte damaging;

	public TileData(float rarity, int spriteIndex) {
		this.rarity = rarity;
		this.spriteIndex = spriteIndex;
	}

	public TileData(float rarity, int spriteIndex, int[] vary) {
		this.rarity = rarity;
		this.spriteIndex = spriteIndex;
		this.vary = vary;
	}

	public int generate() {
		double random = Math.random();
		if((this.vary != null) && (random > 0.9))
			return this.vary[((int) (random * 100)) % (this.vary.length )];

		return this.spriteIndex;
	}
}