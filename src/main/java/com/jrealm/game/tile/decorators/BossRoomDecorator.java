package com.jrealm.game.tile.decorators;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.util.GameObjectUtils;

public class BossRoomDecorator extends RealmDecoratorBase {

    private static final int BOSS_ENEMY_ID = 13;
    private static final int[] MINION_ENEMY_IDS = {2, 3, 2, 3};
    private static final float MINION_OFFSET = 160f;

    public BossRoomDecorator(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public void decorate(Realm input) {
        Vector2f spawnPos = input.getTileManager().getBossSpawnPos();
        if (spawnPos == null) {
            spawnPos = new Vector2f(GlobalConstants.BASE_TILE_SIZE * 16,
                    GlobalConstants.BASE_TILE_SIZE * 16);
        }

        final Portal exitPortal = new Portal(Realm.RANDOM.nextLong(), (short) 3, spawnPos.clone(250, 0));
        exitPortal.linkPortal(input, mgr.getTopRealm());
        exitPortal.setNeverExpires();

        final Enemy boss = GameObjectUtils.getEnemyFromId(BOSS_ENEMY_ID, spawnPos);
        boss.setHealth(boss.getHealth() * 4);
        boss.getStats().setHp((short) (boss.getStats().getHp() * 4));
        input.addEnemy(boss);

        // Spawn 4 minions at cardinal positions around the boss
        float[][] offsets = {
            {0, -MINION_OFFSET},
            {0, MINION_OFFSET},
            {-MINION_OFFSET, 0},
            {MINION_OFFSET, 0}
        };
        for (int i = 0; i < MINION_ENEMY_IDS.length; i++) {
            Vector2f minionPos = new Vector2f(spawnPos.x + offsets[i][0], spawnPos.y + offsets[i][1]);
            final Enemy minion = GameObjectUtils.getEnemyFromId(MINION_ENEMY_IDS[i], minionPos);
            minion.setHealth(minion.getHealth() * 2);
            minion.getStats().setHp((short) (minion.getStats().getHp() * 2));
            input.addEnemy(minion);
        }

        input.addPortal(exitPortal);
    }

    @Override
    public Integer getTargetMapId() {
        return 5;
    }

}
