package com.jrealm.net.client;

import com.jrealm.game.GameLauncher;
import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.messaging.CommandType;
import com.jrealm.game.messaging.LoginResponseMessage;
import com.jrealm.game.realm.RealmManagerClient;
import com.jrealm.game.ui.DamageText;
import com.jrealm.game.ui.TextEffect;
import com.jrealm.game.util.Camera;
import com.jrealm.net.EntityType;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.ObjectMovement;
import com.jrealm.net.client.packet.TextEffectPacket;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientGameLogic {
	public static void handleTextEffectClient(RealmManagerClient cli, Packet packet) {
		final TextEffectPacket textEffect = (TextEffectPacket) packet;
		try {
			final Vector2f sourcePos = cli.getState().getPlayer().getPos();
			final DamageText hitText = DamageText.builder().damage(textEffect.getText())
					.effect(TextEffect.fromOrdinal(textEffect.getTextEffectId())).sourcePos(sourcePos).build();
			cli.getState().getDamageText().add(hitText);
		} catch (Exception e) {
			ClientGameLogic.log.error("Failed to handle LoadMap Packet. Reason: {}", e);
		}
	}

	public static void handleLoadMapClient(RealmManagerClient cli, Packet packet) {
		final LoadMapPacket loadPacket = (LoadMapPacket) packet;
		try {
			//			log.info("[CLIENT] Recieved Load Packet \nPlayers: {}\nEnemies: {}\nBullets: {}\nLootContainers: {}",
			//
			cli.getRealm().setRealmId(loadPacket.getRealmId());
			cli.getRealm().getTileManager().mergeMap(loadPacket);
		} catch (Exception e) {
			ClientGameLogic.log.error("Failed to handle LoadMap Packet. Reason: {}", e);
		}
	}
	public static void handleLoadClient(RealmManagerClient cli, Packet packet) {
		final LoadPacket loadPacket = (LoadPacket) packet;
		try {
			//			log.info("[CLIENT] Recieved Load Packet \nPlayers: {}\nEnemies: {}\nBullets: {}\nLootContainers: {}",
			//					textPacket.getPlayers().length, textPacket.getEnemies().length, textPacket.getBullets().length,
			//					textPacket.getContainers().length);

			for (final Player p : loadPacket.getPlayers()) {
				if(p.getId()==cli.getCurrentPlayerId()) {
					continue;
				}
				cli.getRealm().addPlayerIfNotExists(p);
			}
			for (final LootContainer lc : loadPacket.getContainers()) {
				if(lc.getContentsChanged()) {
					LootContainer current = cli.getRealm().getLoot().get(lc.getLootContainerId());
					current.setContentsChanged(true);
					current.setItemsUncondensed(lc.getItems());
				}else {
					cli.getRealm().addLootContainerIfNotExists(lc);
				}
			}

			for (final Bullet b : loadPacket.getBullets()) {
				cli.getRealm().addBulletIfNotExists(b);
			}

			for (final Enemy e : loadPacket.getEnemies()) {
				cli.getRealm().addEnemyIfNotExists(e);
			}

			for (final Portal p : loadPacket.getPortals()) {
				cli.getRealm().addPortalIfNotExists(p);
			}
		}catch(Exception e) {
			ClientGameLogic.log.error("Failed to handle Load Packet. Reason: {}", e);
		}
	}

	public static void handleUnloadClient(RealmManagerClient cli, Packet packet) {
		final UnloadPacket unloadPacket = (UnloadPacket) packet;
		//log.info("[CLIENT] Recieved Unload Packet");
		try {

			for (final Long p : unloadPacket.getPlayers()) {
				if(p==cli.getCurrentPlayerId()) {
					continue;
				}
				final Player removed = cli.getRealm().getPlayers().remove(p);
				if (removed == null) {
					ClientGameLogic.log.error("Player {} does not exist", p);
				}
			}
			for (final Long lc : unloadPacket.getContainers()) {
				final LootContainer removed = cli.getRealm().getLoot().remove(lc);
				if (removed == null) {
					ClientGameLogic.log.error("LootContainer {} does not exist", lc);
				}
			}

			for (final Long b : unloadPacket.getBullets()) {
				final Bullet removed = cli.getRealm().getBullets().remove(b);
				if (removed == null) {
					ClientGameLogic.log.error("Bullet {} does not exist", b);
				}
			}

			for (final Long e : unloadPacket.getEnemies()) {
				final Enemy removed = cli.getRealm().getEnemies().remove(e);
				if (removed == null) {
					ClientGameLogic.log.error("Enemy {} does not exist", e);
				}
			}

			for (final Long p : unloadPacket.getPortals()) {
				final Portal removed = cli.getRealm().getPortals().remove(p);
				if (removed == null) {
					ClientGameLogic.log.error("Portal {} does not exist", p);
				}
			}

		}catch(Exception e) {
			ClientGameLogic.log.error("Failed to handle Unload Packet. Reason: {}", e);
		}
	}

	public static void handleTextClient(RealmManagerClient cli, Packet packet) {
		final TextPacket textPacket = (TextPacket) packet;
		ClientGameLogic.log.info("[CLIENT] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
				textPacket.getFrom(), textPacket.getMessage());
		try {
			cli.getState().getPui().enqueueChat(textPacket.clone());
		}catch(Exception e) {
			ClientGameLogic.log.error("Failed to response to initial text packet. Reason: {}", e.getMessage());
		}
	}

	public static void handleCommandClient(RealmManagerClient cli, Packet packet) {
		final CommandPacket commandPacket = (CommandPacket) packet;
		ClientGameLogic.log.info("[CLIENT] Recieved Command Packet for Player {} Command={}", commandPacket.getPlayerId(), commandPacket.getCommand());
		try {
			switch(commandPacket.getCommandId()) {
			case 2:
				final LoginResponseMessage loginResponse = CommandType.fromPacket(commandPacket);
				ClientGameLogic.doLoginResponse(cli, loginResponse);
				break;
			}
		}catch(Exception e) {
			ClientGameLogic.log.error("Failed to handle client command packet. Reason: {}", e.getMessage());
		}
	}

	public static void handleObjectMoveClient(RealmManagerClient cli, Packet packet) {
		final ObjectMovePacket objectMovePacket = (ObjectMovePacket) packet;
		for(ObjectMovement movement : objectMovePacket.getMovements()) {
			final EntityType type = movement.getTargetEntityType();
			if(type==null) {
				continue;
			}
			switch(type) {
			case PLAYER:
				final Player playerToUpdate = cli.getRealm().getPlayer(movement.getEntityId());
				if(playerToUpdate==null) {
					break;
				}
				if(cli.getCurrentPlayerId()==movement.getEntityId()) {
					playerToUpdate.applyMovementLerp(movement, 1.0f);
				}else {
					playerToUpdate.applyMovementLerp(movement);
				}
				break;
			case ENEMY:
				final Enemy enemyToUpdate = cli.getRealm().getEnemy(movement.getEntityId());
				if(enemyToUpdate == null) {
					break;
				}
				enemyToUpdate.applyMovementLerp(movement);
				break;
			case BULLET:
				final Bullet bulletToUpdate = cli.getRealm().getBullet(movement.getEntityId());
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
						new Vector2f(loginResponse.getSpawnX(), loginResponse.getSpawnY()),
						GlobalConstants.PLAYER_SIZE, cls);
				ClientGameLogic.log.info("Login succesful, added Player ID {}", player.getId());
				player.getCam().target(player);

				cli.getState().loadClass(player, cls, true);
				cli.setCurrentPlayerId(player.getId());
				cli.getState().setPlayerId(player.getId());
				cli.startHeartbeatThread();
				TextPacket packet = TextPacket.create("SYSTEM", "Player", "Welcome to JRealm "+GameLauncher.GAME_VERSION+"!");
				cli.getState().getPui().enqueueChat(packet);
			}
		}catch(Exception e) {
			ClientGameLogic.log.error("Failed to response to login response. Reason: {}", e.getMessage());
		}
	}
}
