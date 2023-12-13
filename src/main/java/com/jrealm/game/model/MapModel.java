package com.jrealm.game.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper=false)
public class MapModel {
	private int mapId;
	private String mapName;
	private String mapKey;
	private int tileSize;
	private int width;
	private int height;
	
	private int[][] data;
}
