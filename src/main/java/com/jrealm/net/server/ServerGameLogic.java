package com.jrealm.net.server;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jrealm.account.dto.CharacterDto;
import com.jrealm.account.dto.ChestDto;
import com.jrealm.account.dto.GameItemRefDto;
import com.jrealm.account.dto.LoginRequestDto;
import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.account.dto.SessionTokenDto;
import com.jrealm.account.service.JrealmServerDataService;
import com.jrealm.game.JRealmGame;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.ProjectileEffectType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.DungeonGenerationParams;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.model.PortalModel;
import com.jrealm.util.GameObjectUtils;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.net.Packet;
import com.jrealm.net.messaging.CommandType;
import com.jrealm.net.messaging.LoginRequestMessage;
import com.jrealm.net.messaging.LoginResponseMessage;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.DeathAckPacket;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.MoveItemPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.net.server.packet.UseAbilityPacket;
import com.jrealm.net.server.packet.UsePortalPacket;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.entity.NetTile;
import com.jrealm.util.Cardinality;
import com.jrealm.util.PacketHandlerServer;
import com.jrealm.util.WorkerThread;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerGameLogic {
	/**
	 * As of release 0.3.0 DATA_SERVICE static class variable is required for Game
	 * functionality
	 */
	public static JrealmServerDataService DATA_SERVICE = null;

	private static void sendImmediateLoadMap(RealmManagerServer mgr, Realm realm, Player player) {
		try {
			final NetTile[] tiles = realm.getTileManager().getLoadMapTiles(player);
			final LoadMapPacket loadMap = LoadMapPacket.from(realm.getRealmId(),
					(short) realm.getMapId(), realm.getTileManager().getMapWidth(),
					realm.getTileManager().getMapHeight(), tiles);
			mgr.getPlayerLoadMapState().put(player.getId(), loadMap);
			mgr.enqueueServerPacket(player, loadMap);
		} catch (Exception e) {
			log.error("[SERVER] Failed to send immediate LoadMap on realm transition. Reason: {}", e.getMessage());
		}
	}

	public static void onPlayerJoin(RealmManagerServer mgr, Realm realm, Player player) {
		// Show dungeon graph node name if available, otherwise fallback to realm ID
		String zoneName;
		if (realm.getNodeId() != null && GameDataManager.DUNGEON_GRAPH != null) {
			com.jrealm.game.model.DungeonGraphNode node = GameDataManager.DUNGEON_GRAPH.get(realm.getNodeId());
			zoneName = (node != null) ? node.getDisplayName() : realm.getNodeId();
		} else {
			zoneName = "Realm " + realm.getRealmId();
		}
		TextPacket text = TextPacket.create("SYSTEM", player.getName(), zoneName);
		mgr.enqueueServerPacket(player, text);
		text = TextPacket.create("SYSTEM", player.getName(), "Difficulty: " + realm.getDifficultyMultiplier());
		mgr.enqueueServerPacket(player, text);
		text = TextPacket.create("SYSTEM", player.getName(), "Enemies: " + realm.getEnemies().size());
		mgr.enqueueServerPacket(player, text);
	}

	public static void handleUsePortalServer(RealmManagerServer mgr, Packet packet) {
		final UsePortalPacket usePortalPacket = (UsePortalPacket) packet;
		if (!validateCallingPlayer(mgr, packet, usePortalPacket.getPlayerId())) {
			return;
		}
		mgr.acquireRealmLock();
		
		if (usePortalPacket.isToVault()) {
			final Realm currentRealm = mgr.getRealms().get(usePortalPacket.getFromRealmId());
			if (currentRealm.getMapId() == 1)
				return;

			final Player user = currentRealm.getPlayers().remove(usePortalPacket.getPlayerId());

			final MapModel mapModel = GameDataManager.MAPS.get(1);
			final Realm generatedRealm = new Realm(true, 1, -1);
			final Vector2f chestLoc = new Vector2f((0 + (1920 / 2)) - 450, (0 + (1080 / 2)) - 300);
			final Portal exitPortal = new Portal(Realm.RANDOM.nextLong(), (short) 3, chestLoc);
			exitPortal.setNeverExpires();
			exitPortal.linkPortal(generatedRealm, currentRealm);
			generatedRealm.setupChests(user);
			user.setPos(mapModel.getCenter());
			generatedRealm.addPortal(exitPortal);
			mgr.addRealm(generatedRealm);
			generatedRealm.addPlayer(user);
			mgr.clearPlayerState(user.getId());
			sendImmediateLoadMap(mgr, generatedRealm, user);
			onPlayerJoin(mgr, generatedRealm, user);
			mgr.releaseRealmLock();
			return;
		}

		final Realm currentRealm = mgr.getRealms().get(usePortalPacket.getFromRealmId());
		final Player user = currentRealm.getPlayers().remove(usePortalPacket.getPlayerId());
		final Portal used = currentRealm.getPortals().get(usePortalPacket.getPortalId());
		Realm targetRealm = mgr.getRealms().get(used.getToRealmId());
		final PortalModel portalUsed = GameDataManager.PORTALS.get((int) used.getPortalId());
		currentRealm.removePlayer(user);

		// Resolve target node from dungeon graph
		final String targetNodeId = used.getTargetNodeId();
		final com.jrealm.game.model.DungeonGraphNode targetNode = (targetNodeId != null)
				? GameDataManager.DUNGEON_GRAPH.get(targetNodeId) : null;

		if (targetRealm == null) {
			// Try graph-based lookup first, then fallback to legacy depth
			Optional<Realm> existingRealm;
			if (targetNodeId != null) {
				existingRealm = mgr.findRealmForNode(targetNodeId);
			} else {
				existingRealm = mgr.findRealmAtDepth(portalUsed.getTargetRealmDepth());
			}

			if (existingRealm.isEmpty()) {
				// Create new realm from graph node or legacy portal model
				final int mapId = (targetNode != null) ? targetNode.getMapId() : portalUsed.getMapId();
				final int depth = (targetNode != null) ? targetNode.getDifficulty() : portalUsed.getTargetRealmDepth();
				final Realm generatedRealm = new Realm(true, mapId, depth, targetNodeId);

				targetRealm = generatedRealm;

				final boolean isBossNode = (targetNode != null && targetNode.isBossNode());

				// Boss node or Boss_0 map: skip random enemies, spawn player at ring center
				if (isBossNode || generatedRealm.getMapId() == 5) {
					user.setPos(new Vector2f(GlobalConstants.BASE_TILE_SIZE * 16,
							GlobalConstants.BASE_TILE_SIZE * 12));
				} else {
					generatedRealm.spawnRandomEnemies(generatedRealm.getMapId());
					user.setPos(generatedRealm.getTileManager().getSafePosition());
				}

				// Place exit portal and boss enemy if applicable
				final Vector2f bossSpawnPos = generatedRealm.getTileManager().getBossSpawnPos();
				final MapModel mapModel = GameDataManager.MAPS.get(mapId);
				if (bossSpawnPos != null && mapModel.getDungeonParams() != null) {
					final DungeonGenerationParams dungeonParams = mapModel.getDungeonParams();
					final int bossEnemyId = dungeonParams.getBossEnemyId();
					if (bossEnemyId > 0) {
						final Enemy boss = GameObjectUtils.getEnemyFromId(bossEnemyId, bossSpawnPos.clone());
						final int bossMult = (targetNode != null) ? targetNode.getDifficulty() : 4;
						boss.setHealth(boss.getHealth() * bossMult);
						boss.getStats().setHp((short) (boss.getStats().getHp() * bossMult));
						generatedRealm.addEnemy(boss);
					}

					final Portal exitPortal = new Portal(Realm.RANDOM.nextLong(), (short) 3,
							bossSpawnPos.clone(250, 0));
					exitPortal.linkPortal(generatedRealm, currentRealm);
					exitPortal.setNeverExpires();
					generatedRealm.addPortal(exitPortal);
				} else {
					final Portal exitPortal = new Portal(Realm.RANDOM.nextLong(), (short) 3,
							generatedRealm.getTileManager().getSafePosition());
					exitPortal.linkPortal(generatedRealm, currentRealm);
					exitPortal.setNeverExpires();
					generatedRealm.addPortal(exitPortal);
				}

				if (targetNode != null) {
					ServerGameLogic.log.info("[SERVER] Created realm for graph node: {} ({})",
							targetNode.getNodeId(), targetNode.getDisplayName());
				}
				mgr.addRealm(generatedRealm);
			} else {
				targetRealm = existingRealm.get();
			}
		} else {
			// Target realm already exists
			user.setPos(targetRealm.getTileManager().getSafePosition());

			// Vault cleanup
			if (currentRealm.getMapId() == 1) {
				List<ChestDto> chestsToSave = currentRealm.serializeChests();
				try {
					final PlayerAccountDto savedAccount = ServerGameLogic.DATA_SERVICE.executePost(
							"/data/account/" + user.getAccountUuid() + "/chest", chestsToSave, PlayerAccountDto.class);
					ServerGameLogic.log.info("Succesfully saved chests for account {}", savedAccount.getAccountUuid());
				} catch (Exception e) {
					ServerGameLogic.log.error("Failed to save account chests for account {}. Reason: {}",
							user.getAccountUuid(), e);
				}
				mgr.getRealms().remove(currentRealm.getRealmId());
			}
			// Boss realm cleanup when empty
			else if (currentRealm.getPlayers().size() == 0) {
				final com.jrealm.game.model.DungeonGraphNode currentNode = (currentRealm.getNodeId() != null)
						? GameDataManager.DUNGEON_GRAPH.get(currentRealm.getNodeId()) : null;
				boolean isBoss = (currentNode != null && currentNode.isBossNode())
						|| (currentRealm.getMapId() == 5);
				if (isBoss && currentRealm.getEnemies().size() == 0) {
					mgr.getRealms().remove(currentRealm.getRealmId());
				}
			}
		}
		targetRealm.addPlayer(user);
		mgr.clearPlayerState(user.getId());
		sendImmediateLoadMap(mgr, targetRealm, user);
		onPlayerJoin(mgr, targetRealm, user);
		mgr.releaseRealmLock();
	}

	public static void handleHeartbeatServer(RealmManagerServer mgr, Packet packet) {
		final HeartbeatPacket heartbeatPacket = (HeartbeatPacket) packet;
		final Player player = mgr.getPlayerByRemoteAddress(packet.getSrcIp());
		if(player==null) {
			log.error("Failed to process heartbeat packet. Player does not exist");
			return;
		}
		mgr.getPlayerLastHeartbeatTime().put(player.getId(), heartbeatPacket.getTimestamp());
	}

	public static void handlePlayerMoveServer(RealmManagerServer mgr, Packet packet) {
		final PlayerMovePacket playerMovePacket = (PlayerMovePacket) packet;
		if (!validateCallingPlayer(mgr, packet, playerMovePacket.getEntityId())) {
			return;
		}
		final Realm realm = mgr.findPlayerRealm(playerMovePacket.getEntityId());
		if (realm == null) {
			ServerGameLogic.log.error("Failed to get realm for player {}", playerMovePacket.getEntityId());
			return;
		}
		final Player toMove = realm.getPlayer(playerMovePacket.getEntityId());
		if (toMove.hasEffect(ProjectileEffectType.PARALYZED))
			return;
		boolean doMove = playerMovePacket.isMove();
		float spd = (float) ((5.6 * (toMove.getComputedStats().getSpd() + 53.5)) / 75.0f);
		spd = spd / 1.5f;
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
		
		if (toMove.getIsUp() && toMove.getIsRight()) {
			spd = (float) ((spd * Math.sqrt(2)) / 2.0f);
			toMove.setDy(doMove ? -spd : 0.0f);
			toMove.setDx(doMove ? spd : 0.0f);
		}

		if (toMove.getIsUp() && toMove.getIsLeft()) {
			spd = (float) ((spd * Math.sqrt(2)) / 2.0f);
			toMove.setDy(doMove ? -spd : 0.0f);
			toMove.setDx(doMove ? -spd : 0.0f);
		}

		if (toMove.getIsDown() && toMove.getIsRight()) {
			spd = (float) ((spd * Math.sqrt(2)) / 2.0f);
			toMove.setDy(doMove ? spd : 0.0f);
			toMove.setDx(doMove ? spd : 0.0f);
		}

		if (toMove.getIsDown() && toMove.getIsLeft()) {
			spd = (float) ((spd * Math.sqrt(2)) / 2.0f);
			toMove.setDy(doMove ? spd : 0.0f);
			toMove.setDx(doMove ? -spd : 0.0f);
		}
		if (playerMovePacket.getDirection().equals(Cardinality.NONE)) {
			toMove.setLeft(false);
			toMove.setRight(false);
			toMove.setDown(false);
			toMove.setUp(false);
			toMove.setDx(0);
			toMove.setDy(0);
		}
	}

	public static void handleUseAbilityServer(RealmManagerServer mgr, Packet packet) {
		final UseAbilityPacket useAbilityPacket = (UseAbilityPacket) packet;
		if (!validateCallingPlayer(mgr, packet, useAbilityPacket.getPlayerId())) {
			return;
		}
		final Realm realm = mgr.findPlayerRealm(useAbilityPacket.getPlayerId());
		mgr.useAbility(realm.getRealmId(), useAbilityPacket.getPlayerId(),
				new Vector2f(useAbilityPacket.getPosX(), useAbilityPacket.getPosY()));
		ServerGameLogic.log.info("[SERVER] Recieved UseAbility Packet For Player {}", useAbilityPacket.getPlayerId());
	}

	@PacketHandlerServer(TextPacket.class)
	public static void handleText0(RealmManagerServer mgr, Packet packet) {
		final TextPacket textPacket = (TextPacket) packet;
		final long fromPlayerId = mgr.getRemoteAddresses().get(textPacket.getSrcIp());
		if (!validateCallingPlayer(mgr, packet, fromPlayerId)) {
			return;
		}
		final Player player = mgr.searchRealmsForPlayer(fromPlayerId);
		final Realm realm = mgr.findPlayerRealm(fromPlayerId);

		log.info("Player {} says {} from Realm {}", player.getName(), textPacket.getMessage(), realm.getRealmId());
	}

	public static void handlePlayerShootServer(RealmManagerServer mgr, Packet packet) {
		final PlayerShootPacket shootPacket = (PlayerShootPacket) packet;
		if (!validateCallingPlayer(mgr, packet, shootPacket.getEntityId())) {
			return;
		}

		final Realm realm = mgr.findPlayerRealm(shootPacket.getEntityId());
		if (realm == null) {
			ServerGameLogic.log.error("Failed to get realm for player {}", shootPacket.getEntityId());
			return;
		}
		final Player player = realm.getPlayer(shootPacket.getEntityId());
		boolean canShoot = false;
		if (realm.getPlayerLastShotTime().get(player.getId()) != null) {
			double dex = (int) ((6.5 * (player.getComputedStats().getDex() + 17.3)) / 75);
			if (player.hasEffect(ProjectileEffectType.SPEEDY)) {
				dex = dex * 1.5;
			}else if(player.hasEffect(ProjectileEffectType.DAZED)) {
				dex = 1.0;
			}
			canShoot = ((Instant.now().toEpochMilli() - realm.getPlayerLastShotTime().get(player.getId())) > (1000
					/ dex));
			if (canShoot && !player.hasEffect(ProjectileEffectType.STUNNED)) {
				realm.getPlayerLastShotTime().put(player.getId(), Instant.now().toEpochMilli());
			} else {
				canShoot = false;
			}
		} else {
			realm.getPlayerLastShotTime().put(player.getId(), Instant.now().toEpochMilli());
			canShoot = true;
		}
		if (canShoot) {
			final Vector2f dest = new Vector2f(shootPacket.getDestX(), shootPacket.getDestY());
			final Vector2f source = player.getCenteredPosition();
			final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(player.getWeaponId());
			float angle = Bullet.getAngle(source, dest);
			for (Projectile proj : group.getProjectiles()) {
				short offset = (short) (player.getSize() / (short) 2);
				short rolledDamage = player.getInventory()[0].getDamage().getInRange();
				float shootAngle = angle + Float.parseFloat(proj.getAngle());
				rolledDamage += player.getComputedStats().getAtt();
				mgr.addProjectile(realm.getRealmId(), Realm.RANDOM.nextLong(), player.getId(), proj.getProjectileId(),
						player.getWeaponId(), source.clone(-offset, -offset), shootAngle, proj.getSize(),
						proj.getMagnitude(), proj.getRange(), rolledDamage, false, proj.getFlags(), proj.getAmplitude(),
						proj.getFrequency(), player.getId());
			}
		}
	}

	public static void handleTextServer(RealmManagerServer mgr, Packet packet) {
		final TextPacket textPacket = (TextPacket) packet;
		final long fromPlayerId = mgr.getRemoteAddresses().get(textPacket.getSrcIp());
		if (!validateCallingPlayer(mgr, packet, fromPlayerId)) {
			return;
		}
		final Player fromPlayer = mgr.searchRealmsForPlayer(fromPlayerId);
		final Realm from = mgr.findPlayerRealm(fromPlayerId);
		try {
//            ServerGameLogic.log.info("[SERVER] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}\nSrcIp: {}",
//                    textPacket.getTo(), textPacket.getFrom(), textPacket.getMessage(), textPacket.getSrcIp());

			TextPacket toBroadcast = TextPacket.create(from.getPlayer(fromPlayerId).getName(), textPacket.getTo(),
					textPacket.getMessage());
			mgr.enqueueServerPacket(toBroadcast);
			ServerGameLogic.log.info("[SERVER] Broadcasted player chat message from {}", fromPlayer.getName());
		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to send welcome message. Reason: {}", e);
		}
	}
	
	private static final byte LOGIN_REQUEST_MSG_CODE = 1;
	private static final byte SERVER_COMMAND_MSG_CODE = 3;
	
	public static void handleCommandServer(RealmManagerServer mgr, Packet packet) {
		final CommandPacket commandPacket = (CommandPacket) packet;
		if (commandPacket.getCommandId() != 1 && !validateCallingPlayer(mgr, packet, commandPacket.getPlayerId())) {
			return;
		}
		ServerGameLogic.log.info("[SERVER] Recieved Command Packet For Player {}. Command={}. SrcIp={}",
				commandPacket.getPlayerId(), commandPacket.getCommand(), commandPacket.getSrcIp());
		try {
			switch (commandPacket.getCommandId()) {
			case LOGIN_REQUEST_MSG_CODE:
				ServerGameLogic.doLogin(mgr, CommandType.fromPacket(commandPacket), commandPacket);
				break;
			case SERVER_COMMAND_MSG_CODE:
				ServerCommandHandler.invokeCommand(mgr, commandPacket);
				break;
			}
		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to perform Server command for Player {}. Reason: {}",
					commandPacket.getPlayerId(), e.getMessage());
		}
	}

	public static void handleMoveItemServer(RealmManagerServer mgr, Packet packet) {
		final MoveItemPacket moveItemPacket = (MoveItemPacket) packet;
		if (!validateCallingPlayer(mgr, packet, moveItemPacket.getPlayerId())) {
			return;
		}
		try {
			ServerItemHelper.handleMoveItemPacket(mgr, packet);
		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to handle MoveItem packet. Reason: {}", e);
		}
	}
	
	@PacketHandlerServer(DeathAckPacket.class)
	public static void handleDeathAckServer(RealmManagerServer mgr, Packet packet) {
		final DeathAckPacket deathPacket = (DeathAckPacket) packet;
		final Player real = mgr.getPlayerByRemoteAddress(packet.getSrcIp());
		if (!validateCallingPlayer(mgr, packet, deathPacket.getPlayerId())) {
			log.error("**DEATH ACK PLAYER ID DID NOT MATCH, REAL={}, attempted={}, BAN THEM**", real, deathPacket.getPlayerId());
			return;
		}
		final Player player = mgr.getPlayerById(deathPacket.getPlayerId());

		log.info("Handling death ack for player {}", player.getName());
		//final Realm playerRealm = mgr.findPlayerRealm(deathPacket.getPlayerId());
		mgr.disconnectPlayer(player);

	}

