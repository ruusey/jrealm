package com.jrealm.net.server;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jrealm.account.dto.AccountDto;
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
import com.jrealm.game.model.DungeonGraphNode;
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
import com.jrealm.net.server.packet.LoginAckPacket;
import com.jrealm.net.server.packet.MoveItemPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.net.server.packet.UseAbilityPacket;
import com.jrealm.net.server.packet.UsePortalPacket;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.core.IOService;
import com.jrealm.net.entity.NetBullet;
import com.jrealm.net.entity.NetEnemy;
import com.jrealm.net.entity.NetLootContainer;
import com.jrealm.net.entity.NetPlayer;
import com.jrealm.net.entity.NetPortal;
import com.jrealm.net.entity.NetTile;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.GameObject;
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

	public static void sendImmediateLoadMap(RealmManagerServer mgr, Realm realm, Player player) {
		try {
			final NetTile[] tiles = realm.getTileManager().getLoadMapTiles(player);
			final LoadMapPacket loadMap = LoadMapPacket.from(realm.getRealmId(),
					(short) realm.getMapId(), realm.getTileManager().getMapWidth(),
					realm.getTileManager().getMapHeight(), tiles);
			mgr.getPlayerLoadMapState().put(player.getId(), loadMap);
			mgr.enqueueServerPacket(player, loadMap);

			// Send player's position so the client knows where they are in the new realm
			final ObjectMovePacket posPacket = ObjectMovePacket.from(new GameObject[]{ player });
			mgr.enqueueServerPacket(player, posPacket);
		} catch (Exception e) {
			log.error("[SERVER] Failed to send immediate LoadMap on realm transition. Reason: {}", e.getMessage());
		}
	}

	public static void onPlayerJoin(RealmManagerServer mgr, Realm realm, Player player) {
		// Show dungeon graph node name if available, otherwise fallback to realm ID
		String zoneName;
		if (realm.getNodeId() != null && GameDataManager.DUNGEON_GRAPH != null) {
			DungeonGraphNode node = GameDataManager.DUNGEON_GRAPH.get(realm.getNodeId());
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

		// Overseer welcome message
		if (realm.getOverseer() != null) {
			realm.getOverseer().welcomePlayer(player);
		}

		// Show online player list when joining the overworld (beach entry node)
		if (realm.getNodeId() != null && realm.getNodeId().equals("beach")) {
			final StringBuilder sb = new StringBuilder("Players online: ");
			int count = 0;
			for (Player p : mgr.getPlayers()) {
				if (p.getId() == player.getId()) continue;
				if (count > 0) sb.append(", ");
				sb.append(p.getName());
				count++;
			}
			if (count == 0) {
				sb.append("(none)");
			}
			mgr.enqueueServerPacket(player, TextPacket.create("SYSTEM", player.getName(), sb.toString()));
		}
	}

	public static void handleUsePortalServer(RealmManagerServer mgr, Packet packet) {
		final UsePortalPacket usePortalPacket = (UsePortalPacket) packet;
		if (!validateCallingPlayer(mgr, packet, usePortalPacket.getPlayerId())) {
			return;
		}
		mgr.acquireRealmLock();
		
		if (usePortalPacket.isToVault()) {
			final Realm currentRealm = mgr.getRealms().get(usePortalPacket.getFromRealmId());
			if (currentRealm == null || currentRealm.getMapId() == 1) {
				mgr.releaseRealmLock();
				return;
			}

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
		if (currentRealm == null) { mgr.releaseRealmLock(); return; }
		final Player user = currentRealm.getPlayers().remove(usePortalPacket.getPlayerId());
		if (user == null) { mgr.releaseRealmLock(); return; }
		final Portal used = currentRealm.getPortals().get(usePortalPacket.getPortalId());
		if (used == null) { mgr.releaseRealmLock(); return; }
		Realm targetRealm = mgr.getRealms().get(used.getToRealmId());
		final PortalModel portalUsed = GameDataManager.PORTALS.get((int) used.getPortalId());
		currentRealm.removePlayer(user);

		// Save + remove vault realm if leaving a vault (regardless of target)
		if (currentRealm.getMapId() == 1) {
			try {
				List<ChestDto> chestsToSave = currentRealm.serializeChests();
				ServerGameLogic.DATA_SERVICE.executePost(
						"/data/account/" + user.getAccountUuid() + "/chest", chestsToSave, PlayerAccountDto.class);
				log.info("[SERVER] Saved vault chests for {} on portal exit", user.getName());
			} catch (Exception e) {
				log.error("[SERVER] Failed to save vault chests on portal exit: {}", e.getMessage());
			}
			currentRealm.setShutdown(true);
			mgr.getRealms().remove(currentRealm.getRealmId());
		}

		// Resolve target node from dungeon graph
		final String targetNodeId = used.getTargetNodeId();
		final DungeonGraphNode targetNode = (targetNodeId != null)
				? GameDataManager.DUNGEON_GRAPH.get(targetNodeId) : null;

		if (targetRealm == null) {
			// Each portal creates its own dungeon instance (1:1 portal-to-dungeon).
			// The portal's toRealmId is set after creation so subsequent uses of the
			// SAME portal route to the SAME instance.
			{
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
					// Spawn player at the first room (far from boss room)
					final Vector2f spawnPos = generatedRealm.getTileManager().getPlayerSpawnPos();
					if (spawnPos != null) {
						user.setPos(spawnPos);
					} else {
						user.setPos(generatedRealm.getTileManager().getSafePosition());
					}
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
				// Link this portal to the new dungeon so subsequent uses route here
				used.setToRealmId(generatedRealm.getRealmId());
			}
		} else {
			// Target realm already exists — spawn at the dungeon's fixed entry point
			final Vector2f entryPos = targetRealm.getTileManager().getPlayerSpawnPos();
			if (entryPos != null) {
				// Spawn at dungeon entry room with slight random offset to avoid stacking
				user.setPos(entryPos.clone(
						Realm.RANDOM.nextInt(32) - 16,
						Realm.RANDOM.nextInt(32) - 16));
			} else if (targetRealm.getTileManager().getTerrainParams() != null) {
				// Overworld: spawn in beach zone
				user.setPos(targetRealm.getTileManager().getSafePosition());
			} else {
				// Static map: use spawn points if defined, otherwise safe position
				user.setPos(targetRealm.getTileManager().getSafePosition());
			}

			// Dungeon cleanup: remove any dungeon when last player leaves via portal.
			// depth > 0 covers both dungeon-graph and legacy portal-created dungeons.
			if (currentRealm.getPlayers().size() == 0 && currentRealm.getDepth() > 0) {
				ServerGameLogic.log.info("[SERVER] Removing empty dungeon realm {} (mapId={}, node={})",
						currentRealm.getRealmId(), currentRealm.getMapId(), currentRealm.getNodeId());
				currentRealm.setShutdown(true);
				mgr.getRealms().remove(currentRealm.getRealmId());
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
			log.debug("Failed to process heartbeat packet. Player does not exist");
			return;
		}
		mgr.getPlayerLastHeartbeatTime().put(player.getId(), heartbeatPacket.getTimestamp());
	}

	@PacketHandlerServer(LoginAckPacket.class)
	public static void handleLoginAckServer(RealmManagerServer mgr, Packet packet) {
		final LoginAckPacket ackPacket = (LoginAckPacket) packet;
		final Player player = mgr.getPlayerByRemoteAddress(packet.getSrcIp());
		if (player == null) {
			log.debug("Failed to process LoginAck. Player does not exist for {}", packet.getSrcIp());
			return;
		}
		final Realm playerRealm = mgr.findPlayerRealm(player.getId());
		if (playerRealm != null) {
			sendImmediateLoadMap(mgr, playerRealm, player);
			log.info("[SERVER] Sent initial LoadMap to {} on LoginAck", player.getName());
		}
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
		// lastInputSeq is now incremented per-tick in movePlayer(), not per-packet
		if (toMove.hasEffect(ProjectileEffectType.PARALYZED))
			return;
		boolean doMove = playerMovePacket.isMove();

		// Update direction flags from packet
		if (playerMovePacket.getDirection().equals(Cardinality.NONE)) {
			toMove.setLeft(false);
			toMove.setRight(false);
			toMove.setDown(false);
			toMove.setUp(false);
		} else if (playerMovePacket.getDirection().equals(Cardinality.NORTH)) {
			toMove.setUp(doMove);
		} else if (playerMovePacket.getDirection().equals(Cardinality.SOUTH)) {
			toMove.setDown(doMove);
		} else if (playerMovePacket.getDirection().equals(Cardinality.EAST)) {
			toMove.setRight(doMove);
		} else if (playerMovePacket.getDirection().equals(Cardinality.WEST)) {
			toMove.setLeft(doMove);
		}

		// Recalculate velocity from current flags — always consistent,
		// no intermediate state where one axis has cardinal speed and
		// the other has diagonal speed.
		float tilesPerSec = 4.0f + 5.6f * (toMove.getComputedStats().getSpd() / 75.0f);
		if (toMove.hasEffect(ProjectileEffectType.SPEEDY)) tilesPerSec *= 1.5f;
		// DAZED only affects dex (attack speed), not movement speed
		float spd = tilesPerSec * 32.0f / 64.0f;

		boolean movingX = toMove.getIsLeft() || toMove.getIsRight();
		boolean movingY = toMove.getIsUp() || toMove.getIsDown();
		if (movingX && movingY) {
			spd = (float) ((spd * Math.sqrt(2)) / 2.0f);
		}

		toMove.setDx(toMove.getIsRight() ? spd : toMove.getIsLeft() ? -spd : 0.0f);
		toMove.setDy(toMove.getIsDown() ? spd : toMove.getIsUp() ? -spd : 0.0f);

		if (playerMovePacket.getDirection().equals(Cardinality.NONE)) {
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
		ServerGameLogic.log.debug("[SERVER] Recieved UseAbility Packet For Player {}", useAbilityPacket.getPlayerId());
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
			final int weaponPgId = player.getWeaponId();
			final ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(weaponPgId);
			if (group == null) {
				log.error("No projectile group {} for weapon. Loaded groups: {}",
					weaponPgId, GameDataManager.PROJECTILE_GROUPS.keySet().stream().sorted()
					.map(String::valueOf).reduce((a,b)->a+","+b).orElse("none"));
				return;
			}
			float angle = Bullet.getAngle(source, dest);
			for (Projectile proj : group.getProjectiles()) {
				short offset = (short) (player.getSize() / (short) 2);
				short rolledDamage = player.getInventory()[0].getDamage().getInRange();
				float shootAngle = angle + Float.parseFloat(proj.getAngle());
				rolledDamage += player.getComputedStats().getAtt();
				Bullet b = mgr.addProjectile(realm.getRealmId(), Realm.RANDOM.nextLong(), player.getId(), player.getWeaponId(),
						proj.getProjectileId(), source.clone(-offset, -offset), shootAngle, proj.getSize(),
						proj.getMagnitude(), proj.getRange(), rolledDamage, false, proj.getFlags(), proj.getAmplitude(),
						proj.getFrequency(), player.getId());
				if (b != null && proj.getEffects() != null) b.setEffects(proj.getEffects());
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

			String chatTo = fromPlayer.getChatRole() != null ? fromPlayer.getChatRole() : "";
			TextPacket toBroadcast = TextPacket.create(fromPlayer.getName(), chatTo, textPacket.getMessage());
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
			// Immediately send updated inventory AND container state so the client
			// sees the change without waiting for the next scheduled tick
			final Realm realm = mgr.findPlayerRealm(moveItemPacket.getPlayerId());
			if (realm != null) {
				final Player player = realm.getPlayer(moveItemPacket.getPlayerId());
				if (player != null) {
					// Send inventory update
					final UpdatePacket update = realm.getPlayerAsPacket(player.getId());
					if (update != null) {
						mgr.enqueueServerPacket(player, update);
					}
					// Send container update immediately to all nearby players.
					// Without this, container changes wait for the next LoadPacket tick
					// (up to 62ms at 16Hz), causing visible desync.
					final LootContainer nearLoot = mgr.getClosestLootContainer(
						realm.getRealmId(), player.getPos(), 32);
					if (nearLoot != null && nearLoot.getContentsChanged()) {
						try {
							final NetLootContainer netContainer = IOService.mapModel(nearLoot, NetLootContainer.class);
							final LoadPacket containerUpdate = new LoadPacket(
								new NetPlayer[0], new NetEnemy[0], new NetBullet[0],
								new NetLootContainer[] { netContainer },
								new NetPortal[0]);
							// Send to all players near the container, not just the mover
							for (final Map.Entry<Long, Player> p : realm.getPlayers().entrySet()) {
								if (p.getValue().isHeadless()) continue;
								float dx = p.getValue().getPos().x - nearLoot.getPos().x;
								float dy = p.getValue().getPos().y - nearLoot.getPos().y;
								if (dx * dx + dy * dy <= 640 * 640) {
									mgr.enqueueServerPacket(p.getValue(), containerUpdate);
								}
							}
						} catch (Exception ex) {
							ServerGameLogic.log.error("Failed to send immediate container update: {}", ex.getMessage());
						}
					}
				}
			}
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
		log.info("[SERVER] Recieved login command {}", request);
		// Run the slow HTTP authentication on a worker thread so the tick loop isn't blocked
		WorkerThread.doAsync(() -> {
			CommandPacket commandResponse = null;
			long assignedId = -1l;
			Player player = null;
			try {
				SessionTokenDto loginToken = ServerGameLogic.doLoginRemote(request.getEmail(), request.getPassword());
				PlayerAccountDto account = ServerGameLogic.DATA_SERVICE.executeGet(
						"/data/account/" + loginToken.getAccountGuid(), null, PlayerAccountDto.class);
				String accountName = account.getAccountName();
				String accountUuid = account.getAccountUuid();
				Optional<CharacterDto> characterClass = account.getCharacters().stream()
						.filter(character -> character.getCharacterUuid().equals(request.getCharacterUuid())).findAny();
				if (characterClass.isEmpty()) {
					throw new Exception("Player character with UUID " + request.getCharacterUuid() + " does not exist");
				}

				final CharacterDto targetCharacter = characterClass.get();
				// Force disconnect any existing session for this account before proceeding.
				// This handles ghost players stuck in-game after a dirty disconnect.
				final boolean isBotAccount = request.getEmail() != null && request.getEmail().endsWith("@jrealm-bot.local");
				if (!isBotAccount) {
					boolean disconnectedExisting = false;
					for (Player existing : mgr.getPlayers()) {
						if (existing.getAccountUuid() != null && existing.getAccountUuid().equals(accountUuid)) {
							log.info("[SERVER] Force-disconnecting previous session for account {} (re-login)", accountUuid);
							mgr.disconnectPlayer(existing);
							disconnectedExisting = true;
							break;
						}
					}
					if (disconnectedExisting) {
						// Brief pause to let realm state settle after force-disconnect
						try { Thread.sleep(250); } catch (InterruptedException ignored) {}
						// Safety net: if the player is STILL in a realm after disconnect
						// (e.g. disconnectPlayer partially failed), forcibly remove them
						for (Player ghost : mgr.getPlayers()) {
							if (ghost.getAccountUuid() != null && ghost.getAccountUuid().equals(accountUuid)) {
								log.warn("[SERVER] Ghost player {} still present after disconnect, forcibly removing", ghost.getName());
								final Realm ghostRealm = mgr.findPlayerRealm(ghost.getId());
								if (ghostRealm != null) {
									ghostRealm.getExpiredPlayers().add(ghost.getId());
									ghostRealm.removePlayer(ghost);
								}
								mgr.clearPlayerState(ghost.getId());
							}
						}
					}
				}

				final Map<Integer, GameItem> loadedEquipment = new HashMap<>();
				for (final GameItemRefDto item : targetCharacter.getItems()) {
					loadedEquipment.put(item.getSlotIdx(), GameItem.fromGameItemRef(item));
				}

				assignedId = Realm.RANDOM.nextLong();
				Integer resolvedClassId = targetCharacter.getCharacterClass();
				if ((resolvedClassId == null || resolvedClassId == 0) && targetCharacter.getStats() != null
						&& targetCharacter.getStats().getClassId() != null && targetCharacter.getStats().getClassId() > 0) {
					resolvedClassId = targetCharacter.getStats().getClassId();
				}
				final CharacterClass cls = CharacterClass.valueOf(resolvedClassId != null ? resolvedClassId : 0);
				final Vector2f playerPos = new Vector2f((0 + (JRealmGame.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
						(0 + (JRealmGame.height / 2)) - GlobalConstants.PLAYER_SIZE);
				player = new Player(assignedId, playerPos, GlobalConstants.PLAYER_SIZE, cls);
				final Realm targetRealm = mgr.getTopRealm();
				final ClientSession userSession = mgr.getServer().getClients().get(command.getSrcIp());
				if (userSession == null) {
					throw new Exception("Client session not found for " + command.getSrcIp());
				}
				player.setAccountUuid(accountUuid);
				player.setCharacterUuid(targetCharacter.getCharacterUuid());
				player.equipSlots(loadedEquipment);
				player.applyStats(targetCharacter.getStats());
				player.setName(accountName);
				player.setHeadless(false);
				// Cache chat role from auth provisions for name coloring
				try {
					AccountDto authAccount = ServerGameLogic.DATA_SERVICE.executeGet(
						"/admin/account/" + loginToken.getAccountGuid(), null, AccountDto.class);
					if (authAccount != null && authAccount.isSysAdmin()) player.setChatRole("sysadmin");
					else if (authAccount != null && authAccount.isAdmin()) player.setChatRole("admin");
					else if (authAccount != null && authAccount.isModerator()) player.setChatRole("mod");
				} catch (Exception roleEx) {
					log.warn("[SERVER] Could not fetch auth role for {}: {}", accountName, roleEx.getMessage());
				}
				if (isBotAccount) {
					player.setBot(true);
				}
				player.addEffect(ProjectileEffectType.INVINCIBLE, 3000);
				player.setPos(targetRealm.getTileManager().getSafePosition());
				log.info("[SERVER] Adding player {} to realm. bot={}, headless={}, accountUuid={}",
						player.getName(), player.isBot(), player.isHeadless(), player.getAccountUuid());
				targetRealm.addPlayer(player);
				userSession.setHandshakeComplete(true);

				final LoginResponseMessage message = LoginResponseMessage.builder()
						.classId(resolvedClassId != null ? resolvedClassId : 0)
						.spawnX(player.getPos().x).spawnY(player.getPos().y)
						.playerId(player.getId()).success(true).account(account).token(loginToken.getToken()).build();
				mgr.getRemoteAddresses().put(command.getSrcIp(), player.getId());

				commandResponse = CommandPacket.create(player, CommandType.LOGIN_RESPONSE, message);
				final Player toWelcome = player;
				final Realm welcomeRealm = targetRealm;
				// Tiles are sent when the client's first heartbeat arrives (proves client is ready)
				WorkerThread.runLater(() -> ServerGameLogic.onPlayerJoin(mgr, welcomeRealm, toWelcome), 2000);
				log.info("[SERVER] Player {} logged in successfully", player);
			} catch (Exception e) {
				ServerGameLogic.log.error("Failed to perform Client Login. Reason: {}", e);
				commandResponse = CommandPacket.createError(assignedId, 503,
						"Failed to perform Client Login. Reason: " + e.getMessage());
			} finally {
				if (player != null && commandResponse != null) {
					mgr.enqueueServerPacket(player, commandResponse);
				}
			}
		});
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
