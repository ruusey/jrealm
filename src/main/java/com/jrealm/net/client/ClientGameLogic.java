package com.jrealm.net.client;

import com.jrealm.game.GameLauncher;
import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.messaging.CommandType;
import com.jrealm.game.messaging.LoginResponseMessage;
import com.jrealm.game.realm.RealmManagerClient;
import com.jrealm.game.util.Camera;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.ObjectMovement;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientGameLogic {
	public static void handleLoadClient(RealmManagerClient cli, Packet packet) {
		LoadPacket loadPacket = (LoadPacket) packet;
		try {
//			log.info("[CLIENT] Recieved Load Packet \nPlayers: {}\nEnemies: {}\nBullets: {}\nLootContainers: {}",
//					textPacket.getPlayers().length, textPacket.getEnemies().length, textPacket.getBullets().length,
//					textPacket.getContainers().length);

			for(Player p : loadPacket.getPlayers()) {
				if(p.getId()==cli.getCurrentPlayerId()) {
					continue;
				}
				cli.getRealm().addPlayerIfNotExists(p);
			}
			for(LootContainer lc : loadPacket.getContainers()) {
				if(lc.getContentsChanged()) {
					LootContainer current = cli.getRealm().getLoot().get(lc.getLootContainerId());
					current.setContentsChanged(true);
					current.setItemsUncondensed(lc.getItems());
				}else {
					cli.getRealm().addLootContainerIfNotExists(lc);
				}
			}

			for(Bullet b : loadPacket.getBullets()) {
				cli.getRealm().addBulletIfNotExists(b);
			}

			for(Enemy e : loadPacket.getEnemies()) {
				cli.getRealm().addEnemyIfNotExists(e);
			}
		}catch(Exception e) {
			log.error("Failed to handle Load Packet. Reason: {}", e);
		}
	}

	public static void handleUnloadClient(RealmManagerClient cli, Packet packet) {
		UnloadPacket unloadPacket = (UnloadPacket) packet;
		//log.info("[CLIENT] Recieved Unload Packet");
		try {

			for(Long p : unloadPacket.getPlayers()) {
				if(p==cli.getCurrentPlayerId()) {
					continue;
				}
				cli.getRealm().getPlayers().remove(p);
			}
			for(Long lc : unloadPacket.getContainers()) {
				cli.getRealm().getLoot().remove(lc);
			}

			for(Long b : unloadPacket.getBullets()) {
				cli.getRealm().getBullets().remove(b);
			}

			for(Long e : unloadPacket.getEnemies()) {
				cli.getRealm().getEnemies().remove(e);
			}

		}catch(Exception e) {
			log.error("Failed to handle Unload Packet. Reason: {}", e);
		}
	}

	public static void handleTextClient(RealmManagerClient cli, Packet packet) {
		TextPacket textPacket = (TextPacket) packet;
		log.info("[CLIENT] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
				textPacket.getFrom(), textPacket.getMessage());
		try {
			cli.getState().getPui().enqueueChat(textPacket.clone());
		}catch(Exception e) {
			log.error("Failed to response to initial text packet. Reason: {}", e.getMessage());
		}
	}

	public static void handleCommandClient(RealmManagerClient cli, Packet packet) {
		CommandPacket commandPacket = (CommandPacket) packet;
		log.info("[CLIENT] Recieved Command Packet for Player {} Command={}", commandPacket.getPlayerId(), commandPacket.getCommand());
		try {
			switch(commandPacket.getCommandId()) {
			case 2:
				LoginResponseMessage loginResponse = CommandType.fromPacket(commandPacket);
				ClientGameLogic.doLoginResponse(cli, loginResponse);
				break;
			}
		}catch(Exception e) {
			log.error("Failed to handle client command packet. Reason: {}", e.getMessage());
		}
	}

	public static void handleObjectMoveClient(RealmManagerClient cli, Packet packet) {
		ObjectMovePacket objectMovePacket = (ObjectMovePacket) packet;
		for(ObjectMovement movement : objectMovePacket.getMovements()) {
			switch(movement.getTargetEntityType()) {
			case PLAYER:
				Player playerToUpdate = cli.getRealm().getPlayer(movement.getEntityId());
				if(playerToUpdate==null) {
					break;
				}
				if(cli.getCurrentPlayerId()==movement.getEntityId()) {
					playerToUpdate.applyMovementLerp(movement);
				}else {
					playerToUpdate.applyMovementLerp(movement);
				}
				break;
			case ENEMY:
				Enemy enemyToUpdate = cli.getRealm().getEnemy(movement.getEntityId());
				if(enemyToUpdate == null) {
					break;
				}
				enemyToUpdate.applyMovementLerp(movement);
				break;
			case BULLET:
				Bullet bulletToUpdate = cli.getRealm().getBullet(movement.getEntityId());
				if(bulletToUpdate==null) {
					break;
				}
				bulletToUpdate.applyMovementLerp(movement);
				break;
			default:
				break;
			}
		}
	}

	public static void handleUpdateClient(RealmManagerClient cli, Packet packet) {
		UpdatePacket updatePacket = (UpdatePacket) packet;
		Player toUpdate = cli.getRealm().getPlayer((updatePacket.getPlayerId()));
		if (toUpdate == null)
			return;
		toUpdate.applyUpdate(updatePacket, cli.getState());
		//log.info("[CLIENT] Recieved PlayerUpdate Packet for Player ID {}", updatePacket.getPlayerId());
	}

	private static void doLoginResponse(RealmManagerClient cli, LoginResponseMessage loginResponse) {
		try {
			if(loginResponse.isSuccess()) {
				Camera c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
				CharacterClass cls = CharacterClass.valueOf(loginResponse.getClassId());
				Player player = new Player(loginResponse.getPlayerId(), c, GameDataManager.loadClassSprites(cls),
						new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
								(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
						GlobalConstants.PLAYER_SIZE, cls);
				log.info("Login succesful, added Player ID {}", player.getId());
				cli.getState().loadClass(player, cls, true);
				cli.setCurrentPlayerId(player.getId());
				cli.getState().setPlayerId(player.getId());
				cli.startHeartbeatThread();
				TextPacket packet = TextPacket.create("SYSTEM", "Player", "Welcome to JRealm "+GameLauncher.GAME_VERSION+"!");
				cli.getState().getPui().enqueueChat(packet);
			}
		}catch(Exception e) {
			log.error("Failed to response to login response. Reason: {}", e.getMessage());
		}
	}
}
