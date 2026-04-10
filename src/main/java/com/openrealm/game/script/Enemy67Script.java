package com.openrealm.game.script;

import com.openrealm.game.contants.EntityType;
import com.openrealm.game.contants.TextEffect;
import com.openrealm.game.entity.Enemy;
import com.openrealm.game.entity.Player;
import com.openrealm.game.math.Vector2f;
import com.openrealm.net.client.packet.CreateEffectPacket;
import com.openrealm.net.realm.Realm;
import com.openrealm.net.realm.RealmManagerServer;

/**
 * Healer NPC (Enemy 67) — friendly entity that heals nearby players' HP and MP.
 * Spawns in the vault via static spawns. Does not attack or spawn projectiles.
 */
public class Enemy67Script extends EnemyScriptBase {

    private static final int HEAL_AMOUNT = 80;
    private static final float HEAL_RADIUS = 224.0f;

    public Enemy67Script(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public int getTargetEnemyId() {
        return 67;
    }

    @Override
    public void attack(final Realm targetRealm, final Enemy enemy, final Player targetPlayer) throws Exception {
        final Vector2f center = enemy.getPos().clone(enemy.getSize() / 2, enemy.getSize() / 2);

        // Broadcast heal radius visual
        this.getMgr().enqueueServerPacket(CreateEffectPacket.aoeEffect(
                CreateEffectPacket.EFFECT_HEAL_RADIUS, center.x, center.y, HEAL_RADIUS, (short) 1500));

        // Heal all players within range
        final float radiusSq = HEAL_RADIUS * HEAL_RADIUS;
        for (final Player player : targetRealm.getPlayers().values()) {
            float dx = player.getPos().x - center.x;
            float dy = player.getPos().y - center.y;
            if (dx * dx + dy * dy > radiusSq) continue;

            int maxHp = player.getComputedStats().getHp();
            int missingHp = maxHp - player.getHealth();
            if (missingHp > 0) {
                int toHeal = Math.min(HEAL_AMOUNT, missingHp);
                player.setHealth(player.getHealth() + toHeal);
                this.getMgr().broadcastTextEffect(EntityType.PLAYER, player, TextEffect.HEAL, "+" + toHeal);
            }
            // Restore mana
            int maxMp = player.getComputedStats().getMp();
            int missingMp = maxMp - player.getMana();
            if (missingMp > 0) {
                int toRestore = Math.min(HEAL_AMOUNT, missingMp);
                player.setMana(player.getMana() + toRestore);
                this.getMgr().broadcastTextEffect(EntityType.PLAYER, player, TextEffect.HEAL, "+" + toRestore + " MP");
            }
        }
    }
}
