package com.openrealm.game.script.item;

import com.openrealm.game.data.GameDataManager;
import com.openrealm.game.entity.Player;
import com.openrealm.game.entity.Portal;
import com.openrealm.game.entity.item.GameItem;
import com.openrealm.game.model.PortalModel;
import com.openrealm.net.realm.Realm;
import com.openrealm.net.realm.RealmManagerServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Item160Script extends UseableItemScriptBase {

	public Item160Script(final RealmManagerServer mgr) {
		super(mgr);
	}

	@Override
	public void invokeUseItem(final Realm targetRealm, final Player player, final GameItem item) {
		final PortalModel bossPortal = GameDataManager.PORTALS.get(5);
		final Portal toNewRealmPortal = new Portal(Realm.RANDOM.nextLong(), (short) bossPortal.getPortalId(),
				player.getPos().withNoise(64, 64));

		// Link portal to a new realm (will be created on use)
		toNewRealmPortal.linkPortal(targetRealm, null);
		log.info("New boss portal created at player position");
		targetRealm.addPortal(toNewRealmPortal);

		final int idxToRemove = player.findItemIndex(item);
		player.getInventory()[idxToRemove] = null;
	}

	@Override
	public void invokeItemAbility(final Realm targetRealm, final Player player, final GameItem abilityItem) {
		//No Op
	}

	@Override
	public int getTargetItemId() {
		return 160;
	}
}
