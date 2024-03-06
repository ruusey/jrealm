package com.jrealm.game.script.item;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;

public class Item156Script extends UseableItemScriptBase{

	public Item156Script(RealmManagerServer mgr) {
		super(mgr);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void invokeUseItem(Realm targetRealm, Player player, GameItem item) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem) {
		for (final Player other : targetRealm
				.getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(player))) {
			other.addEffect(abilityItem.getEffect().getEffectId(), abilityItem.getEffect().getDuration());
		}
	}

	@Override
	public int getTargetItemId() {
		// TODO Auto-generated method stub
		return 156;
	}

}
