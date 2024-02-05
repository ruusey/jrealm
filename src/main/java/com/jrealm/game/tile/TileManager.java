package com.jrealm.game.tile;

import java.awt.Graphics2D;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.model.TerrainGenerationParameters;
import com.jrealm.game.model.TileGroup;
import com.jrealm.game.model.TileModel;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.util.Camera;
import com.jrealm.net.client.packet.LoadMapPacket;

import lombok.Data;

@Data
public class TileManager {
	private List<TileMap> mapLayers;

	// Server side constructor
	public TileManager(int mapId) {
		MapModel model = GameDataManager.MAPS.get(mapId);
		if (model.getData() != null) {
			this.mapLayers = this.getLayersFromData(model);

		} else {
			TerrainGenerationParameters params = GameDataManager.TERRAINS.get(model.getTerrainId());
			this.mapLayers = this.getLayersFromTerrain(model.getWidth(), model.getHeight(), model.getTileSize(),
					params);
		}
	}

	public TileManager(int width, int height, int tileSize, TerrainGenerationParameters params) {
		this.mapLayers = this.getLayersFromTerrain(width, height, tileSize, params);
	}

	// Client side constructor
	public TileManager(MapModel model) {
		this.mapLayers = new ArrayList<>();
		TileMap baseLayer = new TileMap((short) model.getMapId(), model.getTileSize(), model.getWidth(),
				model.getHeight());
		TileMap collisionLayer = new TileMap((short) model.getMapId(), model.getTileSize(), model.getWidth(),
				model.getHeight());
		this.mapLayers.add(baseLayer);
		this.mapLayers.add(collisionLayer);
	}

