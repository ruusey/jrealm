package com.jrealm.game.model;

import com.jrealm.game.entity.item.Stats;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CharacterClassModel {
	private int spriteOffset;
	private int classId;
	private String className;
	private Stats baseStats;

}
