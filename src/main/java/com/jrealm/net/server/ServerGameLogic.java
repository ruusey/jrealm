package com.jrealm.net.server;

import java.io.DataOutputStream;
import java.io.OutputStream;
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
import com.jrealm.account.service.JRealmDataService;
import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.GameItem;
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
import com.jrealm.game.util.Cardinality;
import com.jrealm.game.util.GameObjectUtils;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.PlayerMovePacket;
import com.jrealm.net.server.packet.PlayerShootPacket;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.net.server.packet.UseAbilityPacket;
import com.jrealm.net.server.packet.UsePortalPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerGameLogic {
	/**
	 * As of release 0.3.0 DATA_SERVICE static member is required for Game
	 * functionality
	 */
	public static JRealmDataService DATA_SERVICE = null;

	public static void handleUsePortalServer(RealmManagerServer mgr, Packet packet) {
		final UsePortalPacket usePortalPacket = (UsePortalPacket) packet;

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
			generatedRealm.addPlayer(user);
			mgr.addRealm(generatedRealm);
			currentRealm.removePlayer(user);
			return;
		}

		final Realm currentRealm = mgr.getRealms().get(usePortalPacket.getFromRealmId());
		final Player user = currentRealm.getPlayers().remove(usePortalPacket.getPlayerId());
		final Portal used = currentRealm.getPortals().get(usePortalPacket.getPortalId());
		final Realm targetRealm = mgr.getRealms().get(used.getToRealmId());
		final PortalModel portalUsed = GameDataManager.PORTALS.get((int) used.getPortalId());

		// Generate target, remove player from current, add to target.
		if (targetRealm == null) {
			// Try to find the realm at the correct depth
			Optional<Realm> realmAtDepth = mgr.findRealmAtDepth(portalUsed.getTargetRealmDepth());
			if (realmAtDepth.isEmpty()) {
				final Realm generatedRealm = new Realm(true, portalUsed.getMapId(), portalUsed.getTargetRealmDepth());
				if (portalUsed.getMapId() == 5) {
					final int healthMult = (4);
					final Vector2f spawnPos = new Vector2f(GlobalConstants.BASE_TILE_SIZE * 12,
							GlobalConstants.BASE_TILE_SIZE * 13);
					
					final Portal exitPortal = new Portal(Realm.RANDOM.nextLong(), (short) 3, spawnPos.clone(250, 0));
					exitPortal.linkPortal(generatedRealm, mgr.getTopRealm());
					exitPortal.setNeverExpires();

					user.setPos(spawnPos);
					
					final Enemy enemy = GameObjectUtils.getEnemyFromId(13, spawnPos);
					enemy.setHealth(enemy.getHealth() * healthMult);
					enemy.setPos(spawnPos.clone(200, 0));
					
					generatedRealm.addEnemy(enemy);
					generatedRealm.addPortal(exitPortal);
					generatedRealm.addPlayer(user);
				} else {
					final Portal exitPortal = new Portal(Realm.RANDOM.nextLong(), (short) 3,
							generatedRealm.getTileManager().getSafePosition());
					
					user.setPos(generatedRealm.getTileManager().getSafePosition());
					
					exitPortal.linkPortal(generatedRealm, currentRealm);
					exitPortal.setNeverExpires();
					
					generatedRealm.spawnRandomEnemies(generatedRealm.getMapId());
					generatedRealm.addPortal(exitPortal);
					generatedRealm.addPlayer(user);
				}
				mgr.addRealm(generatedRealm);
			} else {
				realmAtDepth.get().addPlayer(user);
			}
		}
		// Remove player from current, add to target ( realm already exists)
		else {
			targetRealm.addPlayer(user);
			user.setPos(targetRealm.getTileManager().getSafePosition());

			// If we are coming from the vault, save the data and destroy the realm
			// instance.
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
				mgr.safeRemoveRealm(currentRealm.getRealmId());
			} else if ((currentRealm.getMapId() == 5) && (currentRealm.getEnemies().size() == 0)
					&& (currentRealm.getPlayers().size() == 0)) {
				mgr.safeRemoveRealm(currentRealm.getRealmId());
			}
		}
		currentRealm.removePlayer(user);
		mgr.clearPlayerState(user.getId());
	}

	public static void handleHeartbeatServer(RealmManagerServer mgr, Packet packet) {
		// HeartbeatPacket heartbeatPacket = (HeartbeatPacket) packet;
		// log.info("[SERVER] Recieved Heartbeat Packet For Player {}@{}",
		// heartbeatPacket.getPlayerId(),
		// heartbeatPacket.getTimestamp());
	}

	public static void handlePlayerMoveServer(RealmManagerServer mgr, Packet packet) {
		final PlayerMovePacket playerMovePacket = (PlayerMovePacket) packet;
		final Realm realm = mgr.searchRealmsForPlayer(playerMovePacket.getEntityId());
		if (realm == null) {
			ServerGameLogic.log.error("Failed to get realm for player {}", playerMovePacket.getEntityId());
			return;
		}
		final Player toMove = realm.getPlayer(playerMovePacket.getEntityId());
		if (toMove.hasEffect(EffectType.PARALYZED))
			return;
		boolean doMove = playerMovePacket.isMove();
		float spd = (float) ((5.6 * (toMove.getComputedStats().getSpd() + 53.5)) / 75.0f);
		spd = spd/1.5f;
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
		final Realm realm = mgr.searchRealmsForPlayer(useAbilityPacket.getPlayerId());
		mgr.useAbility(realm.getRealmId(), useAbilityPacket.getPlayerId(),
				new Vector2f(useAbilityPacket.getPosX(), useAbilityPacket.getPosY()));
		ServerGameLogic.log.info("[SERVER] Recieved UseAbility Packet For Player {}", useAbilityPacket.getPlayerId());
	}

	public static void handlePlayerShootServer(RealmManagerServer mgr, Packet packet) {
		final PlayerShootPacket shootPacket = (PlayerShootPacket) packet;
		final Realm realm = mgr.searchRealmsForPlayer(shootPacket.getEntityId());
		if (realm == null) {
			ServerGameLogic.log.error("Failed to get realm for player {}", shootPacket.getEntityId());
			return;
		}
		final Player player = realm.getPlayer(shootPacket.getEntityId());
		boolean canShoot = false;
		if(realm.getPlayerLastShotTime().get(player.getId())!=null) {
			double dex = (int) ((6.5 * (player.getComputedStats().getDex() + 17.3)) / 75);
			if(player.hasEffect(EffectType.SPEEDY)) {
				dex = dex * 1.5;
			}
			canShoot = ((System.currentTimeMillis() - realm.getPlayerLastShotTime().get(player.getId())) > (1000 / dex));
			if(canShoot && !player.hasEffect(EffectType.STUNNED)) {
				realm.getPlayerLastShotTime().put(player.getId(), Instant.now().toEpochMilli());
			}else {
				canShoot=false;
			}
		}else {
			realm.getPlayerLastShotTime().put(player.getId(), Instant.now().toEpochMilli());
			canShoot=true;
		}
		if(canShoot) {
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
						proj.getFrequency());
			}
		}
	}

	public static void handleTextServer(RealmManagerServer mgr, Packet packet) {
		final TextPacket textPacket = (TextPacket) packet;
		final long fromPlayerId = mgr.getRemoteAddresses().get(textPacket.getSrcIp());
		final Realm from = mgr.searchRealmsForPlayer(fromPlayerId);
		try {
			ServerGameLogic.log.info("[SERVER] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}\nSrcIp: {}",
					textPacket.getTo(), textPacket.getFrom(), textPacket.getMessage(), textPacket.getSrcIp());

			TextPacket toBroadcast = TextPacket.create(from.getPlayer(fromPlayerId).getName(), textPacket.getTo(),
					textPacket.getMessage());
			mgr.enqueueServerPacket(toBroadcast);
			ServerGameLogic.log.info("[SERVER] Broadcasted player chat message from {}", textPacket.getSrcIp());
		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to send welcome message. Reason: {}", e);
		}
	}

	public static void handleCommandServer(RealmManagerServer mgr, Packet packet) {
		final CommandPacket commandPacket = (CommandPacket) packet;
		ServerGameLogic.log.info("[SERVER] Recieved Command Packet For Player {}. Command={}. SrcIp={}",
				commandPacket.getPlayerId(), commandPacket.getCommand(), commandPacket.getSrcIp());
		try {
			switch (commandPacket.getCommandId()) {
			case 1:
				ServerGameLogic.doLogin(mgr, CommandType.fromPacket(commandPacket), commandPacket);
				break;
			case 3:
				ServerGameLogic.handleServerCommand(mgr, commandPacket);
				break;
			}
		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to perform Server command for Player {}. Reason: {}",
					commandPacket.getPlayerId(), e.getMessage());
		}
	}

	public static void handleMoveItemServer(RealmManagerServer mgr, Packet packet) {
		try {
			ServerItemHelper.handleMoveItemPacket(mgr, packet);
		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to handle MoveItem packet. Reason: {}", e);
		}
	}

	public static void handleLoadMapServer(RealmManagerServer mgr, Packet packet) {
		@SuppressWarnings("unused")
		final LoadMapPacket loadMapPacket = (LoadMapPacket) packet;
	}

	private static void handleServerCommand(RealmManagerServer mgr,
			CommandPacket command) {
		try {
			ServerCommandHandler.invokeCommand(mgr, command);
		}catch(Exception e) {
			log.error("Failed to invoke server command. Reason: {}", e);
		}	
	}

	private static void doLogin(RealmManagerServer mgr, LoginRequestMessage request, CommandPacket command) {
		CommandPacket commandResponse = null;
		long assignedId = -1l;
		PlayerAccountDto account = null;
		try {
			SessionTokenDto loginToken = null;
			String accountName = request.getEmail();
			String accountUuid = null;
			Optional<CharacterDto> characterClass = null;
			try {
				loginToken = ServerGameLogic.doLoginRemote(request.getEmail(), request.getPassword());
				ServerGameLogic.DATA_SERVICE.setSessionToken(loginToken.getToken());
				account = ServerGameLogic.DATA_SERVICE
						.executeGet("/data/account/" + loginToken.getAccountGuid(), null, PlayerAccountDto.class);
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
			// TODO: Character death currently disabled
			if (targetCharacter.isDeleted() && false)
				throw new Exception("Character " + targetCharacter.getCharacterUuid() + " is deleted!");
			final Map<Integer, GameItem> loadedEquipment = new HashMap<>();
			for (final GameItemRefDto item : targetCharacter.getItems()) {
				loadedEquipment.put(item.getSlotIdx(), GameItem.fromGameItemRef(item));
			}

			assignedId = Realm.RANDOM.nextLong();
			final CharacterClass cls = CharacterClass.valueOf(targetCharacter.getCharacterClass());
			final Vector2f playerPos = new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
					(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE);
			final Player player = new Player(assignedId, playerPos, GlobalConstants.PLAYER_SIZE, cls);

			player.setAccountUuid(accountUuid);
			player.setCharacterUuid(targetCharacter.getCharacterUuid());
			player.equipSlots(loadedEquipment);
			player.applyStats(targetCharacter.getStats());
			player.setName(accountName);
			player.setHeadless(false);
			player.addEffect(EffectType.INVINCIBLE, 3000);
			Realm targetRealm = mgr.getTopRealm();
			player.setPos(targetRealm.getTileManager().getSafePosition());
			targetRealm.addPlayer(player);
			mgr.getServer().getClients().get(command.getSrcIp()).setHandshakeComplete(true);

			final LoginResponseMessage message = LoginResponseMessage.builder()
					.classId(targetCharacter.getCharacterClass()).spawnX(player.getPos().x).spawnY(player.getPos().y)
					.playerId(player.getId()).success(true).account(account)
					.token(loginToken.getToken()).build();
			mgr.getRemoteAddresses().put(command.getSrcIp(), player.getId());

			commandResponse = CommandPacket.create(player, CommandType.LOGIN_RESPONSE, message);
		} catch (Exception e) {
			ServerGameLogic.log.error("Failed to perform Client Login. Reason: {}", e);
			commandResponse = CommandPacket.createError(assignedId, 503,
					"Failed to perform Client Login. Reason: " + e.getMessage());
		} finally {
			OutputStream toClientStream;
			try {
				toClientStream = mgr.getServer().getClients().get(command.getSrcIp()).getClientSocket()
						.getOutputStream();
				final DataOutputStream dosToClient = new DataOutputStream(toClientStream);
				commandResponse.serializeWrite(dosToClient);
			} catch (Exception e) {
				log.error("Failed to write login response to client. Reason: {}", e);
			}
		}
	}

	private static SessionTokenDto doLoginRemote(final String userName, final String password) throws Exception {
		final LoginRequestDto loginReq = new LoginRequestDto(userName, password);
		final SessionTokenDto response = ServerGameLogic.DATA_SERVICE.executePost("/admin/account/login", loginReq,
				SessionTokenDto.class);
		return response;
	}
}
