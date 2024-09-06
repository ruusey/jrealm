package com.jrealm.game.tile;

import java.util.Arrays;
import java.util.List;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.model.TerrainGenerationParameters;
import com.jrealm.game.model.TileModel;
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
    public DungeonGenerator(int width, int height, int tileSize, int minRooms, int maxRooms, int minRoomWidth, int maxRoomWidth, int minRoomHeight, int maxRoomHeight, List<RoomShapeTemplate> shapeTemplates) {
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
    
    public List<TileMap> generateDungeon(TerrainGenerationParameters params) {
        
        TileMap baseLayer = new TileMap(this.tileSize, this.width, this.height);
        baseLayer.fill(0);
        TileMap collisionLayer = new TileMap(this.tileSize, this.width, this.height);
        collisionLayer.fill(0);
        int numRooms =  this.minRooms + Realm.RANDOM.nextInt((this.maxRooms-this.minRooms)+1);
        TileMap previousRoom = null;
        int previousRoomOffsetX = 0;
        int previousRoomOffsetY = 0;
        for(int i = 0 ; i< numRooms; i++) {

            TileMap room = this.getRoom(this.tileSize, this.minRoomWidth, this.maxRoomWidth, this.minRoomHeight, this.maxRoomHeight, this.shapeTemplates);
            int offsetX = Realm.RANDOM.nextInt(previousRoomOffsetX+room.getWidth());
            int offsetY = Realm.RANDOM.nextInt(previousRoomOffsetY+room.getHeight());
            baseLayer.append(room, offsetX, offsetY);
            
            if(previousRoom!=null) {
                int previousRoomCenterX = previousRoomOffsetX + (previousRoom.getWidth()/2);
                int previousRoomCenterY = previousRoomOffsetY + (previousRoom.getHeight()/2);
                
                int roomCenterX = offsetX + (room.getWidth()/2);
                int roomCenterY = offsetY + (room.getHeight()/2);
                int xDiff = roomCenterX-previousRoomCenterX;
                int yDiff = roomCenterY-previousRoomCenterY;
                double ratio = yDiff/xDiff;
                
                //Down to the right
//                if(previousRoomCenterX<roomCenterX && previousRoomCenterY<roomCenterY) {
//                    for(int j = roomCenterX; j<roomCenterX + xDiff;j--) {
//                        TileModel model = GameDataManager.TILES.get(29);
//                        try {
//                            collisionLayer.setBlockAt(j, roomCenterY, (short)29, model.getData());
//
//                        }catch(Exception e) {
//                            log.error("Failed to connect dungeon room. Reason: {}",e);
//                        }
//                        
//                    }
//                    
//                    for(int j = roomCenterY; j>roomCenterY + yDiff;j--) {
//                        TileModel model = GameDataManager.TILES.get(29);
//                        try {
//                            collisionLayer.setBlockAt(roomCenterX, j, (short)29, model.getData());
//
//                        }catch(Exception e) {
//                            log.error("Failed to connect dungeon room. Reason: {}",e);
//                        }
//                        
//                    }
//                    
//                // Up to the right
//                }
//                else if(previousRoomCenterX<roomCenterX && previousRoomCenterY>roomCenterY) {
//                    for(int j = previousRoomCenterX; j>previousRoomCenterX + xDiff;j++) {
//                        TileModel model = GameDataManager.TILES.get(29);
//                        try {
//                            collisionLayer.setBlockAt(roomCenterX, (int) (j*Math.abs(ratio)), (short)29, model.getData());
//
//                        }catch(Exception e) {
//                            log.error("Failed to connect dungeon room. Reason: {}",e);
//                        }
//                        
//                    }
//                    
//                //Down to the left
//                }else if(previousRoomCenterX>roomCenterX && previousRoomCenterY<roomCenterY) {
//                    for(int j = roomCenterX; j>roomCenterX + xDiff;j++) {
//                        TileModel model = GameDataManager.TILES.get(29);
//                        try {
//                            collisionLayer.setBlockAt(roomCenterX, (int) (j*Math.abs(ratio)), (short)29, model.getData());
//
//                        }catch(Exception e) {
//                            log.error("Failed to connect dungeon room. Reason: {}",e);
//                        }
//                        
//                    }
//                    
//                // Up to the left
//                }else if(previousRoomCenterX>roomCenterX && previousRoomCenterY>roomCenterY) {
//                    for(int j = previousRoomCenterX; j>previousRoomCenterX - xDiff;j--) {
//                        TileModel model = GameDataManager.TILES.get(29);
//                        try {
//                            collisionLayer.setBlockAt(roomCenterX, (int) (j*Math.abs(ratio)), (short)29, model.getData());
//
//                        }catch(Exception e) {
//                            log.error("Failed to connect dungeon room. Reason: {}",e);
//                        }
//                        
//                    }
//                }


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
    
    public List<Integer> getRoomLinkParams(TileMap room0, TileMap room1){
        return null;
    }
    
    public TileMap getRoom(int tileSize, int minRoomWidth, int maxRoomWidth, int minRoomHeight, int maxRoomHeight, List<RoomShapeTemplate> shapeTemplates) {
        final RoomShapeTemplate shape = shapeTemplates.get(Realm.RANDOM.nextInt(shapeTemplates.size()));
        int roomWidth = minRoomWidth + Realm.RANDOM.nextInt((maxRoomWidth-minRoomWidth)+1);
        int roomHeight = minRoomHeight + Realm.RANDOM.nextInt((maxRoomHeight-minRoomHeight)+1);
        TileMap baseLayer = new TileMap(tileSize, roomWidth, roomHeight);
        for(int i = 0; i< roomHeight; i++) {
            for(int j = 0 ; j<roomWidth; j++) {
                TileModel model = GameDataManager.TILES.get(29);
                baseLayer.setBlockAt(i, j, (short)model.getTileId(), model.getData());
            }
        }
        return baseLayer;
    }
}
