package com.jrealm.game.tile.decorators;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.TileModel;
import com.jrealm.game.tile.TileMap;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

public class Beach0Decorator extends RealmDecoratorBase {
    private static final Integer SHORE_LINE_SIZE = 5;
    private static  Integer MIN_WATER_POOL_COUNT = 15;
    private static  Integer MAX_WATER_POOL_COUNT = 25;
    private static final TileModel WATER_TILE = GameDataManager.TILES.get(41);
    private static final TileModel WATER_TILE_DEEP = GameDataManager.TILES.get(42);
    

    public Beach0Decorator(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public void decorate(final Realm input) {
        MIN_WATER_POOL_COUNT = input.getTileManager().getBaseLayer().getWidth()/2;
        MAX_WATER_POOL_COUNT = (input.getTileManager().getBaseLayer().getWidth()/2)+15;

        for (int i = 0; i < (Beach0Decorator.MIN_WATER_POOL_COUNT + Realm.RANDOM
                .nextInt(Beach0Decorator.MAX_WATER_POOL_COUNT - Beach0Decorator.MIN_WATER_POOL_COUNT)); i++) {
            final Vector2f pos = input.getTileManager().randomPos();
            final TileMap baseLayer = input.getTileManager().getBaseLayer();
            final int centerX = (int) (pos.x / baseLayer.getTileSize());
            final int centerY = (int) (pos.y / baseLayer.getTileSize());

            baseLayer.setTileAt(centerX, centerY, (short) Beach0Decorator.WATER_TILE_DEEP.getTileId(),
                    Beach0Decorator.WATER_TILE_DEEP.getData());
            baseLayer.setTileAt(centerX, (centerY - 1) > -1 ? centerY - 1 : 0,
                    (short) Beach0Decorator.WATER_TILE.getTileId(), Beach0Decorator.WATER_TILE.getData());
            baseLayer.setTileAt(centerX,
                    (centerY + 1) >= baseLayer.getHeight() ? baseLayer.getHeight() - 1 : centerY + 1,
                    (short) Beach0Decorator.WATER_TILE.getTileId(), Beach0Decorator.WATER_TILE.getData());
            baseLayer.setTileAt((centerX - 1) > -1 ? centerX - 1 : 0, centerY,
                    (short) Beach0Decorator.WATER_TILE.getTileId(), Beach0Decorator.WATER_TILE.getData());
            baseLayer.setTileAt((centerX + 1) >= baseLayer.getWidth() ? baseLayer.getWidth() - 1 : centerX + 1,
                    centerY, (short) Beach0Decorator.WATER_TILE.getTileId(), Beach0Decorator.WATER_TILE.getData());

        }
        this.createShoreline(input);
    }

    private void createShoreline(final Realm input) {
        final TileMap baseLayer = input.getTileManager().getBaseLayer();
        // left side
        for (int i = 0; i < input.getTileManager().getBaseLayer().getWidth(); i++) {
            for (int j = 0; j < 1 + Realm.RANDOM.nextInt(SHORE_LINE_SIZE); j++) {
                baseLayer.setTileAt(i, j, (short) Beach0Decorator.WATER_TILE.getTileId(),
                        Beach0Decorator.WATER_TILE.getData());
            }
        }
        // right side
        for (int i = 0; i < input.getTileManager().getBaseLayer().getWidth(); i++) {
            for (int j = 1; j < 2 + Realm.RANDOM.nextInt(SHORE_LINE_SIZE); j++) {
                baseLayer.setTileAt(i, input.getTileManager().getBaseLayer().getHeight() - (j),
                        (short) Beach0Decorator.WATER_TILE.getTileId(), Beach0Decorator.WATER_TILE.getData());
            }
        }

        // top side
        for (int i = 0; i < input.getTileManager().getBaseLayer().getWidth(); i++) {
            for (int j = 0; j < 1 + Realm.RANDOM.nextInt(SHORE_LINE_SIZE); j++) {
                baseLayer.setTileAt(j, i, (short) Beach0Decorator.WATER_TILE.getTileId(),
                        Beach0Decorator.WATER_TILE.getData());
            }
        }
        // bottom side
        for (int i = 0; i < input.getTileManager().getBaseLayer().getWidth(); i++) {
            for (int j = 1; j < 2 + Realm.RANDOM.nextInt(SHORE_LINE_SIZE); j++) {
                baseLayer.setTileAt(input.getTileManager().getBaseLayer().getHeight() - (j), i,
                        (short) Beach0Decorator.WATER_TILE.getTileId(), Beach0Decorator.WATER_TILE.getData());
            }
        }
    }

    @Override
    public Integer getTargetMapId() {
        return 2;
    }
}
