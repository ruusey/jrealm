package com.jrealm.net.server;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

import com.jrealm.account.dto.AccountDto;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.LootTier;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.messaging.CommandType;
import com.jrealm.game.messaging.ServerCommandMessage;
import com.jrealm.game.model.CharacterClassModel;
import com.jrealm.game.model.PortalModel;
import com.jrealm.game.util.CommandHandler;
import com.jrealm.game.util.GameObjectUtils;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerCommandHandler {
    public static final Map<String, MethodHandle> COMMAND_CALLBACKS = new HashMap<>();

    public static void invokeCommand(RealmManagerServer mgr, CommandPacket command) throws Exception {
        final ServerCommandMessage message = CommandType.fromPacket(command);
        final long fromPlayerId = mgr.getRemoteAddresses().get(command.getSrcIp());
        final Realm playerRealm = mgr.findPlayerRealm(fromPlayerId);
        final Player fromPlayer = playerRealm.getPlayer(fromPlayerId);
        // Look up this players account to see if they are allowed
        // to run Admin server commands
        final AccountDto playerAccount = ServerGameLogic.DATA_SERVICE
                .executeGet("/admin/account/" + fromPlayer.getAccountUuid(), null, AccountDto.class);

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

    // Handler methods are passed a reference to the current RealmManager, the
    // invoking Player object
    // and the ServerCommand message object.
    @CommandHandler("setstat")
    public static void invokeSetStats(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /setstat {STAT_NAME} {STAT_VALUE}");
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

    @CommandHandler("testplayers")
    public static void invokeSpawnTest(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() != 1)
            throw new IllegalArgumentException("Usage: /testplayers {COUNT}");

        log.info("Player {} spawn {} players  at {}", target.getName(), message.getArgs().get(0), target.getPos());
        mgr.spawnTestPlayers(mgr.getTopRealm().getRealmId(), Integer.parseInt(message.getArgs().get(0)),
                target.getPos().clone());
    }

    @CommandHandler("help")
    public static void invokeHelp(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        String commandHelpText = "Available Commands:   ";
        for (String commandHandlerKey : COMMAND_CALLBACKS.keySet()) {
            commandHelpText += "/" + commandHandlerKey + "     ";
        }
        TextPacket commandHelp = TextPacket.from("SYSTEM", target.getName(), commandHelpText);
        mgr.enqueueServerPacket(target, commandHelp);
        log.info("Player {} request command help.", commandHelp);
    }

    @CommandHandler("spawn")
    public static void invokeEnemySpawn(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() != 1)
            throw new IllegalArgumentException("Usage: /spawn {ENEMY_ID}");

        log.info("Player {} spawn enemy {} at {}", target.getName(), message.getArgs().get(0), target.getPos());
        final Realm from = mgr.findPlayerRealm(target.getId());
        final int enemyId = Integer.parseInt(message.getArgs().get(0));
        from.addEnemy(GameObjectUtils.getEnemyFromId(enemyId, target.getPos().clone()));
    }

    @CommandHandler("seteffect")
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

    @CommandHandler("tp")
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

    @CommandHandler("item")
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

    @CommandHandler("realm")
    public static void invokeRealmMove(RealmManagerServer mgr, Player target, ServerCommandMessage message)
            throws Exception {
        if (message.getArgs() == null || message.getArgs().size() < 1)
            throw new IllegalArgumentException("Usage: /realm {up | down}");

        final Realm targetRealm = mgr.findPlayerRealm(target.getId());
        final PortalModel bossPortal = GameDataManager.PORTALS.get(5);
        final Realm generatedRealm = new Realm(true, bossPortal.getMapId(), bossPortal.getTargetRealmDepth());
        final Vector2f spawnPos = new Vector2f(GlobalConstants.BASE_TILE_SIZE * 12,
                GlobalConstants.BASE_TILE_SIZE * 13);
        final Portal exitPortal = new Portal(Realm.RANDOM.nextLong(), (short) 3, spawnPos.clone(250, 0));
        if (message.getArgs().get(0).equalsIgnoreCase("up")) {
            target.setPos(spawnPos);
            Enemy enemy = GameObjectUtils.getEnemyFromId(13, spawnPos);
            int healthMult = (4);
            enemy.setHealth(enemy.getHealth() * healthMult);
            enemy.setPos(spawnPos.clone(200, 0));
            generatedRealm.addEnemy(enemy);
            exitPortal.linkPortal(generatedRealm, mgr.getTopRealm());
            exitPortal.setNeverExpires();
            generatedRealm.addPortal(exitPortal);
            generatedRealm.addPlayer(target);
            mgr.addRealm(generatedRealm);
        } else if (message.getArgs().get(0).equalsIgnoreCase("down")) {
            target.setPos(spawnPos);
            Enemy enemy = GameObjectUtils.getEnemyFromId(13, spawnPos);
            int healthMult = (4);
            enemy.setHealth(enemy.getHealth() * healthMult);
            enemy.setPos(spawnPos.clone(200, 0));
            generatedRealm.addEnemy(enemy);
            exitPortal.linkPortal(generatedRealm, mgr.getTopRealm());
            exitPortal.setNeverExpires();
            generatedRealm.addPortal(exitPortal);
            generatedRealm.addPlayer(target);
            mgr.addRealm(generatedRealm);
        } else {
            throw new IllegalArgumentException("Usage: /realm {up | down}");
        }
        targetRealm.getPlayers().remove(target.getId());
    }
}
