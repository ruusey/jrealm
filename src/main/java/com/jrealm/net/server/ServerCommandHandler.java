package com.jrealm.net.server;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jrealm.account.dto.AccountDto;
import com.jrealm.game.GameLauncher;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.LootTier;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.CharacterClassModel;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.tile.Tile;
import com.jrealm.game.util.CommandHandler;
import com.jrealm.game.util.GameObjectUtils;
import com.jrealm.net.messaging.CommandType;
import com.jrealm.net.messaging.ServerCommandMessage;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerCommandHandler {
    public static final Map<String, MethodHandle> COMMAND_CALLBACKS = new HashMap<>();
    public static final Map<String, CommandHandler> COMMAND_DESCRIPTIONS = new HashMap<>();

    public static void invokeCommand(RealmManagerServer mgr, CommandPacket command) throws Exception {
        final ServerCommandMessage message = CommandType.fromPacket(command);
        final long fromPlayerId = mgr.getRemoteAddresses().get(command.getSrcIp());
        final Realm playerRealm = mgr.findPlayerRealm(fromPlayerId);
        final Player fromPlayer = playerRealm.getPlayer(fromPlayerId);
        // Look up this players account to see if they are allowed
        // to run Admin server commands
//        final AccountDto playerAccount = ServerGameLogic.DATA_SERVICE
//                .executeGet("/admin/account/" + fromPlayer.getAccountUuid(), null, AccountDto.class);

        try {
            // has Subscription 'ADMIN'
//            if (!playerAccount.isAdmin())
//                throw new IllegalStateException(
//                        "Player " + playerAccount.getAccountName() + " is not allowed to use Admin commands.");

            MethodHandle methodHandle = COMMAND_CALLBACKS.get(message.getCommand().toLowerCase());

            if (methodHandle == null) {
                CommandPacket errorResponse = CommandPacket.createError(fromPlayer, 501,
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
	public static void invokeOpUser(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
		if (message.getArgs() == null || message.getArgs().size() < 1)
			throw new IllegalArgumentException("Usage: /op {PLAYER_NAME}");
		log.info("**Playe OP** Player {} is promoting/demoting {} to/from server operator", target.getAccountUuid(),
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

    // Handler methods are passed a reference to the current RealmManager, the
    // invoking Player object
    // and the ServerCommand message object.
    @CommandHandler(value="stat", description="Modify or max individual Player stats")
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
        log.info("Player {} request command about.", target.getName());
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
    public static void invokePlayerHeal(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        target.setHealth(target.getComputedStats().getHp());
        target.setMana(target.getComputedStats().getMp());
        log.info("Player {} healed themselves", target.getName());
    }

    @CommandHandler(value="spawn", description="Spawn a given Enemy by it's id")
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
    public static void invokeSetEffect(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /seteffect {add | clear} {EFFECT_ID} {DURATION (sec)}");
        log.info("Player {} set effect {}", target.getName(), message);
        switch (message.getArgs().get(0)) {
        case "add":
            target.addEffect(EffectType.valueOf(Short.valueOf(message.getArgs().get(1))),
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

    @CommandHandler(value="realm", description="Move the player to the boss realm or the top realm")
    public static void invokeRealmMove(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /realm {up | down}");

        final Realm currentRealm = mgr.findPlayerRealm(target.getId());
        
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
        currentRealm.getPlayers().remove(target.getId());
    }
}