//	public static void handleLoadMapServer(RealmManagerServer mgr, Packet packet) {
//		@SuppressWarnings("unused")
//		final LoadMapPacket loadMapPacket = (LoadMapPacket) packet;
//	}

	private static void doLogin(RealmManagerServer mgr, LoginRequestMessage request, CommandPacket command) {
		CommandPacket commandResponse = null;
		long assignedId = -1l;
		PlayerAccountDto account = null;
		log.info("[SERVER] Recieved login command {}", request);
		Player player = null;
		try {
			SessionTokenDto loginToken = null;
			String accountName = request.getEmail();
			String accountUuid = null;
			Optional<CharacterDto> characterClass = null;
			try {
				loginToken = ServerGameLogic.doLoginRemote(request.getEmail(), request.getPassword());
				account = ServerGameLogic.DATA_SERVICE.executeGet("/data/account/" + loginToken.getAccountGuid(), null,
						PlayerAccountDto.class);
				accountName = account.getAccountName();
				accountUuid = account.getAccountUuid();
				characterClass = account.getCharacters().stream()
						.filter(character -> character.getCharacterUuid().equals(request.getCharacterUuid())).findAny();
				if (characterClass.isEmpty()) {
					throw new Exception("Player character with UUID " + request.getCharacterUuid() + " does not exist");
				}
			} catch (Exception e) {
				ServerGameLogic.log.error("Failed to perform remote login. Reason: {}", e);
				throw e;
			}
			final CharacterDto targetCharacter = characterClass.get();
			// Re-login check: if this account name already has an active player, remove it
			// Skip for bot accounts to avoid accidentally removing real players
			final boolean isBotAccount = request.getEmail() != null && request.getEmail().endsWith("@jrealm-bot.local");
			if (!isBotAccount) {
				final Player existing = mgr.searchRealmsForPlayer(account.getAccountName());
				if (existing != null) {
					player = existing;
					final Realm currentRealm = mgr.findPlayerRealm(existing.getId());
					currentRealm.removePlayer(existing);
					if (currentRealm.getMapId() == 1) {
						mgr.safeRemoveRealm(currentRealm.getRealmId());
					}
					log.info("Player {} re-logged in with new Character ID {}", accountName,
							targetCharacter.getCharacterUuid());
				}
			}
			// TODO: Character death currently disabled
//            if (targetCharacter.isDeleted())
//                throw new Exception("Character " + targetCharacter.getCharacterUuid() + " is dead!");
			final Map<Integer, GameItem> loadedEquipment = new HashMap<>();
			for (final GameItemRefDto item : targetCharacter.getItems()) {
				loadedEquipment.put(item.getSlotIdx(), GameItem.fromGameItemRef(item));
			}

			assignedId = Realm.RANDOM.nextLong();
			// Resolve character class - prefer characterClass field, fall back to stats.classId
			Integer resolvedClassId = targetCharacter.getCharacterClass();
			if ((resolvedClassId == null || resolvedClassId == 0) && targetCharacter.getStats() != null
					&& targetCharacter.getStats().getClassId() != null && targetCharacter.getStats().getClassId() > 0) {
				resolvedClassId = targetCharacter.getStats().getClassId();
			}
			log.info("[SERVER] Resolved classId={} for character {} (dto.characterClass={}, stats.classId={})",
					resolvedClassId, targetCharacter.getCharacterUuid(),
					targetCharacter.getCharacterClass(),
					targetCharacter.getStats() != null ? targetCharacter.getStats().getClassId() : "null");
			final CharacterClass cls = CharacterClass.valueOf(resolvedClassId != null ? resolvedClassId : 0);
			final Vector2f playerPos = new Vector2f((0 + (JRealmGame.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
					(0 + (JRealmGame.height / 2)) - GlobalConstants.PLAYER_SIZE);
			player = new Player(assignedId, playerPos, GlobalConstants.PLAYER_SIZE, cls);
			final Realm targetRealm = mgr.getTopRealm();
			final ClientSession userSession = mgr.getServer().getClients().get(command.getSrcIp());
			player.setAccountUuid(accountUuid);
			player.setCharacterUuid(targetCharacter.getCharacterUuid());
			player.equipSlots(loadedEquipment);
			player.applyStats(targetCharacter.getStats());
			player.setName(accountName);
			player.setHeadless(false);
			// Mark bot accounts so they skip persistence and get cleaned up on death
			if (request.getEmail() != null && request.getEmail().endsWith("@jrealm-bot.local")) {
				player.setBot(true);
			}
			player.addEffect(ProjectileEffectType.INVINCIBLE, 3000);
			player.setPos(targetRealm.getTileManager().getSafePosition());
			log.info("[SERVER] Adding player {} to realm. bot={}, headless={}, accountUuid={}",
					player.getName(), player.isBot(), player.isHeadless(), player.getAccountUuid());
			targetRealm.addPlayer(player);
			// Begin processing.
			userSession.setHandshakeComplete(true);

			final LoginResponseMessage message = LoginResponseMessage.builder()
					.classId(targetCharacter.getCharacterClass()).spawnX(player.getPos().x).spawnY(player.getPos().y)
					.playerId(player.getId()).success(true).account(account).token(loginToken.getToken()).build();
			mgr.getRemoteAddresses().put(command.getSrcIp(), player.getId());

			commandResponse = CommandPacket.create(player, CommandType.LOGIN_RESPONSE, message);
			final Player toWelcome = player;
			Runnable welcomePlayer = () ->{
				ServerGameLogic.onPlayerJoin(mgr, targetRealm, toWelcome);
			};
			WorkerThread.runLater(welcomePlayer, 2000);
			log.info("[SERVER] Player {} logged in successfully", player);
		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to perform Client Login. Reason: {}", e);
			commandResponse = CommandPacket.createError(assignedId, 503,
					"Failed to perform Client Login. Reason: " + e.getMessage());
		} finally {
			// Enqueue the response packet
			mgr.enqueueServerPacket(player, commandResponse);
		}
	}

	private static SessionTokenDto doLoginRemote(final String userName, final String password) throws Exception {
		final LoginRequestDto loginReq = new LoginRequestDto(userName, password);
		final SessionTokenDto response = ServerGameLogic.DATA_SERVICE.executePost("/admin/account/login", loginReq,
				SessionTokenDto.class);
		return response;
	}

	// Looks up a player by source IP, which is determined by the server and less likely to be
	// vulnerable to spoofing assuming someone reverse engineers the packet protocol
	private static boolean validateCallingPlayer(RealmManagerServer mgr, Packet packet, Long declaredPlayerId) {
		final Long actualPlayerId = mgr.getRemoteAddresses().get(packet.getSrcIp());
		final Player actualPlayer = mgr.searchRealmsForPlayer(actualPlayerId);
		if (actualPlayer == null) {
			return false;
		}
		if (actualPlayer.getId() != declaredPlayerId) {
			log.info("Player ids do not match for Packet {}. Actual PlayerId: {}. Declared PlayerId: {}", packet,
					actualPlayerId, declaredPlayerId);
			// Disconnect the player
			mgr.disconnectPlayer(actualPlayer);
			return false;
		}
		return true;
	}
}
