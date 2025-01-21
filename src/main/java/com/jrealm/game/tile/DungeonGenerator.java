package com.jrealm.game.tile;

import java.util.Arrays;
import java.util.List;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.TileModel;
import com.jrealm.game.util.GameObjectUtils;
import com.jrealm.game.util.Graph;
import com.jrealm.net.realm.Realm;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DungeonGenerator {
	private int width;
	private int height;
	private int tileSize;
	private int minRooms;
	private int maxRooms;
	private int minRoomWidth;
	private int maxRoomWidth;
	private int minRoomHeight;
	private int maxRoomHeight;
	private List<RoomShapeTemplate> shapeTemplates;
	private Graph<TileMap> dungeon;

	public DungeonGenerator(int width, int height, int tileSize, int minRooms, int maxRooms, int minRoomWidth,
			int maxRoomWidth, int minRoomHeight, int maxRoomHeight, List<RoomShapeTemplate> shapeTemplates) {
		this.width = width;
		this.height = height;
		this.tileSize = tileSize;
		this.minRooms = minRooms;
		this.maxRooms = maxRooms;
		this.minRoomWidth = minRoomWidth;
		this.maxRoomWidth = maxRoomWidth;
		this.minRoomHeight = minRoomHeight;
		this.maxRoomHeight = maxRoomHeight;
		this.shapeTemplates = shapeTemplates;
		this.dungeon = new Graph<>();
	}

	public List<TileMap> generateDungeon() {

		final TileMap baseLayer = new TileMap(this.tileSize, this.width, this.height);
		final TileMap collisionLayer = new TileMap(this.tileSize, this.width, this.height);

		baseLayer.fill(0);
		collisionLayer.fill(0);
		final int numRooms = this.minRooms + Realm.RANDOM.nextInt((this.maxRooms - this.minRooms) + 1);

		TileMap previousRoom = null;
		int previousRoomOffsetX = 0;
		int previousRoomOffsetY = 0;
		for (int i = 0; i < numRooms; i++) {

			final TileMap room = this.getRoom(this.tileSize, this.minRoomWidth, this.maxRoomWidth, this.minRoomHeight,
					this.maxRoomHeight, this.shapeTemplates);
			// Try to group the rooms within 10+(roomWidth*2) tiles of the previous room
			// WIP
			final int xModifier = Realm.RANDOM.nextInt(2) == 0 ? -2 : 2;
			final int yModifier = Realm.RANDOM.nextInt(2) == 0 ? -2 : 2;

			final int rangeX = (previousRoomOffsetX + (room.getWidth() * xModifier)) <= 0 ? 1
					: previousRoomOffsetX + (room.getWidth() * xModifier);
			final int rangeY = (previousRoomOffsetY + (room.getHeight() * yModifier)) <= 0 ? 1
					: previousRoomOffsetY + (room.getHeight() * yModifier);

			int offsetX = 15 + Realm.RANDOM.nextInt(rangeX);
			int offsetY = 15 + Realm.RANDOM.nextInt(rangeY);

			if (offsetX < 0) {
				offsetX = 0;
			}
			if (offsetX > this.width - room.getWidth()) {
				offsetX = this.width - room.getWidth();
			}

			if (offsetY < 0) {
				offsetY = 0;
			}
			if (offsetY > this.height - room.getHeight()) {
				offsetY = this.height - room.getHeight();
			}
			// Place the room anywhere in the dungeon
//            final int offsetX = Realm.RANDOM.nextInt(this.width-room.getWidth());
//            final int offsetY = Realm.RANDOM.nextInt(this.height-room.getHeight());

			baseLayer.append(room, offsetX, offsetY);
			log.info("Dungeon room added at {}, {}", offsetX, offsetY);
			if (previousRoom != null) {
				final int previousRoomCenterX = previousRoomOffsetX + (previousRoom.getWidth() / 2);
				final int previousRoomCenterY = previousRoomOffsetY + (previousRoom.getHeight() / 2);

				final int roomCenterX = offsetX + (room.getWidth() / 2);
				final int roomCenterY = offsetY + (room.getHeight() / 2);

				this.connectPoints(baseLayer, previousRoomCenterX, previousRoomCenterY, roomCenterX, roomCenterY);
				if (i == numRooms - 1) {
					final Enemy enemy = GameObjectUtils.getEnemyFromId(13,
							new Vector2f(roomCenterX * tileSize, roomCenterY * tileSize));
					enemy.setHealth(enemy.getHealth() * 4);
					// enemy.setPos(spawnPos.clone(200, 0));
				}
				this.dungeon.addVertex(previousRoom);
				this.dungeon.addVertex(room);
				this.dungeon.addEdge(previousRoom, room, true);
			}
			previousRoomOffsetX = offsetX;
			previousRoomOffsetY = offsetY;
			previousRoom = room;
		}
		return Arrays.asList(baseLayer, collisionLayer);

	}

	public void connectPoints(TileMap targetLayer, int srcX, int srcY, int destX, int destY) {
		final int xDiff = destX - srcX;
		final int yDiff = destY - srcY;
		final TileModel model = GameDataManager.TILES.get(29);
		final TileModel model0 = GameDataManager.TILES.get(29);

		log.info("Connecting rooms SRC {}, {}. TARGET {}, {}", srcX, srcY, destX, destY);

		if (xDiff > 0) {
			for (int i = srcX; i < srcX + xDiff; i++) {
				log.info("Filling X walkway tile at {}, {}", i, srcY);
				targetLayer.setTileAt(srcY, i, model);
				try {
					targetLayer.setTileAt(srcY - 1, i, model0);
					targetLayer.setTileAt(srcY + 1, i, model0);
				} catch (Exception e) {

				}
			}
		} else {
			for (int i = srcX; i > (srcX + xDiff); i--) {
				log.info("Filling X walkway tile at {}, {}", i, srcY);
				targetLayer.setTileAt(srcY, i, model);
				try {
					targetLayer.setTileAt(srcY - 1, i, model0);
					targetLayer.setTileAt(srcY + 1, i, model0);
				} catch (Exception e) {

				}
			}
		}

		if (yDiff > 0) {
			for (int i = srcY; i < srcY + yDiff; i++) {
				log.info("Filling Y walkway tile at {}, {}", srcX, i);
				targetLayer.setTileAt(i, destX, model);
				try {
					targetLayer.setTileAt(i, destX - 1, model0);
					targetLayer.setTileAt(i, destX + 1, model0);
				} catch (Exception e) {

				}

			}
		} else {
			for (int i = srcY; i > (srcY + yDiff); i--) {
				log.info("Filling Y walkway tile at {}, {}", srcX, i);
				targetLayer.setTileAt(i, destX, model);
				try {
					targetLayer.setTileAt(i, destX - 1, model0);
					targetLayer.setTileAt(i, destX + 1, model0);
				} catch (Exception e) {

				}
			}
		}
	}

	public List<Integer> getRoomLinkParams(TileMap room0, TileMap room1) {
		return null;
	}

	public TileMap getRoom(int tileSize, int minRoomWidth, int maxRoomWidth, int minRoomHeight, int maxRoomHeight,
			List<RoomShapeTemplate> shapeTemplates) {
		@SuppressWarnings("unused")
		// TODO: Implement room shapes
		final RoomShapeTemplate shape = shapeTemplates.get(Realm.RANDOM.nextInt(shapeTemplates.size()));
		final int roomWidth = minRoomWidth + Realm.RANDOM.nextInt((maxRoomWidth - minRoomWidth) + 1);
		final int roomHeight = minRoomHeight + Realm.RANDOM.nextInt((maxRoomHeight - minRoomHeight) + 1);
		final TileMap baseLayer = new TileMap(tileSize, roomWidth, roomHeight);

		// Currently only supports rectangular rooms because i'm terrible at programming
		if (shape.equals(RoomShapeTemplate.RECTANGLE)) {
			for (int i = 0; i < roomHeight; i++) {
				for (int j = 0; j < roomWidth; j++) {
					TileModel model = GameDataManager.TILES.get(29);
					baseLayer.setTileAt(i, j, (short) model.getTileId(), model.getData());
				}
			}
		}

		if (shape.equals(RoomShapeTemplate.OVAL)) {
			final int centerX = roomWidth / 2;
			final int centerY = roomHeight / 2;
			//int radius = centerX;
			final Vector2f pos = new Vector2f(centerX, centerY);
			for (int i = 0; i < roomHeight; i++) {
				for (int j = 0; j < roomWidth; j++) {
					if (pos.distanceTo(new Vector2f(i, j)) < (roomWidth / 2)) {
						final TileModel model = GameDataManager.TILES.get(29);
						baseLayer.setTileAt(i, j, (short) model.getTileId(), model.getData());
					}
				}
			}
		}
		return baseLayer;
	}
}
