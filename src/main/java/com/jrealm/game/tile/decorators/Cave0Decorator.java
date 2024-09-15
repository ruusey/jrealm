package com.jrealm.game.tile.decorators;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.TileModel;
import com.jrealm.game.tile.TileMap;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

public class Cave0Decorator extends RealmDecoratorBase {
    private static  Integer MIN_LAVA_POOL_COUNT = 15;
    private static  Integer MAX_LAVA_POOL_COUNT = 25;
    private static final TileModel LAVA_TILE0 = GameDataManager.TILES.get(47);
    private static final TileModel LAVA_TILE1 = GameDataManager.TILES.get(48);
    
    public Cave0Decorator(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public void decorate(final Realm input) {
        MIN_LAVA_POOL_COUNT = input.getTileManager().getBaseLayer().getWidth()/2;
        MAX_LAVA_POOL_COUNT = (input.getTileManager().getBaseLayer().getWidth()/2)+15;

        for (int i = 0; i < (Cave0Decorator.MIN_LAVA_POOL_COUNT + Realm.RANDOM
                .nextInt(Cave0Decorator.MAX_LAVA_POOL_COUNT - Cave0Decorator.MIN_LAVA_POOL_COUNT)); i++) {
            final Vector2f pos = input.getTileManager().randomPos();
            final TileMap baseLayer = input.getTileManager().getBaseLayer();
            final int centerX = (int) (pos.x / baseLayer.getTileSize());
            final int centerY = (int) (pos.y / baseLayer.getTileSize());

            baseLayer.setTileAt(centerY, centerX, (short) Cave0Decorator.LAVA_TILE1.getTileId(),
                    Cave0Decorator.LAVA_TILE1.getData());
            baseLayer.setTileAt(centerY, (centerX - 1) > -1 ? centerX - 1 : 0,
                    (short) Cave0Decorator.LAVA_TILE0.getTileId(), Cave0Decorator.LAVA_TILE0.getData());
            baseLayer.setTileAt(centerY, (centerX - 2) > -1 ? centerX - 2 : 0,
                    (short) Cave0Decorator.LAVA_TILE0.getTileId(), Cave0Decorator.LAVA_TILE0.getData());
            baseLayer.setTileAt(centerY,
                    (centerX + 1) >= baseLayer.getHeight() ? baseLayer.getHeight() - 1 : centerX + 1,
                    (short) Cave0Decorator.LAVA_TILE0.getTileId(), Cave0Decorator.LAVA_TILE0.getData());
            baseLayer.setTileAt(centerY,
                    (centerX + 2) >= baseLayer.getHeight() ? baseLayer.getHeight() - 1 : centerX + 2,
                    (short) Cave0Decorator.LAVA_TILE0.getTileId(), Cave0Decorator.LAVA_TILE0.getData());
//            baseLayer.setTileAt((centerY - 1) > -1 ? centerX - 1 : 0, centerX,
//                    (short) Cave0Decorator.LAVA_TILE0.getTileId(), Cave0Decorator.LAVA_TILE0.getData());
//            baseLayer.setTileAt((centerY + 1) >= baseLayer.getWidth() ? baseLayer.getWidth() - 1 : centerY + 1,
//                    centerX, (short) Cave0Decorator.LAVA_TILE0.getTileId(), Cave0Decorator.LAVA_TILE0.getData());

        }
    }

    @Override
    public Integer getTargetMapId() {
        return 3;
    }

}
