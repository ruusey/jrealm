package com.jrealm.game.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameItem {
	private int itemId;
	private String uid;
	private String name;
	private String description;
	
	
}
