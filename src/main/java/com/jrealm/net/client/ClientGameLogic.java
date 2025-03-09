package com.jrealm.net.client;

import com.jrealm.account.service.JrealmClientDataService;
import com.jrealm.game.GameLauncher;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EntityType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.TextEffect;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.state.GameStateManager;
import com.jrealm.game.ui.EffectText;
import com.jrealm.game.util.PacketHandlerClient;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.AcceptTradeRequestPacket;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.ObjectMovement;
import com.jrealm.net.client.packet.PlayerDeathPacket;
import com.jrealm.net.client.packet.RequestTradePacket;
import com.jrealm.net.client.packet.TextEffectPacket;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.client.packet.UpdatePlayerTradeSelectionPacket;
import com.jrealm.net.client.packet.UpdateTradePacket;
import com.jrealm.net.core.IOService;
import com.jrealm.net.entity.NetBullet;
import com.jrealm.net.entity.NetEnemy;
import com.jrealm.net.entity.NetInventorySelection;
import com.jrealm.net.entity.NetLootContainer;
import com.jrealm.net.entity.NetPlayer;
import com.jrealm.net.entity.NetPortal;
import com.jrealm.net.entity.NetTradeSelection;
import com.jrealm.net.messaging.CommandType;
import com.jrealm.net.messaging.LoginResponseMessage;
import com.jrealm.net.messaging.PlayerAccountMessage;
import com.jrealm.net.messaging.ServerErrorMessage;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerClient;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.DeathAckPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientGameLogic {
	public static JrealmClientDataService DATA_SERVICE = null;

	@PacketHandlerClient(RequestTradePacket.class)
	public static void handleTradeRequestClient(RealmManagerClient cli, Packet packet) {
		final RequestTradePacket tradeRequest = (RequestTradePacket) packet;
		cli.getState().getPui().getPlayerChat().addChatMessage(TextPacket.create(tradeRequest.getRequestingPlayerName(),
				cli.getState().getPlayer().getName(),
				tradeRequest.getRequestingPlayerName() + " has proposed a trade, type /accept to initiate the trade"));

	}
	
	@PacketHandlerClient(AcceptTradeRequestPacket.class)
	public static void handleAcceptTrade(RealmManagerClient cli, Packet packet) {
		final AcceptTradeRequestPacket tradeRequest = (AcceptTradeRequestPacket) packet;
		log.info("[CLIENT] Recieved new trade accepted packet. Accepted = {}", tradeRequest.isAccepted());

		cli.getState().getPui().setTrading(tradeRequest.isAccepted());
	}

	@PacketHandlerClient(UpdatePlayerTradeSelectionPacket.class)
	public static void handleUpdateTradeSelection(RealmManagerClient mgr, Packet packet) {
		final UpdatePlayerTradeSelectionPacket updateTrade = (UpdatePlayerTradeSelectionPacket) packet;
		final NetInventorySelection selection = updateTrade.getSelection();

		mgr.getState().getPui().getCurrentTradeSelection().applyUpdate(selection);
	}

	@PacketHandlerClient(UpdateTradePacket.class)
	public static void handleUpdateTrade(RealmManagerClient mgr, Packet packet) {
		final UpdateTradePacket updateTrade = (UpdateTradePacket) packet;
		final NetTradeSelection selection = updateTrade.getSelections();

		NetTradeSelection currSelection = mgr.getState().getPui().getCurrentTradeSelection();
		if(currSelection==null) {
			currSelection = selection;
			mgr.getState().getPui().setCurrentTradeSelection(selection);
		}else {
			currSelection.applyUpdate(selection);
		}

	}

	public static void handlePlayerDeathClient(RealmManagerClient cli, Packet packet) {
		@SuppressWarnings("unused")
		final PlayerDeathPacket playerDeath = (PlayerDeathPacket) packet;
		try {
			cli.getClient().sendRemote(new DeathAckPacket(cli.getState().getPlayer().getId()));
			// Give the client time to send the death acknowledgement... this is just a courtesy to the server
			// ??Better way to do this??
			Thread.sleep(100);
			 
			cli.getState().getRealmManager().getClient().setShutdown(true);
			cli.getState().getRealmManager().getWorkerThread().setShutdown(true);
			cli.getState().gsm.add(GameStateManager.GAMEOVER);
			cli.getState().gsm.pop(GameStateManager.PLAY);
		} catch (Exception e) {
			ClientGameLogic.log.error("[CLIENT] Failed to handle PlayerDeath Packet. Reason: {}", e);
		}
	}

	public static void handleTextEffectClient(RealmManagerClient cli, Packet packet) {
		final TextEffectPacket textEffect = (TextEffectPacket) packet;
		try {
			final Realm clientRealm = cli.getState().getRealmManager().getRealm();
			Vector2f targetPos = null;
			try {
				switch (EntityType.valueOf(textEffect.getEntityType())) {
				case BULLET:
					final Bullet b = clientRealm.getBullet(textEffect.getTargetEntityId());
					if (b == null) {
						ClientGameLogic.log.warn("[CLIENT] Bullet with id {} was not found for targeted TextEffect",
								textEffect.getTargetEntityId());
						return;
					}
					targetPos = b.getPos();

					break;
				case ENEMY:
					final Enemy e = clientRealm.getEnemy(textEffect.getTargetEntityId());
					if (e == null) {
						ClientGameLogic.log.warn("[CLIENT] Enemy with id {} was not found for targeted TextEffect",
								textEffect.getTargetEntityId());
						return;
					}
					targetPos = e.getPos();
					break;
				case PLAYER:
					final Player p = clientRealm.getPlayer(textEffect.getTargetEntityId());
					if (p == null) {
						ClientGameLogic.log.warn("[CLIENT] Player with id {} was not found for targeted TextEffect",
								textEffect.getTargetEntityId());
						return;
					}
					targetPos = p.getPos();
					break;
				default:
					break;
				}

				final EffectText hitText = EffectText.builder().damage(textEffect.getText())
						.effect(TextEffect.from(textEffect.getTextEffectId())).sourcePos(targetPos).build();
				cli.getState().getDamageText().add(hitText);
			} catch (Exception e) {
				ClientGameLogic.log.error("[CLIENT] Failed to create client TextEffect. Reason: {}", e);
			}

		} catch (Exception e) {
			ClientGameLogic.log.error("[CLIENT] Failed to handle TextEffect Packet. Reason: {}", e);
		}
	}

	public static void handleLoadMapClient(RealmManagerClient cli, Packet packet) {
		final LoadMapPacket loadPacket = (LoadMapPacket) packet;
		try {
			cli.getState().getPui().getMinimap().initializeMap((int) loadPacket.getMapId());
			cli.getRealm().setRealmId(loadPacket.getRealmId());
			cli.getRealm().setMapId(loadPacket.getMapId());
			cli.getRealm().getTileManager().mergeMap(loadPacket);
		} catch (Exception e) {
			ClientGameLogic.log.error("[CLIENT] Failed to handle LoadMap Packet. Reason: {}", e);
		}
	}

	public static void handleLoadClient(RealmManagerClient cli, Packet packet) {
		final LoadPacket loadPacket = (LoadPacket) packet;
		try {
			for (final NetPlayer player : loadPacket.getPlayers()) {
				Player p = player.toPlayer();

				if (p.getId() == cli.getCurrentPlayerId()) {
					continue;
				}
				cli.getRealm().addPlayerIfNotExists(p);
			}
			for (final NetLootContainer loot : loadPacket.getContainers()) {
				final LootContainer lc = loot.asLootContainer();
				if (lc.getContentsChanged()) {
					LootContainer current = cli.getRealm().getLoot().get(lc.getLootContainerId());
					if (current == null) {
						cli.getRealm().addLootContainerIfNotExists(lc);
						// current = cli.getRealm().getLoot().get(lc.getLootContainerId());
					} else {
						current.setContentsChanged(true);
						current.setItemsUncondensed(LootContainer.getCondensedItems(lc));
					}

				} else {
					cli.getRealm().addLootContainerIfNotExists(lc);
				}
			}

			for (final NetBullet bullet : loadPacket.getBullets()) {
				final Bullet b = bullet.asBullet();
				cli.getRealm().addBulletIfNotExists(b);
			}

			for (final NetEnemy enemy : loadPacket.getEnemies()) {
				final Enemy e = IOService.mapModel(enemy, Enemy.class);
				cli.getRealm().addEnemyIfNotExists(e);
			}

			for (final NetPortal portal : loadPacket.getPortals()) {
				final Portal p = IOService.mapModel(portal, Portal.class);
				cli.getRealm().addPortalIfNotExists(p);
			}
		} catch (Exception e) {
			ClientGameLogic.log.error("[CLIENT] Failed to handle Load Packet. Reason: {}", e);
		}
	}

	public static void handleUnloadClient(RealmManagerClient cli, Packet packet) {
		final UnloadPacket unloadPacket = (UnloadPacket) packet;
		try {
			for (final Long p : unloadPacket.getPlayers()) {
				if (p == cli.getCurrentPlayerId()) {
					continue;
				}
				final Player removed = cli.getRealm().getPlayers().remove(p);
				if (removed == null) {
					ClientGameLogic.log.error("[CLIENT] Player {} does not exist", p);
				}
			}
			for (final Long lc : unloadPacket.getContainers()) {
				final LootContainer removed = cli.getRealm().getLoot().remove(lc);
				if (removed == null) {
					ClientGameLogic.log.error("[CLIENT] LootContainer {} does not exist", lc);
				}
			}
			for (final Long b : unloadPacket.getBullets()) {
				final Bullet removed = cli.getRealm().getBullets().remove(b);
				if (removed == null) {
					ClientGameLogic.log.error("[CLIENT] Bullet {} does not exist", b);
				}
			}
			for (final Long e : unloadPacket.getEnemies()) {
				final Enemy removed = cli.getRealm().getEnemies().remove(e);
				if (removed == null) {
					ClientGameLogic.log.error("[CLIENT] Enemy {} does not exist", e);
				}
			}
			for (final Long p : unloadPacket.getPortals()) {
				final Portal removed = cli.getRealm().getPortals().remove(p);
				if (removed == null) {
					ClientGameLogic.log.error("[CLIENT] Portal {} does not exist", p);
				}
			}

		} catch (Exception e) {
			ClientGameLogic.log.error("[CLIENT] Failed to handle Unload Packet. Reason: {}", e);
		}
	}

	public static void handleTextClient(RealmManagerClient cli, Packet packet) {
		final TextPacket textPacket = (TextPacket) packet;
		ClientGameLogic.log.info("[CLIENT] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
				textPacket.getFrom(), textPacket.getMessage());
		try {
			cli.getState().getPui().enqueueChat(textPacket.clone());
		} catch (Exception e) {
			ClientGameLogic.log.error("[CLIENT] Failed to handle text packet. Reason: {}", e.getMessage());
		}
	}
	
	// Client command codes for readability
	private static final byte LOGIN_RESPONSE_MSG_CODE = 2;
	private static final byte SERVER_ERROR_MSG_CODE = 4;
	private static final byte PLAYER_ACCOUNT_MSG_CODE = 5;
	// Switched command packet message type handler
	public static void handleCommandClient(RealmManagerClient cli, Packet packet) {
		final CommandPacket commandPacket = (CommandPacket) packet;
		ClientGameLogic.log.info("[CLIENT] Recieved Command Packet for Player {} Command={}",
				commandPacket.getPlayerId(), commandPacket.getCommand());
		try {
			switch (commandPacket.getCommandId()) {
			case LOGIN_RESPONSE_MSG_CODE:
				final LoginResponseMessage loginResponse = CommandType.fromPacket(commandPacket);
				ClientGameLogic.doLoginResponse(cli, loginResponse);
				break;
			case SERVER_ERROR_MSG_CODE:
				final ServerErrorMessage serverError = CommandType.fromPacket(commandPacket);
				ClientGameLogic.handleServerError(cli, serverError);
				break;
			case PLAYER_ACCOUNT_MSG_CODE:
				final PlayerAccountMessage playerAccount = CommandType.fromPacket(commandPacket);
				cli.getState().setAccount(playerAccount.getAccount());
				break;
			}
		} catch (Exception e) {
			ClientGameLogic.log.error("[CLIENT] Failed to handle client command packet. Reason: {}", e.getMessage());
		}
	}

	public static void handleObjectMoveClient(RealmManagerClient cli, Packet packet) {
		final ObjectMovePacket objectMovePacket = (ObjectMovePacket) packet;
		for (ObjectMovement movement : objectMovePacket.getMovements()) {
			final EntityType type = movement.getTargetEntityType();
			if (type == null) {
				continue;
			}
			switch (type) {
			case PLAYER:
				final Player playerToUpdate = cli.getRealm().getPlayer(movement.getEntityId());
				if (playerToUpdate == null) {
					break;
				}
				if (cli.getCurrentPlayerId() == movement.getEntityId()) {
					playerToUpdate.applyMovementLerp(movement, 1.0f);
				} else {
					playerToUpdate.applyMovementLerp(movement, 0.55f);
				}
				break;
			case ENEMY:
				final Enemy enemyToUpdate = cli.getRealm().getEnemy(movement.getEntityId());
				if (enemyToUpdate == null) {
					break;
				}
				enemyToUpdate.applyMovementLerp(movement);
				break;
			case BULLET:
				final Bullet bulletToUpdate = cli.getRealm().getBullet(movement.getEntityId());
				if (bulletToUpdate == null) {
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
		final UpdatePacket updatePacket = (UpdatePacket) packet;
		final Player toUpdate = cli.getRealm().getPlayer((updatePacket.getPlayerId()));
		if (toUpdate != null) {
			toUpdate.applyUpdate(updatePacket, cli.getState());

		} else {
			final Enemy enemyToUpdate = cli.getRealm().getEnemy((updatePacket.getPlayerId()));
			enemyToUpdate.applyUpdate(updatePacket, cli.getState());
			log.debug("[CLIENT] Recieved update for enemy {}", updatePacket);
		}
	}

	private static void handleServerError(RealmManagerClient cli, ServerErrorMessage message) {
		ClientGameLogic.log.error("[CLIENT] Recieved Server Error ***{}", message);
		cli.getState().getPui().enqueueChat(TextPacket.create("SYSTEM", "", message.toString()));
	}

	private static void doLoginResponse(RealmManagerClient cli, LoginResponseMessage loginResponse) {
		try {
			if (loginResponse.isSuccess()) {
				final CharacterClass cls = CharacterClass.valueOf(loginResponse.getClassId());
				final Player player = new Player(loginResponse.getPlayerId(),
						new Vector2f(loginResponse.getSpawnX(), loginResponse.getSpawnY()), GlobalConstants.PLAYER_SIZE,
						cls);
				ClientGameLogic.log.info("[CLIENT] Login succesful, added Player ID {}", player.getId());
				player.setSpriteSheet(GameSpriteManager.loadClassSprites(cls));
				ClientGameLogic.DATA_SERVICE.setSessionToken(loginResponse.getToken());
				cli.getState().setAccount(loginResponse.getAccount());
				cli.getState().loadClass(player, cls, true);
				cli.setCurrentPlayerId(player.getId());
				cli.getState().setPlayerId(player.getId());
				cli.startHeartbeatThread();
				final TextPacket packet = TextPacket.create("SYSTEM", "Player",
						"Welcome to JRealm " + GameLauncher.GAME_VERSION + "!");
				cli.getState().getPui().enqueueChat(packet);
			}
		} catch (Exception e) {
			ClientGameLogic.log.error("[CLIENT] Failed to respond to login response. Reason: {}", e.getMessage());
		}
	}
}
