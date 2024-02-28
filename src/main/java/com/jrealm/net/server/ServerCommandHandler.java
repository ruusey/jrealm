package com.jrealm.net.server;

import com.jrealm.account.dto.AccountDto;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.entity.Player;
import com.jrealm.game.messaging.CommandType;
import com.jrealm.game.messaging.ServerCommandMessage;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.game.util.GameObjectUtils;
import com.jrealm.net.server.packet.CommandPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerCommandHandler {
	public static void invokeCommand(RealmManagerServer mgr, CommandPacket command) throws Exception {
		final ServerCommandMessage message = CommandType.fromPacket(command);
		final long fromPlayerId = mgr.getRemoteAddresses().get(command.getSrcIp());
		final Realm playerRealm = mgr.searchRealmsForPlayers(fromPlayerId);
		final Player fromPlayer = playerRealm.getPlayer(fromPlayerId);
		// Look up this players account to see if they are allowed
		// to run Admin server commands
		final AccountDto playerAccount = ServerGameLogic.DATA_SERVICE.executeGet("/admin/account/"+fromPlayer.getAccountUuid(), null, AccountDto.class);
		
		try {
			if(!playerAccount.isAdmin()) 
				throw new IllegalStateException("Player "+playerAccount.getAccountName()+" is not allowed to use Admin commands.");
			
			switch (message.getCommand().toLowerCase()) {
			case "setstat":
				invokeSetStats(fromPlayer, message);
				break;
			case "spawn":
				invokeEnemySpawn(mgr, fromPlayer, message);
				break;
			case "seteffect":
				invokeSetEffect(fromPlayer, message);
				break;
			default:
				CommandPacket errorResponse = CommandPacket.createError(fromPlayer, 501,
						"Unknown command " + message.getCommand());
				mgr.enqueueServerPacket(fromPlayer, errorResponse);
			}
		} catch (Exception e) {
			log.error("Failed to handle server command. Reason: {}", e);
			final CommandPacket errorResponse = CommandPacket.createError(fromPlayer, 502, e.getMessage());
			mgr.enqueueServerPacket(fromPlayer, errorResponse);
		}
	}

	private static void invokeSetStats(Player target, ServerCommandMessage message) throws Exception {
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

	private static void invokeEnemySpawn(RealmManagerServer mgr, Player player, ServerCommandMessage message)
			throws Exception {
		if(message.getArgs()==null || message.getArgs().size()!=1) 
			throw new IllegalArgumentException("Usage: /spawn {ENEMY_ID}");
		
		log.info("Player {} spawn enemy {} at {}", player.getName(), message.getArgs().get(0), player.getPos());
		final Realm from = mgr.searchRealmsForPlayers(player.getId());
		final int enemyId = Integer.parseInt(message.getArgs().get(0));
		from.addEnemy(GameObjectUtils.getEnemyFromId(enemyId, player.getPos().clone()));
	}

	private static void invokeSetEffect(Player player, ServerCommandMessage message) throws Exception {
		if (message.getArgs()==null || message.getArgs().size() < 3)
			throw new IllegalArgumentException("Usage: /effect {add | clear} {EFFECT_ID} {DURATION (sec)}");
		switch (message.getArgs().get(0)) {
		case "add":
			player.addEffect(EffectType.valueOf(Short.valueOf(message.getArgs().get(1))),
					1000 * Long.parseLong(message.getArgs().get(2)));
			break;
		case "clear":
			player.resetEffects();
			break;
		}
	}
}
