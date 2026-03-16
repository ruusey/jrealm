package com.jrealm.net.server;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.jrealm.account.dto.AccountDto;
import com.jrealm.account.dto.CharacterDto;
import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.game.GameLauncher;
import com.jrealm.net.test.StressTestClient;
import com.jrealm.game.contants.ProjectileEffectType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.LootTier;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.model.CharacterClassModel;
import com.jrealm.game.model.MapModel;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.tile.Tile;
import com.jrealm.net.messaging.CommandType;
import com.jrealm.net.messaging.ServerCommandMessage;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.util.AdminRestrictedCommand;
import com.jrealm.util.CommandHandler;
import com.jrealm.util.GameObjectUtils;
import com.jrealm.util.WorkerThread;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerCommandHandler {
    public static final Map<String, MethodHandle> COMMAND_CALLBACKS = new HashMap<>();
    public static final Map<String, CommandHandler> COMMAND_DESCRIPTIONS = new HashMap<>();
    public static final Set<String> ADMIN_RESTRICTED_COMMANDS = new HashSet<>();
    public static final Set<Long> ADMIN_USER_CACHE = new HashSet<>();
    private static final List<StressTestClient> ACTIVE_BOTS = new ArrayList<>();
    private static final List<String> BOT_ACCOUNT_GUIDS = new ArrayList<>();
    
    // Handler methods are passed a reference to the current RealmManager, the
    // invoking Player object
    // and the ServerCommand message object.
    public static void invokeCommand(RealmManagerServer mgr, CommandPacket command) throws Exception {
        final ServerCommandMessage message = CommandType.fromPacket(command);
        final long fromPlayerId = mgr.getRemoteAddresses().get(command.getSrcIp());
        final Realm playerRealm = mgr.findPlayerRealm(fromPlayerId);
        final Player fromPlayer = playerRealm.getPlayer(fromPlayerId);
        // Look up this players account to see if they are allowed
        // to run Admin server commands
        try {
        	if(ADMIN_RESTRICTED_COMMANDS.contains(message.getCommand().toLowerCase()) && !ADMIN_USER_CACHE.contains(fromPlayer.getId())) {
        	  log.info("Player {} attempting to invvoke admin restricted command '{}'... validating authority", fromPlayer.getName(), message.getCommand());
              final AccountDto playerAccount = ServerGameLogic.DATA_SERVICE.executeGet("/admin/account/" + fromPlayer.getAccountUuid(), null, AccountDto.class);
        		// has Subscription 'ADMIN'
              if (!playerAccount.isAdmin()) {
            	  throw new IllegalStateException(
                          "Player " + playerAccount.getAccountName() + " is not allowed to use Admin commands.");
              }else {
            	  ADMIN_USER_CACHE.add(fromPlayer.getId());
            	  log.info("Player {} attempting to invvoke admin restricted command '{}'... validating authority", fromPlayer.getName(), message.getCommand());
              }
        	}
            
            final MethodHandle methodHandle = COMMAND_CALLBACKS.get(message.getCommand().toLowerCase());

            if (methodHandle == null) {
                final CommandPacket errorResponse = CommandPacket.createError(fromPlayer, 501,
                        "Unknown command " + message.getCommand());
                mgr.enqueueServerPacket(fromPlayer, errorResponse);
            } else {
                methodHandle.invokeExact(mgr, fromPlayer, message);
            }
        } catch (Throwable e) {
            log.error("Failed to handle server command. Reason: {}", e.getMessage());
            final CommandPacket errorResponse = CommandPacket.createError(fromPlayer, 502, e.getMessage());
            mgr.enqueueServerPacket(fromPlayer, errorResponse);
        }
    }
    
	@CommandHandler(value = "op", description = "Promote a user to administrator. Or demote them back to a regular user")
	@AdminRestrictedCommand
	public static void invokeOpUser(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
		if (message.getArgs() == null || message.getArgs().size() < 1)
			throw new IllegalArgumentException("Usage: /op {PLAYER_NAME}");
		log.info("**Player OP** Player {} is promoting/demoting {} to/from server operator", target.getAccountUuid(),
				message.getArgs().get(0));
		try {
			final AccountDto callerAccount = ServerGameLogic.DATA_SERVICE
					.executeGet("/admin/account/" + target.getAccountUuid(), null, AccountDto.class);
			if (!callerAccount.isAdmin()) {
				throw new IllegalArgumentException("You are required to be a server operator to invoke this command");
			}
			final Player toOp = mgr.findPlayerByName(message.getArgs().get(0));
			if (toOp == null) {
				throw new IllegalArgumentException("Player " + message.getArgs().get(0) + " does not exist.");
			} else if (toOp.getAccountUuid().equals(target.getAccountUuid())) {
				throw new IllegalArgumentException("You cannot OP yourself. Idiot.");
			}

			final AccountDto targetAccount = ServerGameLogic.DATA_SERVICE
					.executeGet("/admin/account/" + toOp.getAccountUuid(), null, AccountDto.class);
			boolean removed = false;
			if (targetAccount.isAdmin()) {
				targetAccount.removeAdminSubscription();
				ADMIN_USER_CACHE.remove(toOp.getId());
				removed = true;
			} else {
				targetAccount.addAdminSubscription();
			}
			ServerGameLogic.DATA_SERVICE.executePut("/admin/account/" + targetAccount.getAccountGuid(), null,
					AccountDto.class);
			final String operation = " is " + (removed ? "no longer " : "now ");
			final String msg = "Player " + message.getArgs().get(0) + operation + "a server operator";
			mgr.enqueueServerPacket(target, TextPacket.from("SYSTEM", target.getName(), msg));
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to op user. Reason: " + e.getMessage());
		}
	}

    @CommandHandler(value="stat", description="Modify or max individual Player stats")
	@AdminRestrictedCommand
    public static void invokeSetStats(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /stat {STAT_NAME} {STAT_VALUE}");
        final short valueToSet = message.getArgs().get(0).equalsIgnoreCase("max") ? -1
                : Short.parseShort(message.getArgs().get(1));
        CharacterClassModel classModel = GameDataManager.CHARACTER_CLASSES.get(target.getClassId());
        log.info("Player {} set stat {} to {}", target.getName(), message.getArgs().get(0), valueToSet);
        switch (message.getArgs().get(0)) {
        case "hp":
            target.getStats().setHp(valueToSet);
            break;
        case "mp":
            target.getStats().setMp(valueToSet);
            break;
        case "att":
            target.getStats().setAtt(valueToSet);
            break;
        case "def":
            target.getStats().setDef(valueToSet);
            break;
        case "spd":
            target.getStats().setSpd(valueToSet);
            break;
        case "dex":
            target.getStats().setDex(valueToSet);
            break;
        case "vit":
            target.getStats().setVit(valueToSet);
            break;
        case "wis":
            target.getStats().setWis(valueToSet);
            break;
        case "max":
            target.setStats(classModel.getMaxStats());
            break;
        default:
            throw new IllegalArgumentException("Unknown stat " + message.getArgs().get(0));
        }
    }

    @CommandHandler(value="testplayers", description="Spawns a variable number of headless test players at the user")
	@AdminRestrictedCommand
    public static void invokeSpawnTest(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() != 1)
            throw new IllegalArgumentException("Usage: /testplayers {COUNT}");
        final Realm playerRealm = mgr.findPlayerRealm(target.getId());
        log.info("Player {} spawn {} players  at {}", target.getName(), message.getArgs().get(0), target.getPos());
        mgr.spawnTestPlayers(playerRealm.getRealmId(), Integer.parseInt(message.getArgs().get(0)),
                target.getPos().clone());
    }
    
    @CommandHandler(value="about", description="Get server info")
    public static void invokeAbout(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        final List<String> text = Arrays.asList(
                "JRealm Server " + GameLauncher.GAME_VERSION,
                "Players connected: " + mgr.getRealms().values().stream().map(realm -> realm.getPlayers().size()).collect(Collectors.summingInt(count -> count)),
                "Players in my realm: " + mgr.findPlayerRealm(target.getId()).getPlayers().size());
        mgr.enqueChunkedText(target, text);
        log.info("Player {} request command about.", target.getName());
    }
    
    @CommandHandler(value="tile", description="Change all tiles in the viewport to the provided tile ID")
	@AdminRestrictedCommand
    public static void invokeSetTile(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        final Short newTileId = Short.parseShort(message.getArgs().get(0));
        final Vector2f playerPos = target.getPos();
        final Realm playerRealm = mgr.findPlayerRealm(target.getId());
        final Tile[] toModify = playerRealm.getTileManager().getBaseTiles(playerPos);
        for(Tile tile : toModify) {
            if(tile==null) continue;
            tile.setTileId(newTileId);
        }
        log.info("Player {} request command tile.", target.getName());
    }

    @CommandHandler(value="help", description="This command")
    public static void invokeHelp(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        String commandHelpText = "Available Commands:   ";
        TextPacket commandHelp = TextPacket.from("SYSTEM", target.getName(), commandHelpText);
        mgr.enqueueServerPacket(target, commandHelp);
        for (String commandHandlerKey : COMMAND_CALLBACKS.keySet()) {
            final CommandHandler handler = COMMAND_DESCRIPTIONS.get(commandHandlerKey);
            commandHelpText = "/" + commandHandlerKey + " - "+handler.description();
            commandHelp = TextPacket.from("SYSTEM", target.getName(), commandHelpText);
            mgr.enqueueServerPacket(target, commandHelp);
        }

        log.info("Player {} request command help.", commandHelp);
    }
    
    @CommandHandler(value="heal", description="Restores all Player health and mp")
	@AdminRestrictedCommand
    public static void invokePlayerHeal(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        target.setHealth(target.getComputedStats().getHp());
        target.setMana(target.getComputedStats().getMp());
        log.info("Player {} healed themselves", target.getName());
    }

    @CommandHandler(value="spawn", description="Spawn a given Enemy by it's id")
	@AdminRestrictedCommand
    public static void invokeEnemySpawn(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() != 1)
            throw new IllegalArgumentException("Usage: /spawn {ENEMY_ID}");

        log.info("Player {} spawn enemy {} at {}", target.getName(), message.getArgs().get(0), target.getPos());
        final Realm from = mgr.findPlayerRealm(target.getId());
        final int enemyId = Integer.parseInt(message.getArgs().get(0));
        from.addEnemy(GameObjectUtils.getEnemyFromId(enemyId, target.getPos().clone()));
    }

    @CommandHandler(value="seteffect", description="Add or remove Player stat effects")
	@AdminRestrictedCommand
    public static void invokeSetEffect(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /seteffect {add | clear} {EFFECT_ID} {DURATION (sec)}");
        log.info("Player {} set effect {}", target.getName(), message);
        switch (message.getArgs().get(0)) {
        case "add":
            target.addEffect(ProjectileEffectType.valueOf(Short.valueOf(message.getArgs().get(1))),
                    1000 * Long.parseLong(message.getArgs().get(2)));
            break;
        case "clear":
            target.resetEffects();
            break;
        }
    }

    @CommandHandler(value="tp", description="Teleport to a given Player name or X,Y coordinates")
    public static void invokeTeleport(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /tp {PLAYER_NAME}. /tp {X_CORD} {Y_CORD}");

        log.info("Player {} teleport {}", target.getName(), message);
        if (message.getArgs().size() == 2) {
            final float destX = Float.parseFloat(message.getArgs().get(0));
            final float destY = Float.parseFloat(message.getArgs().get(1));
            if (destX < GlobalConstants.PLAYER_SIZE || destY < GlobalConstants.PLAYER_SIZE) {
                throw new IllegalArgumentException("Invalid destination");
            }
            target.setPos(new Vector2f(destX, destY));
        } else {
            final Player destPlayer = mgr.searchRealmsForPlayer(message.getArgs().get(0));
            if (destPlayer == null) {
                throw new IllegalArgumentException("Player " + message.getArgs().get(0) + " does not exist.");
            } else {
                target.setPos(destPlayer.getPos().clone());
            }
        }
    }

    @CommandHandler(value="item", description="Spawn a given Item by its id")
	@AdminRestrictedCommand
    public static void invokeSpawnItem(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /item {ITEM_ID}");
        log.info("Player {} spawn item {}", target.getName(), message);
        final Realm targetRealm = mgr.findPlayerRealm(target.getId());
        final int gameItemId = Integer.parseInt(message.getArgs().get(0));
        final GameItem itemToSpawn = GameDataManager.GAME_ITEMS.get(gameItemId);
        if (itemToSpawn == null) {
            throw new IllegalArgumentException("Item with ID " + gameItemId + " does not exist.");
        }
        final LootContainer lootDrop = new LootContainer(LootTier.BROWN, target.getPos().clone(), itemToSpawn);
        targetRealm.addLootContainer(lootDrop);
    }

    @CommandHandler(value="portal", description="Spawn a portal to a map by name. Usage: /portal {MAP_NAME}")
	@AdminRestrictedCommand
    public static void invokeSpawnPortal(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /portal {MAP_NAME}");

        final String mapName = String.join(" ", message.getArgs());
        log.info("Player {} spawning portal to map '{}'", target.getName(), mapName);

        // Find map by name (case-insensitive, supports partial match)
        MapModel targetMap = null;
        for (MapModel m : GameDataManager.MAPS.values()) {
            if (m.getMapName().equalsIgnoreCase(mapName)) {
                targetMap = m;
                break;
            }
        }
        // Fallback: partial match
        if (targetMap == null) {
            for (MapModel m : GameDataManager.MAPS.values()) {
                if (m.getMapName().toLowerCase().contains(mapName.toLowerCase())) {
                    targetMap = m;
                    break;
                }
            }
        }
        if (targetMap == null) {
            // List available maps in error message
            final String available = GameDataManager.MAPS.values().stream()
                    .map(m -> m.getMapName() + " (" + m.getMapId() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Map '" + mapName + "' not found. Available: " + available);
        }

        // Find a portal model that targets this map, or fall back to a generic portal
        PortalModel portalModel = null;
        for (PortalModel pm : GameDataManager.PORTALS.values()) {
            if (pm.getMapId() == targetMap.getMapId()) {
                portalModel = pm;
                break;
            }
        }
        // Fall back to dungeon portal (portalId 6) if no matching portal model
        if (portalModel == null) {
            portalModel = GameDataManager.PORTALS.get(6);
        }

        // Create the destination realm
        final Realm currentRealm = mgr.findPlayerRealm(target.getId());
        final Realm destinationRealm = new Realm(true, targetMap.getMapId(), portalModel.getTargetRealmDepth());
        destinationRealm.spawnRandomEnemies(targetMap.getMapId());
        mgr.addRealm(destinationRealm);

        // Create and link portal at player position
        final com.jrealm.game.entity.Portal portal = new com.jrealm.game.entity.Portal(
                Realm.RANDOM.nextLong(), (short) portalModel.getPortalId(), target.getPos().clone());
        portal.linkPortal(currentRealm, destinationRealm);
        portal.setNeverExpires();
        currentRealm.addPortal(portal);

        final String msg = "Portal to " + targetMap.getMapName() + " spawned!";
        mgr.enqueueServerPacket(target, TextPacket.from("SYSTEM", target.getName(), msg));
        log.info("Player {} spawned portal to {} (mapId={})", target.getName(), targetMap.getMapName(), targetMap.getMapId());
    }

    @CommandHandler(value="godmode", description="Toggle invincibility")
	@AdminRestrictedCommand
    public static void invokeGodMode(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (target.hasEffect(ProjectileEffectType.INVINCIBLE)) {
            target.resetEffects();
            mgr.enqueueServerPacket(target, TextPacket.from("SYSTEM", target.getName(), "God mode OFF"));
        } else {
            target.addEffect(ProjectileEffectType.INVINCIBLE, 1000 * 60 * 60 * 24);
            mgr.enqueueServerPacket(target, TextPacket.from("SYSTEM", target.getName(), "God mode ON"));
        }
        log.info("Player {} toggled god mode", target.getName());
    }

    @CommandHandler(value="spawnbots", description="Spawn N bot players with real accounts. Usage: /spawnbots {COUNT}")
    @AdminRestrictedCommand
    public static void invokeSpawnBots(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /spawnbots {COUNT}");

        final int count = Integer.parseInt(message.getArgs().get(0));
        if (count < 1 || count > 50)
            throw new IllegalArgumentException("Count must be between 1 and 50");

        mgr.enqueueServerPacket(target, TextPacket.from("SYSTEM", target.getName(),
                "Spawning " + count + " bot players..."));

        final String serverHost = "127.0.0.1";
        final int serverPort = 2222;
        final float spawnX = target.getPos().x;
        final float spawnY = target.getPos().y;

        WorkerThread.doAsync(() -> {
            // Phase 1: Pre-create ALL accounts and characters (slow HTTP, no game impact)
            final List<String[]> botCredentials = new ArrayList<>(); // [email, password, characterUuid, accountGuid]
            log.info("[BOTS] Phase 1: Creating {} accounts...", count);
            for (int i = 0; i < count; i++) {
                try {
                    final String botId = "bot-" + System.currentTimeMillis() + "-" + i;
                    final String email = botId + "@jrealm-bot.local";
                    final String password = "botpass-" + UUID.randomUUID().toString();
                    final String botName = "Bot_" + i;

                    final AccountDto registerReq = AccountDto.builder()
                            .email(email).password(password).accountName(botName)
                            .accountProvisions(new ArrayList<>())
                            .accountSubscriptions(new ArrayList<>())
                            .build();
                    final JsonNode registered = ServerGameLogic.DATA_SERVICE.executePost(
                            "/admin/account/register", registerReq, JsonNode.class);
                    final String accountGuid = registered.get("accountGuid").asText();

                    final List<CharacterClass> classes = CharacterClass.getCharacterClasses();
                    final int classId = classes.get(Realm.RANDOM.nextInt(classes.size())).classId;
                    final PlayerAccountDto account = ServerGameLogic.DATA_SERVICE.executePost(
                            "/data/account/" + accountGuid + "/character?classId=" + classId,
                            null, PlayerAccountDto.class);

                    String characterUuid = null;
                    if (account.getCharacters() != null && !account.getCharacters().isEmpty()) {
                        characterUuid = account.getCharacters().get(0).getCharacterUuid();
                    }
                    if (characterUuid == null) {
                        log.error("[BOTS] Failed to get character UUID for {}", botName);
                        continue;
                    }
                    log.info("[BOTS] Pre-created {} (class={}, uuid={})", botName, classId, characterUuid);
                    botCredentials.add(new String[]{email, password, characterUuid, accountGuid});

                    synchronized (BOT_ACCOUNT_GUIDS) {
                        BOT_ACCOUNT_GUIDS.add(accountGuid);
                    }
                } catch (Exception e) {
                    log.error("[BOTS] Failed to create bot account {}: {}", i, e.getMessage());
                }
            }

            // Phase 2: Connect bots one at a time with stagger (fast, just TCP + login)
            log.info("[BOTS] Phase 2: Connecting {} bots...", botCredentials.size());
            int success = 0;
            for (int i = 0; i < botCredentials.size(); i++) {
                try {
                    final String[] creds = botCredentials.get(i);
                    final StressTestClient bot = new StressTestClient(i, serverHost, serverPort,
                            creds[0], creds[1], creds[2]);
                    bot.setSpawnNear(spawnX, spawnY);
                    synchronized (ACTIVE_BOTS) {
                        ACTIVE_BOTS.add(bot);
                    }
                    WorkerThread.submitAndForkRun(bot);
                    success++;

                    // Stagger connections so each bot fully logs in before the next connects
                    Thread.sleep(1500);
                } catch (Exception e) {
                    log.error("[BOTS] Failed to connect bot {}: {}", i, e.getMessage());
                }
            }
            log.info("[BOTS] Spawned {}/{} bot players", success, count);
            try {
                mgr.enqueueServerPacket(target, TextPacket.from("SYSTEM", target.getName(),
                        "Spawned " + success + "/" + count + " bot players"));
            } catch (Exception e) {
                // ignore
            }
        });
    }

    @CommandHandler(value="killbots", description="Disconnect all bot players and delete their accounts")
    @AdminRestrictedCommand
    public static void invokeKillBots(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        WorkerThread.doAsync(() -> {
            int disconnected = 0;
            int deleted = 0;

            // Shutdown all bot clients
            synchronized (ACTIVE_BOTS) {
                for (StressTestClient bot : ACTIVE_BOTS) {
                    try {
                        bot.shutdown();
                        disconnected++;
                    } catch (Exception e) {
                        log.error("[BOTS] Failed to shutdown bot: {}", e.getMessage());
                    }
                }
                ACTIVE_BOTS.clear();
            }

            // Delete bot accounts from database
            synchronized (BOT_ACCOUNT_GUIDS) {
                for (String accountGuid : BOT_ACCOUNT_GUIDS) {
                    try {
                        // Get account to find characters
                        PlayerAccountDto account = ServerGameLogic.DATA_SERVICE.executeGet(
                                "/data/account/" + accountGuid, null, PlayerAccountDto.class);
                        if (account != null && account.getCharacters() != null) {
                            for (CharacterDto c : account.getCharacters()) {
                                ServerGameLogic.DATA_SERVICE.executeDelete(
                                        "/data/account/character/" + c.getCharacterUuid(), Object.class);
                            }
                        }
                        deleted++;
                    } catch (Exception e) {
                        log.error("[BOTS] Failed to delete bot account {}: {}", accountGuid, e.getMessage());
                    }
                }
                BOT_ACCOUNT_GUIDS.clear();
            }

            log.info("[BOTS] Killed {} bots, deleted {} accounts", disconnected, deleted);
            try {
                mgr.enqueueServerPacket(target, TextPacket.from("SYSTEM", target.getName(),
                        "Killed " + disconnected + " bots, deleted " + deleted + " accounts"));
            } catch (Exception e) {
                // ignore
            }
        });
    }

    @CommandHandler(value="realm", description="Move the player to the boss realm or the top realm")
    public static void invokeRealmMove(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /realm {up | down}");

        final Realm currentRealm = mgr.findPlayerRealm(target.getId());
        currentRealm.getPlayers().remove(target.getId());
        if (message.getArgs().get(0).equalsIgnoreCase("down")) {
            final PortalModel bossPortal = GameDataManager.PORTALS.get(5);
            final Realm generatedRealm = new Realm(true, bossPortal.getMapId(), bossPortal.getTargetRealmDepth());
            final Vector2f spawnPos = new Vector2f(GlobalConstants.BASE_TILE_SIZE * 12,
                    GlobalConstants.BASE_TILE_SIZE * 13);
            target.setPos(spawnPos);
            generatedRealm.addPlayer(target);
            mgr.addRealm(generatedRealm);
            ServerGameLogic.onPlayerJoin(mgr,  generatedRealm, target);
        }  else if(message.getArgs().get(0).equalsIgnoreCase("up")) {
            currentRealm.removePlayer(target);
            mgr.getTopRealm().addPlayer(target);
            ServerGameLogic.onPlayerJoin(mgr,  mgr.getTopRealm(), target);
        }else {
            throw new IllegalArgumentException("Usage: /realm down");
        }
    }
}
