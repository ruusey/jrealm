package com.jrealm.game.model;

import java.util.List;
import com.jrealm.game.tile.RoomShapeTemplate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class DungeonGenerationParams {
	private int minRooms;
	private int maxRooms;
	private int minRoomWidth;
	private int maxRoomWidth;
	private int minRoomHeight;
	private int maxRoomHeight;
	List<RoomShapeTemplate> shapeTemplates;
}
