package com.jrealm.game.tile.decorators;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.util.GameObjectUtils;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

public class BossRoomDecorator extends RealmDecoratorBase{

    public BossRoomDecorator(RealmManagerServer mgr) {
        super(mgr);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void decorate(Realm input) {
        final Vector2f spawnPos = new Vector2f(GlobalConstants.BASE_TILE_SIZE * 12,
                GlobalConstants.BASE_TILE_SIZE * 13);

        final Portal exitPortal = new Portal(Realm.RANDOM.nextLong(), (short) 3, spawnPos.clone(250, 0));
        exitPortal.linkPortal(input, mgr.getTopRealm());
        exitPortal.setNeverExpires();

        final Enemy enemy = GameObjectUtils.getEnemyFromId(13, spawnPos);
        enemy.setHealth(enemy.getHealth() * 4);
        enemy.setPos(spawnPos.clone(200, 0));

        input.addEnemy(enemy);
        input.addPortal(exitPortal);        
    }

    @Override
    public Integer getTargetMapId() {
        // TODO Auto-generated method stub
        return 5;
    }

}
