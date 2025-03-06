package com.jrealm.game.script.item;

import java.util.Optional;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.model.PortalModel;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;

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

		final Optional<Realm> realmAtDepth = this.mgr.findRealmAtDepth(bossPortal.getTargetRealmDepth() - 1);
		if (realmAtDepth.isEmpty()) {
			toNewRealmPortal.linkPortal(targetRealm, null);
			log.info("New portal created. Will generate realm id {} on use", bossPortal.getTargetRealmDepth());

		} else {
			toNewRealmPortal.linkPortal(targetRealm, realmAtDepth.get());
			log.info("Linking Portal {} to existing realm {}", toNewRealmPortal, realmAtDepth.get().getRealmId());
		}
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
