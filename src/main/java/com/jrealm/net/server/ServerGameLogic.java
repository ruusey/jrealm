package com.jrealm.net.server;

import java.io.DataOutputStream;
import java.io.OutputStream;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.LootTier;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.messaging.CommandType;
import com.jrealm.game.messaging.LoginRequestMessage;
import com.jrealm.game.messaging.LoginResponseMessage;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.game.state.PlayState;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.Cardinality;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.MoveItemPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.net.server.packet.UseAbilityPacket;
import com.jrealm.net.server.packet.UsePortalPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerGameLogic {
	public static void handleUsePortalServer(RealmManagerServer mgr, Packet packet) {
		final UsePortalPacket usePortalPacket = (UsePortalPacket) packet;
		final Realm currentRealm = mgr.getRealms().get(usePortalPacket.getFromRealmId());
		final Realm targetRealm = mgr.getRealms().get(usePortalPacket.getPortalId());
		final Player user = currentRealm.getPlayers().remove(usePortalPacket.getPlayerId());
		final Portal used = currentRealm.getPortals().get(usePortalPacket.getPortalId());

		mgr.clearPlayerState(user.getId());

		// Send the player to the vault
		if ((targetRealm == null) && (usePortalPacket.getPortalId() == -1l)) {
			// Generate the vault dynamically
			final MapModel mapModel = GameDataManager.MAPS.get(1);
			final Realm generatedRealm = new Realm(true, 1);
			final Vector2f chestLoc = new Vector2f((0 + (1920 / 2)) - 450, (0 + (1080 / 2)) - 300);
			final Portal exitPortal = new Portal(Realm.RANDOM.nextLong(), (short) 1,
					chestLoc);

			generatedRealm.setupChests();
			user.setPos(mapModel.getCenter());
			exitPortal.setId(currentRealm.getRealmId());
			generatedRealm.addPortal(exitPortal);
			generatedRealm.addPlayer(user);
			mgr.addRealm(generatedRealm);
		}

		// Generate target, remove player from current, add to target.
		else if (targetRealm == null) {
			final PortalModel portalUsed = GameDataManager.PORTALS.get((int) used.getPortalId());
			final Realm generatedRealm = new Realm(true, portalUsed.getMapId());
			final Portal exitPortal = new Portal(Realm.RANDOM.nextLong(), (short) 1,
					generatedRealm.getTileManager().getSafePosition());
			user.setPos(generatedRealm.getTileManager().getSafePosition());
			exitPortal.setId(currentRealm.getRealmId());
			generatedRealm.addPortal(exitPortal);
			generatedRealm.addPlayer(user);
			mgr.addRealm(generatedRealm);
			// mgr.spawnTestPlayers(generatedRealm.getRealmId(), 2);
		}
		// Remove player from current, add to target ( realm already exists)
		else {
			targetRealm.addPlayer(user);
			user.setPos(targetRealm.getTileManager().getSafePosition());

			// If we are coming from the vault, save the data and destroy the realm
			// instance.
			if (currentRealm.getMapId() == 1) {
				mgr.getRealms().remove(currentRealm.getRealmId());
			}
		}
	}

	public static void handleHeartbeatServer(RealmManagerServer mgr, Packet packet) {
		//		HeartbeatPacket heartbeatPacket = (HeartbeatPacket) packet;
		//		log.info("[SERVER] Recieved Heartbeat Packet For Player {}@{}", heartbeatPacket.getPlayerId(),
		//				heartbeatPacket.getTimestamp());
	}

	public static void handlePlayerMoveServer(RealmManagerServer mgr, Packet packet) {
		final PlayerMovePacket playerMovePacket = (PlayerMovePacket) packet;
		final Realm realm = mgr.searchRealmsForPlayers(playerMovePacket.getEntityId());
		if (realm == null) {
			ServerGameLogic.log.error("Failed to get realm for player {}", playerMovePacket.getEntityId());
			return;
		}
		final Player toMove = realm
				.getPlayer(playerMovePacket.getEntityId());
		boolean doMove = playerMovePacket.isMove();
		final float spd = (float) ((5.6 * (toMove.getComputedStats().getSpd() + 53.5)) / 75.0f);
		if (playerMovePacket.getDirection().equals(Cardinality.NORTH)) {
			toMove.setUp(doMove);
			toMove.setDy(doMove ? -spd : 0.0f);
		}
		if (playerMovePacket.getDirection().equals(Cardinality.SOUTH)) {
			toMove.setDown(doMove);
			toMove.setDy(doMove ? spd : 0.0f);
		}
		if (playerMovePacket.getDirection().equals(Cardinality.EAST)) {
			toMove.setRight(doMove);
			toMove.setDx(doMove ? spd : 0.0f);
		}
		if (playerMovePacket.getDirection().equals(Cardinality.WEST)) {
			toMove.setLeft(doMove);
			toMove.setDx(doMove ? -spd : 0.0f);
		}

		//		if (toMove.getIsRight() && toMove.getIsUp()) {
		//			float rootTwoDx = (float) (toMove.getDx() / Math.sqrt(2));
		//			float rootTwoDy = (float) (toMove.getDy() / Math.sqrt(2));
		//
		//			toMove.setDx(rootTwoDx);
		//			toMove.setDy(rootTwoDy);
		//
		//		}
		if (playerMovePacket.getDirection().equals(Cardinality.NONE)) {
			toMove.setLeft(false);
			toMove.setRight(false);
			toMove.setDown(false);
			toMove.setUp(false);

			toMove.setDx(0);
			toMove.setDy(0);
		}
		//		log.info("[SERVER] Recieved PlayerMove Packet For Player {}", heartbeatPacket.getEntityId());
	}

	public static void handleUseAbilityServer(RealmManagerServer mgr, Packet packet) {
		final UseAbilityPacket useAbilityPacket = (UseAbilityPacket) packet;
		final Realm realm = mgr.searchRealmsForPlayers(useAbilityPacket.getPlayerId());
		mgr.useAbility(realm.getRealmId(), useAbilityPacket.getPlayerId(),
				new Vector2f(useAbilityPacket.getPosX(), useAbilityPacket.getPosY()));
		ServerGameLogic.log.info("[SERVER] Recieved UseAbility Packet For Player {}", useAbilityPacket.getPlayerId());
	}

	public static void handlePlayerShootServer(RealmManagerServer mgr, Packet packet) {
		final PlayerShootPacket shootPacket = (PlayerShootPacket) packet;
		final Realm realm = mgr.searchRealmsForPlayers(shootPacket.getEntityId());
		if (realm == null) {
			ServerGameLogic.log.error("Failed to get realm for player {}", shootPacket.getEntityId());
			return;
		}
		final Player player = realm.getPlayer(shootPacket.getEntityId());
		final Vector2f dest = new Vector2f(shootPacket.getDestX(), shootPacket.getDestY());
		dest.addX(player.getCam().getPos().x);
		dest.addY(player.getCam().getPos().y);
		final Vector2f source = player.getCenteredPosition();
		final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(player.getWeaponId());
		float angle = Bullet.getAngle(source, dest);
		for (Projectile proj : group.getProjectiles()) {
			short offset = (short) (player.getSize() / (short) 2);
			short rolledDamage = player.getInventory()[0].getDamage().getInRange();
			rolledDamage += player.getComputedStats().getAtt();
			mgr.addProjectile(realm.getRealmId(), shootPacket.getProjectileId(), player.getId(), proj.getProjectileId(),
					player.getWeaponId(), source.clone(-offset, -offset), angle + Float.parseFloat(proj.getAngle()),
					proj.getSize(), proj.getMagnitude(), proj.getRange(), rolledDamage, false, proj.getFlags(),
					proj.getAmplitude(), proj.getFrequency());
		}
	}

	public static void handleTextServer(RealmManagerServer mgr, Packet packet) {
		final TextPacket textPacket = (TextPacket) packet;
		try {
			ServerGameLogic.log.info("[SERVER] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}\nSrcIp: {}", textPacket.getTo(),
					textPacket.getFrom(), textPacket.getMessage(), textPacket.getSrcIp());

			TextPacket toBroadcast = TextPacket.create(textPacket.getFrom(), textPacket.getTo(), textPacket.getMessage());
			mgr.enqueueServerPacket(toBroadcast);
			ServerGameLogic.log.info("[SERVER] Broadcasted player chat message from {}", textPacket.getSrcIp());
		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to send welcome message. Reason: {}", e);
		}
	}

	public static void handleCommandServer(RealmManagerServer mgr, Packet packet) {
		final CommandPacket commandPacket = (CommandPacket) packet;
		ServerGameLogic.log.info("[SERVER] Recieved Command Packet For Player {}. Command={}. SrcIp={}", commandPacket.getPlayerId(),
				commandPacket.getCommand(), commandPacket.getSrcIp());
		try {
			switch (commandPacket.getCommandId()) {
			case 1:
				ServerGameLogic.doLogin(mgr, CommandType.fromPacket(commandPacket), commandPacket);
				break;
			}
		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to perform Server command for Player {}. Reason: {}", commandPacket.getPlayerId(),
					e.getMessage());
		}
	}
	// TODO: Isolate Move Item logic into its own helper class
	// I like spaghetti, what about you?
	public static void handleMoveItemServer(RealmManagerServer mgr, Packet packet) {
		final MoveItemPacket moveItemPacket = (MoveItemPacket) packet;
		ServerGameLogic.log.info("[SERVER] Recieved MoveItem Packet from player {}", moveItemPacket.getPlayerId());

		try {
			final Realm realm = mgr.searchRealmsForPlayers(moveItemPacket.getPlayerId());

			final Player player = realm.getPlayer(moveItemPacket.getPlayerId());
			// if moving item from inventory
			final GameItem currentEquip = moveItemPacket.getTargetSlotIndex() == -1 ? null
					: player.getInventory()[moveItemPacket.getTargetSlotIndex()];
			GameItem from = null;
			if(MoveItemPacket.isInv1(moveItemPacket.getFromSlotIndex())) {
				from = player.getInventory()[moveItemPacket.getFromSlotIndex()];
			}else if(MoveItemPacket.isGroundLoot(moveItemPacket.getFromSlotIndex())) {
				LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(), 32);
				from = nearLoot.getItems()[moveItemPacket.getFromSlotIndex()-20];
			}

			if(moveItemPacket.isDrop() && (from!=null)) {
				final LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(), 32);
				if(nearLoot==null) {
					realm.addLootContainer(new LootContainer(LootTier.BROWN, player.getPos().clone(), from.clone()));
					player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
				}else if(nearLoot.getFirstNullIdx()>-1){
					nearLoot.setItem(nearLoot.getFirstNullIdx(), from.clone());
					player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
				}
			}else if((from!=null) && from.isConsumable()&& !MoveItemPacket.isGroundLoot(moveItemPacket.getFromSlotIndex())) {
				final Stats newStats = player.getStats().concat(from.getStats());
				player.setStats(newStats);

				if (from.getStats().getHp() > 0) {
					player.drinkHp();
				} else if (from.getStats().getMp() > 0) {
					player.drinkMp();
				}
				player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
			}else if(MoveItemPacket.isInv1(moveItemPacket.getFromSlotIndex()) && MoveItemPacket.isEquipment(moveItemPacket.getTargetSlotIndex()) && (from!=null)) {

				player.getInventory()[moveItemPacket.getFromSlotIndex()] = currentEquip.clone();
				player.getInventory()[moveItemPacket.getTargetSlotIndex()] = from.clone();

			}else if(MoveItemPacket.isInv1(moveItemPacket.getFromSlotIndex()) && (moveItemPacket.getTargetSlotIndex()==-1)) {
				if(from==null) return;
				if(from.isConsumable() && moveItemPacket.isConsume()) {
					Stats newStats = player.getStats().concat(from.getStats());
					player.setStats(newStats);

					if (from.getStats().getHp() > 0) {
						player.drinkHp();
					} else if (from.getStats().getMp() > 0) {
						player.drinkMp();
					}
					player.getInventory()[moveItemPacket.getFromSlotIndex()] = null;
				}else if(MoveItemPacket.isInv1(moveItemPacket.getTargetSlotIndex())){
					GameItem to = player.getInventory()[moveItemPacket.getTargetSlotIndex()];
					if(to==null) {
						player.getInventory()[moveItemPacket.getTargetSlotIndex()] = from;
					}else {
						GameItem fromClone = from.clone();
						player.getInventory()[moveItemPacket.getFromSlotIndex()] = to;
						player.getInventory()[moveItemPacket.getTargetSlotIndex()] = fromClone;
					}
				}
			}else if(MoveItemPacket.isGroundLoot(moveItemPacket.getFromSlotIndex()) && MoveItemPacket.isInv1(moveItemPacket.getTargetSlotIndex())) {

				final LootContainer nearLoot = mgr.getClosestLootContainer(realm.getRealmId(), player.getPos(),
						player.getSize() / 2);
				if (nearLoot == null)
					return;
				final GameItem lootItem = nearLoot.getItems()[moveItemPacket.getFromSlotIndex() - 20];
				final GameItem currentInvItem = player.getInventory()[moveItemPacket.getTargetSlotIndex()];

				if((lootItem!=null) && (currentInvItem == null)) {
					player.getInventory()[player.firstEmptyInvSlot()] = lootItem.clone();
					nearLoot.setItem(moveItemPacket.getFromSlotIndex()-20, null);
					nearLoot.setItemsUncondensed(LootContainer.getCondensedItems(nearLoot));
				}else if((lootItem != null) & (currentInvItem !=null)) {
					GameItem lootClone = lootItem.clone();
					//GameItem currentInvItemClone = currentInvItem.clone();
					player.getInventory()[player.firstEmptyInvSlot()] = lootClone;
					//nearLoot.setItem(moveItemPacket.getFromSlotIndex()-20, currentInvItemClone);
					nearLoot.setItemsUncondensed(LootContainer.getCondensedItems(nearLoot));
				}
			}

		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to handle MoveItem packet from Player {}. Reason: {}", moveItemPacket.getPlayerId(),
					e);
		}
	}

	public static void handleLoadMapServer(RealmManagerServer mgr, Packet packet) {
		final LoadMapPacket loadMapPacket = (LoadMapPacket) packet;
		//		try {
		//			Player player = mgr.getRealm().getPlayer(loadMapPacket.getPlayerId());
		//			mgr.getRealm().loadMap(1, player);
		//
		//		} catch (Exception e) {
		//			ServerGameLogic.log.error("Failed to  Load Map packet from Player {}. Reason: {}", loadMapPacket.getPlayerId(),
		//					e.getMessage());
		//		}
		//		ServerGameLogic.log.info("[SERVER] Recieved Load Map packet from Player {}. Map={}", loadMapPacket.getPlayerId(),
		//				loadMapPacket.getMapKey());
	}

	private static void doLogin(RealmManagerServer mgr, LoginRequestMessage request, CommandPacket command) {
		try {
			final Camera c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
			final CharacterClass cls = CharacterClass.valueOf(request.getClassId());

			final Vector2f playerPos = new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
					(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE);
			final Player player = new Player(Realm.RANDOM.nextLong(), c, GameDataManager.loadClassSprites(cls),
					playerPos,
					GlobalConstants.PLAYER_SIZE, cls);
			player.equipSlots(PlayState.getStartingEquipment(cls));
			player.setName(request.getUsername());
			player.setHeadless(false);
			for (final Realm test : mgr.getRealms().values()) {
				if (test != null) {
					player.setPos(test.getTileManager().getSafePosition());
					test.addPlayer(player);
					mgr.getServer().getClients().get(command.getSrcIp()).setHandshakeComplete(true);
					break;
				}
			}

			final OutputStream toClientStream = mgr.getServer().getClients().get(command.getSrcIp()).getClientSocket()
					.getOutputStream();
			final DataOutputStream dosToClient = new DataOutputStream(toClientStream);
			final LoginResponseMessage message = LoginResponseMessage.builder().classId(request.getClassId())
					.spawnX(player.getPos().x)
					.spawnY(player.getPos().y)
					.playerId(player.getId()).success(true)
					.build();
			mgr.getRemoteAddresses().put(command.getSrcIp(), player.getId());

			final CommandPacket commandResponse = CommandPacket.create(player, CommandType.LOGIN_RESPONSE, message);
			commandResponse.serializeWrite(dosToClient);


		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to perform Client Login. Reason: {}", e);
		}
	}
}
