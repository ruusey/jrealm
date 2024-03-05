package com.jrealm.game.script.item;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;

public class Item157Script extends UseableItemScriptBase {

	public Item157Script(RealmManagerServer mgr) {
		super(mgr);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void invokeUseItem(Realm targetRealm, Player player, GameItem item) {
		// TODO Auto-generated method stub
	}

	@Override
	public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem) {
		final Effect effect = abilityItem.getEffect();
		player.addEffect(effect.getEffectId(), effect.getDuration());
		int healthDiff = player.getComputedStats().getHp() - player.getHealth();
		if (healthDiff > 0) {
			int healthToAdd = healthDiff < 50 ? healthDiff : 50;
			player.setHealth(player.getHealth() + healthToAdd);
		}
		for (final Player other : targetRealm
				.getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(player))) {
			healthDiff = player.getComputedStats().getHp() - player.getHealth();
			if (healthDiff > 0) {
				int healthToAdd = healthDiff < 50 ? healthDiff : 50;
				other.setHealth(other.getHealth() + healthToAdd);
			}
		}
	}

	@Override
	public int getTargetItemId() {
		// TODO Auto-generated method stub
		return 157;
	}
}
