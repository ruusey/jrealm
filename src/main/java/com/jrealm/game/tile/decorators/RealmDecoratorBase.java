package com.jrealm.game.tile.decorators;

import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;

import lombok.Data;

@Data
public class RealmDecoratorBase implements RealmDecorator{
	private RealmManagerServer mgr;
	public RealmDecoratorBase(RealmManagerServer mgr) {
		this.mgr = mgr;
	}
	@Override
	public void decorate(Realm input) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Integer getTargetMapId() {
		// TODO Auto-generated method stub
		return null;
	}
}
