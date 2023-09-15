package com.jrealm.game.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Enemy extends SpriteModel {
	private int enemyId;

	private String name;

	private int xp;


}
