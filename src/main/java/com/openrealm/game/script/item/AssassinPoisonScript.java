package com.openrealm.game.script.item;

import com.openrealm.game.entity.Player;
import com.openrealm.game.entity.item.GameItem;
import com.openrealm.game.math.Vector2f;
import com.openrealm.net.client.packet.CreateEffectPacket;
import com.openrealm.net.realm.Realm;
import com.openrealm.net.realm.RealmManagerServer;

/**
 * Assassin Poison ability (items 249-255, T0-T6).
 * Throws a poison vial at cursor position with a 0.8s travel time.
 * On landing, enemies in the AoE get POISONED and take DoT (ignores defense).
 *
 * The throw delay is tracked per tick on the Realm — no threads are blocked.
 */
public class AssassinPoisonScript extends UseableItemScriptBase {

    private static final int MIN_ID = 249;
    private static final int MAX_ID = 255;
    private static final float POISON_RADIUS = 128.0f;
    private static final long THROW_DURATION_MS = 800;

    public AssassinPoisonScript(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public boolean handles(int itemId) {
        return itemId >= MIN_ID && itemId <= MAX_ID;
    }

    @Override
    public int getTargetItemId() {
        return MIN_ID;
    }

    @Override
    public void invokeUseItem(Realm targetRealm, Player player, GameItem item) {
    }

    @Override
    public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem) {
        invokeItemAbility(targetRealm, player, abilityItem,
                player.getPos().clone(player.getSize() / 2, player.getSize() / 2));
    }

    @Override
    public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem, Vector2f targetPos) {
        final Vector2f center = (targetPos != null) ? targetPos
                : player.getPos().clone(player.getSize() / 2, player.getSize() / 2);

        int tier = abilityItem.getItemId() - MIN_ID;
        final int totalDamage = 150 + tier * 150 + player.getComputedStats().getAtt();
        final long poisonDuration = 3000 + tier * 250;

        // Broadcast the throw arc visual (800ms travel time)
        final Vector2f playerCenter = player.getPos().clone(player.getSize() / 2, player.getSize() / 2);
        this.mgr.enqueueServerPacket(CreateEffectPacket.lineEffect(
                CreateEffectPacket.EFFECT_POISON_SPLASH,
                playerCenter.x, playerCenter.y, center.x, center.y, (short) THROW_DURATION_MS));

        // Register the pending throw — Realm will process the landing on tick after 800ms
        targetRealm.registerPoisonThrow(THROW_DURATION_MS, player.getId(),
                center.x, center.y, POISON_RADIUS, totalDamage, poisonDuration);
    }
}
