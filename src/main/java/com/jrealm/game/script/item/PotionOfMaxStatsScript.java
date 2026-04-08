package com.jrealm.game.script.item;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.model.CharacterClassModel;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.packet.TextPacket;

public class PotionOfMaxStatsScript extends UseableItemScriptBase {

    private static final int ITEM_ID = 295;

    public PotionOfMaxStatsScript(RealmManagerServer mgr) {
        super(mgr);
    }

    @Override
    public int getTargetItemId() {
        return ITEM_ID;
    }

    @Override
    public void invokeUseItem(Realm targetRealm, Player player, GameItem item) {
        CharacterClassModel classModel = GameDataManager.CHARACTER_CLASSES.get(player.getClassId());
        if (classModel == null) return;
        player.setStats(classModel.getMaxStats().clone());
        player.setHealth(classModel.getMaxStats().getHp());
        player.setMana(classModel.getMaxStats().getMp());
        // Set XP to max level so player becomes level 20
        player.setExperience(GameDataManager.EXPERIENCE_LVLS.maxExperience());
        // Remove the consumed potion
        for (int i = 0; i < player.getInventory().length; i++) {
            if (player.getInventory()[i] != null && player.getInventory()[i].getItemId() == ITEM_ID) {
                player.getInventory()[i] = null;
                break;
            }
        }
        try {
            this.mgr.enqueueServerPacket(player,
                TextPacket.from("SYSTEM", player.getName(), "Your stats have been maxed!"));
        } catch (Exception e) {}
    }

    @Override
    public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem) {
    }
}
