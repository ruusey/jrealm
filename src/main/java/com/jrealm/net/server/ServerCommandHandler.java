package com.jrealm.net.server;

import java.util.HashMap;
import java.util.Map;

import com.jrealm.account.dto.AccountDto;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.entity.Player;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.messaging.CommandType;
import com.jrealm.game.messaging.ServerCommandMessage;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.game.util.GameObjectUtils;
import com.jrealm.game.util.TriConsumer;
import com.jrealm.net.server.packet.CommandPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerCommandHandler {
	private static final Map<String, TriConsumer<RealmManagerServer, Player, ServerCommandMessage>> COMMAND_CALLBACKS = new HashMap<>();
	// Static callback initialization. Using custom TriConsumer to enforce the method signature
	// of command handler methods
	static {
		COMMAND_CALLBACKS.put("setstat", ServerCommandHandler::invokeSetStats);
		COMMAND_CALLBACKS.put("spawn", ServerCommandHandler::invokeEnemySpawn);
		COMMAND_CALLBACKS.put("effect", ServerCommandHandler::invokeSetEffect);
		COMMAND_CALLBACKS.put("tp", ServerCommandHandler::invokeTeleport);
	}
	
	public static void invokeCommand(RealmManagerServer mgr, CommandPacket command) throws Exception {
		final ServerCommandMessage message = CommandType.fromPacket(command);
		final long fromPlayerId = mgr.getRemoteAddresses().get(command.getSrcIp());
		final Realm playerRealm = mgr.searchRealmsForPlayer(fromPlayerId);
		final Player fromPlayer = playerRealm.getPlayer(fromPlayerId);
		// Look up this players account to see if they are allowed
		// to run Admin server commands
		final AccountDto playerAccount = ServerGameLogic.DATA_SERVICE.executeGet("/admin/account/"+fromPlayer.getAccountUuid(), null, AccountDto.class);
		
		try {
			// has Subscription 'ADMIN'
			if(!playerAccount.isAdmin()) 
				throw new IllegalStateException("Player "+playerAccount.getAccountName()+" is not allowed to use Admin commands.");
			
			TriConsumer<RealmManagerServer, Player, ServerCommandMessage> consumer = COMMAND_CALLBACKS.get(message.getCommand().toLowerCase());
			if(consumer==null) {
				CommandPacket errorResponse = CommandPacket.createError(fromPlayer, 501,
						"Unknown command " + message.getCommand());
				mgr.enqueueServerPacket(fromPlayer, errorResponse);
			}else {
				consumer.accept(mgr, fromPlayer, message);
			}
		} catch (Exception e) {
			log.error("Failed to handle server command. Reason: {}", e);
			final CommandPacket errorResponse = CommandPacket.createError(fromPlayer, 502, e.getMessage());
			mgr.enqueueServerPacket(fromPlayer, errorResponse);
		}
	}
	// Handler methods are passed a reference to the current RealmManager, the invoking Player object
	// and the ServerCommand message object.
	private static void invokeSetStats(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
		if (message.getArgs() == null || message.getArgs().size() != 2)
			throw new IllegalArgumentException("Usage: /setstat {STAT_NAME} {STAT_VALUE}");
		final short valueToSet = Short.parseShort(message.getArgs().get(1));
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
		}
	}
	
	private static void invokeEnemySpawn(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
		if(message.getArgs()==null || message.getArgs().size()!=1) 
			throw new IllegalArgumentException("Usage: /spawn {ENEMY_ID}");
		
		log.info("Player {} spawn enemy {} at {}", target.getName(), message.getArgs().get(0), target.getPos());
		final Realm from = mgr.searchRealmsForPlayer(target.getId());
		final int enemyId = Integer.parseInt(message.getArgs().get(0));
		from.addEnemy(GameObjectUtils.getEnemyFromId(enemyId, target.getPos().clone()));
	}

	private static void invokeSetEffect(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
		if (message.getArgs()==null || message.getArgs().size() < 1)
			throw new IllegalArgumentException("Usage: /seteffect {add | clear} {EFFECT_ID} {DURATION (sec)}");
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
	
	private static void invokeTeleport(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
		if (message.getArgs()==null || message.getArgs().size() < 1)
			throw new IllegalArgumentException("Usage: /tp {PLAYER_NAME}. /tp {X_CORD} {Y_CORD}");
		if(message.getArgs().size()==2) {
			final float destX = Float.parseFloat(message.getArgs().get(0));
			final float destY = Float.parseFloat(message.getArgs().get(1));
			if(destX<GlobalConstants.PLAYER_SIZE || destY<GlobalConstants.PLAYER_SIZE) {
				throw new IllegalArgumentException("Invalid destination");
			}
			target.setPos(new Vector2f(destX, destY));
		}else {
			final Player destPlayer = mgr.searchRealmsForPlayer(message.getArgs().get(0));
			if(destPlayer==null) {
				throw new IllegalArgumentException("Player "+message.getArgs().get(0)+" does not exist.");
			}else {
				target.setPos(destPlayer.getPos().clone());
			}
		}
	}
}
