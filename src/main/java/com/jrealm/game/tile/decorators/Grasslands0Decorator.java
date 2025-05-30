package com.jrealm.game.tile.decorators;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.TileModel;
import com.jrealm.game.tile.TileMap;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

public class Grasslands0Decorator extends RealmDecoratorBase {
    private static Integer MIN_FOREST_COUNT = 15;
    private static Integer MAX_FOREST_COUNT = 25;
    private static final TileModel TREE_10 = GameDataManager.TILES.get(38);
    private static final TileModel TREE_11 = GameDataManager.TILES.get(39);

    public Grasslands0Decorator(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public void decorate(final Realm input) {
        MIN_FOREST_COUNT = input.getTileManager().getBaseLayer().getWidth()/2;
        MAX_FOREST_COUNT = (input.getTileManager().getBaseLayer().getWidth()/2)+15;
        
        
        for (int i = 0; i < (Grasslands0Decorator.MIN_FOREST_COUNT + Realm.RANDOM
                .nextInt(Grasslands0Decorator.MAX_FOREST_COUNT - Grasslands0Decorator.MIN_FOREST_COUNT)); i++) {
            final Vector2f pos = input.getTileManager().randomPos();
            final TileMap collisionLayer = input.getTileManager().getCollisionLayer();
            final int centerX = (int) (pos.x / collisionLayer.getTileSize());
            final int centerY = (int) (pos.y / collisionLayer.getTileSize());

            collisionLayer.setTileAt(centerY, centerX, (short) Grasslands0Decorator.TREE_11.getTileId(),
                    Grasslands0Decorator.TREE_11.getData());
            collisionLayer.setTileAt(centerY, (centerX - 1) > -1 ? centerX - 1 : 0,
                    (short) Grasslands0Decorator.TREE_10.getTileId(), Grasslands0Decorator.TREE_10.getData());
            collisionLayer.setTileAt(centerY,
                    (centerX + 1) >= collisionLayer.getHeight() ? collisionLayer.getHeight() - 1 : centerX + 1,
                    (short) Grasslands0Decorator.TREE_10.getTileId(), Grasslands0Decorator.TREE_10.getData());
            collisionLayer.setTileAt((centerY - 1) > -1 ? centerY - 1 : 0, centerX,
                    (short) Grasslands0Decorator.TREE_10.getTileId(), Grasslands0Decorator.TREE_10.getData());
            collisionLayer.setTileAt(
                    (centerY + 1) >= collisionLayer.getWidth() ? collisionLayer.getWidth() - 1 : centerY + 1, centerX,
                    (short) Grasslands0Decorator.TREE_10.getTileId(), Grasslands0Decorator.TREE_10.getData());
        }
    }

    @Override
    public Integer getTargetMapId() {
        return 4;
    }

}
