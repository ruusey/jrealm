package com.jrealm.game.tile;

import java.util.Arrays;
import java.util.List;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.TileModel;
import com.jrealm.net.realm.Realm;
import com.jrealm.util.Graph;

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
	private List<Integer> floorTileIds;
	private int wallTileId;
	private List<String> hallwayStyles;
	private int bossEnemyId;
	private Graph<TileMap> dungeon;
	private int bossRoomCenterX = -1;
	private int bossRoomCenterY = -1;

	public DungeonGenerator(int width, int height, int tileSize, int minRooms, int maxRooms, int minRoomWidth,
			int maxRoomWidth, int minRoomHeight, int maxRoomHeight, List<RoomShapeTemplate> shapeTemplates,
			List<Integer> floorTileIds, int wallTileId, List<String> hallwayStyles, int bossEnemyId) {
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
		this.floorTileIds = floorTileIds;
		this.wallTileId = wallTileId;
		this.hallwayStyles = hallwayStyles;
		this.bossEnemyId = bossEnemyId;
		this.dungeon = new Graph<>();
	}

	public int getBossRoomCenterX() {
		return this.bossRoomCenterX;
	}

	public int getBossRoomCenterY() {
		return this.bossRoomCenterY;
	}

	private TileModel getRandomFloorTile() {
		if (this.floorTileIds != null && !this.floorTileIds.isEmpty()) {
			int id = this.floorTileIds.get(Realm.RANDOM.nextInt(this.floorTileIds.size()));
			TileModel tile = GameDataManager.TILES.get(id);
			if (tile != null) return tile;
		}
		return GameDataManager.TILES.get(29);
	}

	private TileModel getWallTile() {
		if (this.wallTileId > 0) {
			TileModel tile = GameDataManager.TILES.get(this.wallTileId);
			if (tile != null) return tile;
		}
		return null;
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
		log.info("[DungeonGen] Generating new procedural dungeon realm with room count {}. Params: {}", numRooms, this);

		for (int i = 0; i < numRooms; i++) {
			final boolean isBossRoom = (i == numRooms - 1);
			int roomMinW = this.minRoomWidth;
			int roomMaxW = this.maxRoomWidth;
			int roomMinH = this.minRoomHeight;
			int roomMaxH = this.maxRoomHeight;

			// Boss room is 1.5x larger
			if (isBossRoom) {
				roomMinW = (int) (this.maxRoomWidth * 1.2);
				roomMaxW = (int) (this.maxRoomWidth * 1.5);
				roomMinH = (int) (this.maxRoomHeight * 1.2);
				roomMaxH = (int) (this.maxRoomHeight * 1.5);
			}

			final TileMap room = this.getRoom(this.tileSize, roomMinW, roomMaxW, roomMinH, roomMaxH,
					this.shapeTemplates, isBossRoom);

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

			baseLayer.append(room, offsetX, offsetY);

			// Place wall tile border on collision layer for boss room
			if (isBossRoom) {
				TileModel wallTile = this.getWallTile();
				if (wallTile != null) {
					this.placeBossRoomWalls(collisionLayer, room, offsetX, offsetY, wallTile);
				}
			}

			log.info("[DungeonGen] Dungeon room added at coordinates {}, {} (boss={})", offsetX, offsetY, isBossRoom);
			if (previousRoom != null) {
				final int previousRoomCenterX = previousRoomOffsetX + (previousRoom.getWidth() / 2);
				final int previousRoomCenterY = previousRoomOffsetY + (previousRoom.getHeight() / 2);

				final int roomCenterX = offsetX + (room.getWidth() / 2);
				final int roomCenterY = offsetY + (room.getHeight() / 2);

				this.connectPoints(baseLayer, previousRoomCenterX, previousRoomCenterY, roomCenterX, roomCenterY);

				if (isBossRoom) {
					this.bossRoomCenterX = roomCenterX;
					this.bossRoomCenterY = roomCenterY;
					this.carveBossRoomEntrance(baseLayer, collisionLayer, offsetX, offsetY,
							room.getWidth(), room.getHeight());
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

	private void placeBossRoomWalls(TileMap collisionLayer, TileMap room, int offsetX, int offsetY, TileModel wallTile) {
		for (int row = 0; row < room.getHeight(); row++) {
			for (int col = 0; col < room.getWidth(); col++) {
				Tile tile = room.getBlocks()[row][col];
				if (tile == null || tile.isVoid()) continue;
				// Check if this tile is on the edge of the room shape
				boolean isEdge = false;
				if (row == 0 || col == 0 || row == room.getHeight() - 1 || col == room.getWidth() - 1) {
					isEdge = true;
				} else {
					// Check if any neighbor is void/null
					if (room.getBlocks()[row - 1][col] == null || room.getBlocks()[row - 1][col].isVoid()
							|| room.getBlocks()[row + 1][col] == null || room.getBlocks()[row + 1][col].isVoid()
							|| room.getBlocks()[row][col - 1] == null || room.getBlocks()[row][col - 1].isVoid()
							|| room.getBlocks()[row][col + 1] == null || room.getBlocks()[row][col + 1].isVoid()) {
						isEdge = true;
					}
				}
				if (isEdge) {
					int targetRow = row + offsetY;
					int targetCol = col + offsetX;
					if (targetRow >= 0 && targetRow < collisionLayer.getHeight()
							&& targetCol >= 0 && targetCol < collisionLayer.getWidth()) {
						collisionLayer.setTileAt(targetRow, targetCol, wallTile);
					}
				}
			}
		}
	}

	private void carveBossRoomEntrance(TileMap baseLayer, TileMap collisionLayer,
			int roomOffsetX, int roomOffsetY, int roomWidth, int roomHeight) {
		// Scan 1 tile outside each edge of the boss room. Where the hallway has placed
		// floor tiles adjacent to the room, carve through the wall to create an opening.
		int halfOpening = 1;

		// Check top edge (row just above room)
		int topRow = roomOffsetY - 1;
		if (topRow >= 0) {
			for (int col = roomOffsetX; col < roomOffsetX + roomWidth; col++) {
				if (col < 0 || col >= baseLayer.getWidth()) continue;
				Tile outside = baseLayer.getBlocks()[topRow][col];
				if (outside != null && !outside.isVoid()) {
					this.carveWallAt(collisionLayer, roomOffsetY, col, halfOpening);
				}
			}
		}

		// Check bottom edge (row just below room)
		int bottomRow = roomOffsetY + roomHeight;
		if (bottomRow < baseLayer.getHeight()) {
			for (int col = roomOffsetX; col < roomOffsetX + roomWidth; col++) {
				if (col < 0 || col >= baseLayer.getWidth()) continue;
				Tile outside = baseLayer.getBlocks()[bottomRow][col];
				if (outside != null && !outside.isVoid()) {
					this.carveWallAt(collisionLayer, roomOffsetY + roomHeight - 1, col, halfOpening);
				}
			}
		}

		// Check left edge (col just left of room)
		int leftCol = roomOffsetX - 1;
		if (leftCol >= 0) {
			for (int row = roomOffsetY; row < roomOffsetY + roomHeight; row++) {
				if (row < 0 || row >= baseLayer.getHeight()) continue;
				Tile outside = baseLayer.getBlocks()[row][leftCol];
				if (outside != null && !outside.isVoid()) {
					this.carveWallAt(collisionLayer, row, roomOffsetX, halfOpening);
				}
			}
		}

		// Check right edge (col just right of room)
		int rightCol = roomOffsetX + roomWidth;
		if (rightCol < baseLayer.getWidth()) {
			for (int row = roomOffsetY; row < roomOffsetY + roomHeight; row++) {
				if (row < 0 || row >= baseLayer.getHeight()) continue;
				Tile outside = baseLayer.getBlocks()[row][rightCol];
				if (outside != null && !outside.isVoid()) {
					this.carveWallAt(collisionLayer, row, roomOffsetX + roomWidth - 1, halfOpening);
				}
			}
		}
	}

	private void carveWallAt(TileMap collisionLayer, int row, int col, int halfOpening) {
		for (int dr = -halfOpening; dr <= halfOpening; dr++) {
			for (int dc = -halfOpening; dc <= halfOpening; dc++) {
				int nr = row + dr;
				int nc = col + dc;
				if (nr >= 0 && nr < collisionLayer.getHeight() && nc >= 0 && nc < collisionLayer.getWidth()) {
					collisionLayer.getBlocks()[nr][nc] = null;
				}
			}
		}
	}

	private String pickHallwayStyle() {
		if (this.hallwayStyles != null && !this.hallwayStyles.isEmpty()) {
			return this.hallwayStyles.get(Realm.RANDOM.nextInt(this.hallwayStyles.size()));
		}
		return "L_SHAPED";
	}

	public void connectPoints(TileMap targetLayer, int srcX, int srcY, int destX, int destY) {
		String style = this.pickHallwayStyle();
		log.info("[DungeonGen] Connecting rooms SRC {}, {}. TARGET {}, {} with style {}", srcX, srcY, destX, destY, style);

		switch (style) {
			case "ZIGZAG":
				this.connectZigzag(targetLayer, srcX, srcY, destX, destY);
				break;
			case "WIDE":
				this.connectWide(targetLayer, srcX, srcY, destX, destY);
				break;
			case "WINDING":
				this.connectWinding(targetLayer, srcX, srcY, destX, destY);
				break;
			case "L_SHAPED":
			default:
				this.connectLShaped(targetLayer, srcX, srcY, destX, destY);
				break;
		}
	}

	private void fillHorizontal(TileMap targetLayer, int row, int startCol, int endCol, int halfWidth) {
		int minCol = Math.min(startCol, endCol);
		int maxCol = Math.max(startCol, endCol);
		for (int col = minCol; col <= maxCol; col++) {
			for (int r = row - halfWidth; r <= row + halfWidth; r++) {
				this.safeSetFloorTile(targetLayer, r, col);
			}
		}
	}

	private void fillVertical(TileMap targetLayer, int col, int startRow, int endRow, int halfWidth) {
		int minRow = Math.min(startRow, endRow);
		int maxRow = Math.max(startRow, endRow);
		for (int row = minRow; row <= maxRow; row++) {
			for (int c = col - halfWidth; c <= col + halfWidth; c++) {
				this.safeSetFloorTile(targetLayer, row, c);
			}
		}
	}

	private void safeSetFloorTile(TileMap targetLayer, int row, int col) {
		try {
			if (row >= 0 && row < targetLayer.getHeight() && col >= 0 && col < targetLayer.getWidth()) {
				targetLayer.setTileAt(row, col, this.getRandomFloorTile());
			}
		} catch (Exception e) {
			log.debug("[DungeonGen] Failed to fill walkway tile at {}, {}. Reason: {}", row, col, e.getMessage());
		}
	}

	// L-shaped: horizontal then vertical, 3 tiles wide (existing behavior)
	private void connectLShaped(TileMap targetLayer, int srcX, int srcY, int destX, int destY) {
		this.fillHorizontal(targetLayer, srcY, srcX, destX, 1);
		this.fillVertical(targetLayer, destX, srcY, destY, 1);
	}

	// Zigzag: break path into segments with random offsets
	private void connectZigzag(TileMap targetLayer, int srcX, int srcY, int destX, int destY) {
		int midX1 = (srcX + destX) / 3 + (Realm.RANDOM.nextInt(5) - 2);
		int midX2 = (srcX + destX) * 2 / 3 + (Realm.RANDOM.nextInt(5) - 2);
		int midY = (srcY + destY) / 2 + (Realm.RANDOM.nextInt(7) - 3);

		// Clamp midpoints to map bounds
		midX1 = Math.max(1, Math.min(midX1, this.width - 2));
		midX2 = Math.max(1, Math.min(midX2, this.width - 2));
		midY = Math.max(1, Math.min(midY, this.height - 2));

		// Segment 1: src -> (midX1, midY)
		this.fillHorizontal(targetLayer, srcY, srcX, midX1, 1);
		this.fillVertical(targetLayer, midX1, srcY, midY, 1);

		// Segment 2: (midX1, midY) -> (midX2, midY)
		this.fillHorizontal(targetLayer, midY, midX1, midX2, 1);

		// Segment 3: (midX2, midY) -> dest
		this.fillVertical(targetLayer, midX2, midY, destY, 1);
		this.fillHorizontal(targetLayer, destY, midX2, destX, 1);
	}

	// Wide: same L-shaped but 5 tiles wide instead of 3
	private void connectWide(TileMap targetLayer, int srcX, int srcY, int destX, int destY) {
		this.fillHorizontal(targetLayer, srcY, srcX, destX, 2);
		this.fillVertical(targetLayer, destX, srcY, destY, 2);
	}

	// Winding: walk toward dest with random perpendicular drift
	private void connectWinding(TileMap targetLayer, int srcX, int srcY, int destX, int destY) {
		int curX = srcX;
		int curY = srcY;
		int maxSteps = Math.abs(destX - srcX) + Math.abs(destY - srcY) + 20;
		int steps = 0;

		while ((curX != destX || curY != destY) && steps < maxSteps) {
			// Place floor tiles at current position (2 wide)
			for (int dr = -1; dr <= 1; dr++) {
				for (int dc = -1; dc <= 1; dc++) {
					this.safeSetFloorTile(targetLayer, curY + dr, curX + dc);
				}
			}

			// Move toward destination with random drift
			int dx = Integer.compare(destX, curX);
			int dy = Integer.compare(destY, curY);

			// 70% chance to move toward target, 30% to drift
			if (Realm.RANDOM.nextFloat() < 0.7f) {
				// Move toward destination - prefer the axis with greater distance
				if (Math.abs(destX - curX) > Math.abs(destY - curY)) {
					curX += dx;
				} else if (dy != 0) {
					curY += dy;
				} else {
					curX += dx;
				}
			} else {
				// Random perpendicular drift
				if (Realm.RANDOM.nextBoolean()) {
					curX += (Realm.RANDOM.nextInt(3) - 1);
				} else {
					curY += (Realm.RANDOM.nextInt(3) - 1);
				}
			}

			// Clamp to map bounds
			curX = Math.max(1, Math.min(curX, this.width - 2));
			curY = Math.max(1, Math.min(curY, this.height - 2));
			steps++;
		}

		// Ensure the destination is also floored
		for (int dr = -1; dr <= 1; dr++) {
			for (int dc = -1; dc <= 1; dc++) {
				this.safeSetFloorTile(targetLayer, destY + dr, destX + dc);
			}
		}
	}

	public List<Integer> getRoomLinkParams(TileMap room0, TileMap room1) {
		return null;
	}

	public TileMap getRoom(int tileSize, int minRoomWidth, int maxRoomWidth, int minRoomHeight, int maxRoomHeight,
			List<RoomShapeTemplate> shapeTemplates, boolean isBossRoom) {
		final RoomShapeTemplate shape = shapeTemplates.get(Realm.RANDOM.nextInt(shapeTemplates.size()));
		final int roomWidth = minRoomWidth + Realm.RANDOM.nextInt((maxRoomWidth - minRoomWidth) + 1);
		final int roomHeight = minRoomHeight + Realm.RANDOM.nextInt((maxRoomHeight - minRoomHeight) + 1);
		final TileMap baseLayer = new TileMap(tileSize, roomWidth, roomHeight);
		log.info("[DungeonGen] Generating room with params tileSize={}, roomWidth={}, roomHeight={}, shape={}", tileSize, roomWidth, roomHeight, shape);

		switch (shape) {
			case RECTANGLE:
				this.generateRectangleRoom(baseLayer, roomWidth, roomHeight);
				break;
			case OVAL:
				this.generateOvalRoom(baseLayer, roomWidth, roomHeight);
				break;
			case DIAMOND:
				this.generateDiamondRoom(baseLayer, roomWidth, roomHeight);
				break;
			case CROSS:
				this.generateCrossRoom(baseLayer, roomWidth, roomHeight);
				break;
			case L_SHAPE:
				this.generateLShapeRoom(baseLayer, roomWidth, roomHeight);
				break;
			default:
				this.generateRectangleRoom(baseLayer, roomWidth, roomHeight);
				break;
		}

		return baseLayer;
	}

	private void generateRectangleRoom(TileMap baseLayer, int roomWidth, int roomHeight) {
		for (int i = 0; i < roomHeight; i++) {
			for (int j = 0; j < roomWidth; j++) {
				TileModel model = this.getRandomFloorTile();
				baseLayer.setTileAt(i, j, (short) model.getTileId(), model.getData());
			}
		}
	}

	private void generateOvalRoom(TileMap baseLayer, int roomWidth, int roomHeight) {
		final int centerX = roomWidth / 2;
		final int centerY = roomHeight / 2;
		final Vector2f pos = new Vector2f(centerX, centerY);
		for (int i = 0; i < roomHeight; i++) {
			for (int j = 0; j < roomWidth; j++) {
				if (pos.distanceTo(new Vector2f(i, j)) < (roomWidth / 2)) {
					final TileModel model = this.getRandomFloorTile();
					baseLayer.setTileAt(i, j, (short) model.getTileId(), model.getData());
				}
			}
		}
	}

	private void generateDiamondRoom(TileMap baseLayer, int roomWidth, int roomHeight) {
		final int centerX = roomWidth / 2;
		final int centerY = roomHeight / 2;
		final int radius = Math.min(centerX, centerY);
		for (int i = 0; i < roomHeight; i++) {
			for (int j = 0; j < roomWidth; j++) {
				int dx = Math.abs(j - centerX);
				int dy = Math.abs(i - centerY);
				if (dx + dy <= radius) {
					final TileModel model = this.getRandomFloorTile();
					baseLayer.setTileAt(i, j, (short) model.getTileId(), model.getData());
				}
			}
		}
	}

	private void generateCrossRoom(TileMap baseLayer, int roomWidth, int roomHeight) {
		final int centerX = roomWidth / 2;
		final int centerY = roomHeight / 2;
		final int armWidth = Math.max(2, roomWidth / 3);
		final int armHeight = Math.max(2, roomHeight / 3);
		for (int i = 0; i < roomHeight; i++) {
			for (int j = 0; j < roomWidth; j++) {
				boolean inVerticalArm = (j >= centerX - armWidth / 2 && j < centerX + armWidth / 2);
				boolean inHorizontalArm = (i >= centerY - armHeight / 2 && i < centerY + armHeight / 2);
				if (inVerticalArm || inHorizontalArm) {
					final TileModel model = this.getRandomFloorTile();
					baseLayer.setTileAt(i, j, (short) model.getTileId(), model.getData());
				}
			}
		}
	}

	private void generateLShapeRoom(TileMap baseLayer, int roomWidth, int roomHeight) {
		int splitX = roomWidth / 2 + Realm.RANDOM.nextInt(Math.max(1, roomWidth / 4));
		int splitY = roomHeight / 2 + Realm.RANDOM.nextInt(Math.max(1, roomHeight / 4));
		boolean mirrored = Realm.RANDOM.nextBoolean();

		for (int i = 0; i < roomHeight; i++) {
			for (int j = 0; j < roomWidth; j++) {
				boolean inShape;
				if (mirrored) {
					inShape = (i >= splitY || j >= (roomWidth - splitX));
				} else {
					inShape = (i >= splitY || j < splitX);
				}
				if (inShape) {
					final TileModel model = this.getRandomFloorTile();
					baseLayer.setTileAt(i, j, (short) model.getTileId(), model.getData());
				}
			}
		}
	}
}
