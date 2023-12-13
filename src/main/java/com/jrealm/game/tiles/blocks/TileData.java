package com.jrealm.game.tiles.blocks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TileData {
	private float rarity;
	private byte hasCollision;
	private byte slows;
	private byte damaging;

	public TileData(float rarity) {
		this.rarity = rarity;
	}
	
	public boolean hasCollision() {
		return this.hasCollision != 0;
	}
	
	public boolean slows() {
		return this.slows != 0;
	}
	
	public boolean damaging() {
		return this.damaging!=0;
	}
	
	public static TileData withCollision() {
		return new TileData(0.0f, (byte)1, (byte)0, (byte)0);
	}
	
	public static TileData withoutCollision() {
		return new TileData(0.0f, (byte)0, (byte)0, (byte)0);
	}
}