	// Generates a random terrain of size with the given parameters
	private List<TileMap> getLayersFromTerrain(int width, int height, int tileSize,
			TerrainGenerationParameters params) {
		final Random random = new Random(Instant.now().toEpochMilli());
		// Build empty base and collision layers with given size
		TileMap baseLayer = new TileMap(tileSize, width, height);
		TileMap collisionLayer = new TileMap(tileSize, width, height);

		// For each group to attempt to populate in the given area (WIP, only expects one group right now)
		for (TileGroup group : params.getTileGroups()) {
			// Separate tiles having collision from background tiles
			List<TileModel> tileIdsCollision = group.getTileIds().stream().map(id -> GameDataManager.TILES.get(id))
					.filter(tm -> tm.getData().hasCollision()).collect(Collectors.toList());
			List<TileModel> tileIdsNormal = group.getTileIds().stream().map(id -> GameDataManager.TILES.get(id))
					.filter(tm -> !tm.getData().hasCollision()).collect(Collectors.toList());

			// Iterate over every potential tile space and build the background layer
			// using the tiles defined in this TileGroup that do NOT have collision data
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					TileModel tileIdToCreate = tileIdsNormal.get(random.nextInt(tileIdsNormal.size()));
					float rarity = group.getRarities().get(tileIdToCreate.getTileId() + "");
					if((rarity>0.0) && (random.nextFloat() <= rarity)) {
						baseLayer.setBlockAt(i, j, (short) tileIdToCreate.getTileId(), tileIdToCreate.getData());
					}else {
						tileIdToCreate = tileIdsNormal.get(0);
						baseLayer.setBlockAt(i, j, (short) tileIdToCreate.getTileId(), tileIdToCreate.getData());
					}

				}
			}
			// Iterate over every potential tile space and build the collision layer
			// using the tiles defined in this TileGroup that have collision data
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					TileModel tileIdToCreate = tileIdsCollision.get(random.nextInt(tileIdsCollision.size()));
					float rarity = group.getRarities().get(tileIdToCreate.getTileId() + "");
					if ((rarity>0.0) && (random.nextFloat() <= rarity)) {
						collisionLayer.setBlockAt(i, j, (short) tileIdToCreate.getTileId(), tileIdToCreate.getData());
					} else {
						collisionLayer.setBlockAt(i, j, (short) 0, tileIdToCreate.getData());
					}
				}
			}
		}
		return Arrays.asList(baseLayer, collisionLayer);
	}

	private List<TileMap> getLayersFromData(MapModel model) {
		Map<String, int[][]> layerMap = model.getData();
		TileMap baseLayer = new TileMap((short)model.getMapId(), model.getTileSize(), model.getWidth(), model.getHeight());
		TileMap collisionLayer = new TileMap((short)model.getMapId(), model.getTileSize(), model.getWidth(), model.getHeight());

		final int[][] baseData = layerMap.get("0");
		final int[][] collisionData = layerMap.get("1");


		for(int i = 0; i< baseData.length; i++) {
			for(int j = 0; j<baseData[i].length; j++) {
				int tileIdToCreate = baseData[i][j];
				TileData tileData = GameDataManager.TILES.get(tileIdToCreate).getData();
				baseLayer.setBlockAt(i, j, (short)tileIdToCreate, tileData);
				//collisionLayer.setBlockAt(i, j, (short)0, GameDataManager.TILES.get(0).getData());
			}
		}

		for(int i = 0; i< collisionData.length; i++) {
			for(int j = 0; j<collisionData[i].length; j++) {
				int tileIdToCreate = collisionData[i][j];
				TileData tileData = GameDataManager.TILES.get(tileIdToCreate).getData();
				collisionLayer.setBlockAt(i, j, (short)tileIdToCreate, tileData);
				//collisionLayer.setBlockAt(i, j, (short)0, GameDataManager.TILES.get(0).getData());
			}
		}
		return Arrays.asList(baseLayer, collisionLayer);

	}

	public Tile[] getBaseTiles(Vector2f pos) {
		Tile[] block = new Tile[144];
		Vector2f posNormalized = new Vector2f(pos.x / GlobalConstants.BASE_TILE_SIZE, pos.y / GlobalConstants.BASE_TILE_SIZE);
		this.normalizeToBounds(posNormalized);
		int i = 0;
		for (int x = (int) (posNormalized.x - 5); x < (posNormalized.x + 6); x++) {
			for (int y = (int) (posNormalized.y - 5); y < (int) (posNormalized.y + 6); y++) {
				if ((x >= this.getBaseLayer().getWidth()) || (y >= this.getBaseLayer().getHeight()) || (x < 0)
						|| (y < 0)) {
					continue;
				}
				try {
					block[i] = (Tile) this.mapLayers.get(0).getBlocks()[y][x];
					i++;
				}catch(Exception e) {

				}
			}
		}
		return block;
	}

	public Tile[] getCollisionTiles(Vector2f pos) {
		Tile[] block = new Tile[144];
		Vector2f posNormalized = new Vector2f(pos.x / GlobalConstants.BASE_TILE_SIZE, pos.y / GlobalConstants.BASE_TILE_SIZE);
		this.normalizeToBounds(posNormalized);
		int i = 0;
		for (int x = (int) (posNormalized.x - 5); x < (posNormalized.x + 6); x++) {
			for (int y = (int) (posNormalized.y - 5); y < (int) (posNormalized.y + 6); y++) {
				if ((x >= this.getCollisionLayer().getWidth()) || (y >= this.getCollisionLayer().getHeight()) || (x < 0)
						|| (y < 0)) {
					continue;
				}
				try {
					block[i] = (Tile) this.mapLayers.get(1).getBlocks()[y][x];
					i++;
				}catch(Exception e) {

				}
			}
		}
		return block;
	}

	public TileMap getCollisionLayer() {
		return this.mapLayers.get(this.mapLayers.size()-1);
	}

	public TileMap getBaseLayer() {
		return this.mapLayers.get(0);
	}

	private void normalizeToBounds(Vector2f pos) {
		if(pos.x< 0) {
			pos.x = 0;
		}
		if(pos.x>(this.getBaseLayer().getWidth()-1)) {
			pos.x = this.getBaseLayer().getWidth()-1;
		}

		if(pos.y< 0) {
			pos.y = 0;
		}
		if(pos.y>(this.getBaseLayer().getHeight()-1)) {
			pos.y = this.getBaseLayer().getWidth()-1;
		}
	}

	public Vector2f getSafePosition() {
		Vector2f pos = this.randomPos();
		while(this.isCollisionTile(pos)) {
			pos = this.randomPos();
		}
		return pos;
	}

	public boolean isCollisionTile(Vector2f pos) {
		final TileMap collisionLayer = this.getCollisionLayer();
		final int tileX = (int) pos.x / collisionLayer.getTileSize();
		final int tileY = (int) pos.y / collisionLayer.getTileSize();
		final Tile currentTile = collisionLayer.getBlocks()[tileY][tileX];
		return (currentTile != null) && !currentTile.isVoid();
	}

	public boolean collidesXLimit(Entity e, float ax) {
		final Vector2f futurePos = e.getPos().clone(ax, 0);
		return (futurePos.x <= 0)
				|| ((futurePos.x + e.getSize()) >= (this.getBaseLayer().getWidth()
						* this.getBaseLayer().getTileSize()));

	}

	public boolean collidesYLimit(Entity e, float dy) {
		final Vector2f futurePos = e.getPos().clone(0, dy);
		return (futurePos.y <= 0)
				|| ((futurePos.y + e.getSize()) >= (this.getBaseLayer().getHeight()
						* this.getBaseLayer().getTileSize()));

	}

	public boolean collidesSlowTile(Entity e) {
		final Vector2f centerPos = e.getCenteredPosition();
		final int startX = (int) (centerPos.x / (float) this.getBaseLayer().getTileSize());
		final int startY = (int) (centerPos.y / (float) this.getBaseLayer().getTileSize());

		final Tile currentTile = this.getBaseLayer().getBlocks()[startY][startX];

		final AABB tileBounds = new AABB(currentTile.getPos(), currentTile.getWidth(), currentTile.getHeight());
		final AABB futurePosBounds = new AABB(e.getPos(), (e.getSize() / 2), e.getSize() / 2);

		return currentTile.getData().slows() && tileBounds.intersect(futurePosBounds);
	}

	public boolean collisionTile(Entity e, float ax, float ay) {
		final Vector2f futurePos = e.getPos().clone(ax, ay);
		for (Tile t : this.getCollisionTiles(e.getPos())) {
			if((t==null) || t.isVoid()) {
				continue;
			}
			AABB tileBounds = new AABB(t.getPos(), t.getWidth(), t.getHeight());
			AABB futurePosBounds = new AABB(futurePos, (int) (e.getSize() / 1.5), (int) (e.getSize() / 1.5));
			if (tileBounds.intersect(futurePosBounds))
				return true;
		}

		return false;
	}

	public Vector2f randomPos() {
		final float x = Realm.RANDOM.nextInt(this.getBaseLayer().getWidth()) * this.getBaseLayer().getTileSize();
		final float y = Realm.RANDOM.nextInt(this.getBaseLayer().getHeight()) * this.getBaseLayer().getTileSize();
		return new Vector2f(x, y);
	}

	public AABB getRenderViewPort(Camera cam) {
		return new AABB(
				cam.getTarget().getPos().clone(-(6 * GlobalConstants.BASE_TILE_SIZE),
						-(6 * GlobalConstants.BASE_TILE_SIZE)),
				(12 * GlobalConstants.BASE_TILE_SIZE), (12 * GlobalConstants.BASE_TILE_SIZE));
	}

	public AABB getRenderViewPort(Entity p) {
		return new AABB(
				p.getPos().clone(-(6 * GlobalConstants.BASE_TILE_SIZE),
						-(6 * GlobalConstants.BASE_TILE_SIZE)),
				(12 * GlobalConstants.BASE_TILE_SIZE), (12 * GlobalConstants.BASE_TILE_SIZE));
	}

	public NetTile[] getLoadMapTiles(Player player) {
		final Vector2f pos = player.getPos();
		final List<NetTile> tiles = new ArrayList<>();
		final Vector2f posNormalized = new Vector2f(pos.x / GlobalConstants.BASE_TILE_SIZE,
				pos.y / GlobalConstants.BASE_TILE_SIZE);
		this.normalizeToBounds(posNormalized);
		for (int x = (int) (posNormalized.x - 6); x < (posNormalized.x + 7); x++) {
			for (int y = (int) (posNormalized.y - 6); y < (int) (posNormalized.y + 7); y++) {
				// Temp fix. Aint nobody got time for array math.
				if ((x >= this.getBaseLayer().getWidth()) || (y >= this.getBaseLayer().getHeight()) || (x < 0)
						|| (y < 0)) {
					continue;
				}
				try {
					Tile collisionTile = (Tile) this.mapLayers.get(1).getBlocks()[y][x];
					Tile normalTile = (Tile) this.mapLayers.get(0).getBlocks()[y][x];
					NetTile collisionNetTile = new NetTile(collisionTile.getTileId(), (byte) 1, y, x);
					NetTile normalNetTile = new NetTile(normalTile.getTileId(), (byte) 0, y, x);

					tiles.add(normalNetTile);
					tiles.add(collisionNetTile);

				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		return tiles.toArray(new NetTile[0]);
	}

	public void mergeMap(LoadMapPacket packet) {
		for(NetTile tile : packet.getTiles()) {
			TileData data = GameDataManager.TILES.get((int) tile.getTileId()).getData();
			this.mapLayers.get((int) tile.getLayer()).setBlockAt(tile.getXIndex(), tile.getYIndex(), tile.getTileId(),
					data);
		}
	}

	public void render(Player player, Graphics2D g) {
		for (Tile tile : this.getBaseTiles(player.getPos())) {
			if(tile==null) {
				continue;
			}
			tile.render(g);
		}

		for (Tile tile : this.getCollisionTiles(player.getPos())) {
			if(tile==null) {
				continue;
			}

			if(!tile.isVoid()) {
				tile.render(g);
			}
		}
	}
}
