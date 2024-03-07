package com.jrealm.game.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class SpriteModel {
	private String spriteKey;
	private int row;
	private int col;
	private int spriteSize;
	private String angleOffset;
}
