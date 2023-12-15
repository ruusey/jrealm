package com.jrealm.game.tiles;

import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.tiles.blocks.Tile;
import com.jrealm.game.tiles.blocks.TileData;
import com.jrealm.game.util.Camera;

import lombok.Data;

@Data
public class TileManager {

	private List<TileMap> mapLayers;

	public TileManager(int mapId) {
		MapModel model = GameDataManager.MAPS.get(mapId);
		this.mapLayers = this.getLayersFromData(model);
		
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

	public Tile[] getNormalTile(Vector2f pos) {
		Tile[] block = new Tile[144];
		Vector2f posNormalized = new Vector2f(pos.x / GlobalConstants.BASE_TILE_SIZE, pos.y / GlobalConstants.BASE_TILE_SIZE);
		this.normalizeToBounds(posNormalized);
		int i = 0;
		for (int x = (int) (posNormalized.x - 5); x < (posNormalized.x + 6); x++) {
			for (int y = (int) (posNormalized.y - 5); y < (int) (posNormalized.y + 6); y++) {
				// Temp fix. Aint nobody got time for array math.
				try {
					block[i] = (Tile) this.mapLayers.get(0).getBlocks()[y][x];
					i++;
				}catch(Exception e) {
					
				}
			}
		}
		return block;
	}
	
	public Tile[] getCollisionTile(Vector2f pos) {
		Tile[] block = new Tile[144];
		Vector2f posNormalized = new Vector2f(pos.x / GlobalConstants.BASE_TILE_SIZE, pos.y / GlobalConstants.BASE_TILE_SIZE);
		this.normalizeToBounds(posNormalized);
		int i = 0;
		for (int x = (int) (posNormalized.x - 5); x < (posNormalized.x + 6); x++) {
			for (int y = (int) (posNormalized.y - 5); y < (int) (posNormalized.y + 6); y++) {
				// Temp fix. Aint nobody got time for array math.
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
		if(pos.x>this.getBaseLayer().getWidth()-1) {
			pos.x = this.getBaseLayer().getWidth()-1;
		}
		
		if(pos.y< 0) {
			pos.y = 0;
		}
		if(pos.y>this.getBaseLayer().getHeight()-1) {
			pos.y = this.getBaseLayer().getWidth()-1;
		}
	}
	
	public boolean collisionTile(Entity e, float ax, float ay) {
		Vector2f futurePos = e.getPos().clone(ax, ay);
		
		for(Tile t : this.getCollisionTile(e.getPos())) {
			if(t==null || t.isVoid()) continue;
			AABB tileBounds = new AABB(t.getPos(), t.getWidth(), t.getHeight());
			AABB futurePosBounds = new AABB(futurePos, e.getSize(), e.getSize());
			if(futurePosBounds.intersect(tileBounds)) {
				return true;
			}
		}

		return false;
	}

	public AABB getRenderViewPort(Camera cam) {
		return new AABB(
				cam.getTarget().getPos().clone(-(6 * GlobalConstants.BASE_TILE_SIZE),
						-(6 * GlobalConstants.BASE_TILE_SIZE)),
				(12 * GlobalConstants.BASE_TILE_SIZE), (12 * GlobalConstants.BASE_TILE_SIZE));

	}
	
	public AABB getRenderViewPort(Player p) {
		return new AABB(
				p.getPos().clone(-(6 * GlobalConstants.BASE_TILE_SIZE),
						-(6 * GlobalConstants.BASE_TILE_SIZE)),
				(12 * GlobalConstants.BASE_TILE_SIZE), (12 * GlobalConstants.BASE_TILE_SIZE));

	}

	public void render(Player player, Graphics2D g) {
		
		for(Tile tile : this.getNormalTile(player.getPos())) {
			if(tile==null) continue;
			tile.render(g);
		}
		
		for(Tile tile : this.getCollisionTile(player.getPos())){
			if(tile==null) continue;

			if(!tile.isVoid()) {
				tile.render(g);
			}
		}
//		for (int i = 0; i < this.mapLayers.size(); i++) {
//			this.mapLayers.get(i).render(g, bounds);
//		}
	}
}
