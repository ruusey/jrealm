package com.jrealm.game.util;

import com.jrealm.game.realm.Realm;
public class StatsThread extends Thread {
	private Realm toMonitor;

	public StatsThread(Realm realm) {
		this.toMonitor = realm;
	}

	@Override
	public void run() {
		while (true) {
			int bSize = this.toMonitor.getBullets().size();
			int eSize = this.toMonitor.getEnemies().size();
			int pSize = this.toMonitor.getPlayers().size();
			int lSize = this.toMonitor.getLoot().size();

			System.out.println("BULLETS:        " + bSize);
			System.out.println("ENEMIES:        " + eSize);
			System.out.println("PLAYERS:        " + pSize);
			System.out.println("LOOTContainers: " + lSize);

			try {
				Thread.sleep(2500);
			} catch (Exception e) {

			}
		}
	}

}
