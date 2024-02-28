package com.jrealm.net.server;

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
		
		try {
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
			CommandPacket errorResponse = CommandPacket.createError(fromPlayer, 502, e.getMessage());
			mgr.enqueueServerPacket(fromPlayer, errorResponse);
		}
	}
	
	private static void invokeSetStats(Player target, ServerCommandMessage message) {
		switch (message.getCommand().toLowerCase()) {
		case "setstat":
			log.info("Player {} set stat {} to {}", target.getName(),message.getArgs().get(0),message.getArgs().get(1));
			switch(message.getArgs().get(0)) {
			case "hp":
				target.getStats().setHp(Short.parseShort(message.getArgs().get(1)));
				break;
			case "mp":
				target.getStats().setMp(Short.parseShort(message.getArgs().get(1)));
				break;
			case "att":
				target.getStats().setAtt(Short.parseShort(message.getArgs().get(1)));
				break;
			case "def":
				target.getStats().setDef(Short.parseShort(message.getArgs().get(1)));
				break;
			case "spd":
				target.getStats().setSpd(Short.parseShort(message.getArgs().get(1)));
				break;
			case "dex":
				target.getStats().setDex(Short.parseShort(message.getArgs().get(1)));
				break;
			case "vit":
				target.getStats().setVit(Short.parseShort(message.getArgs().get(1)));
				break;
			case "wis":
				target.getStats().setWis(Short.parseShort(message.getArgs().get(1)));
				break;
			}
		}
	}
	
	private static void invokeEnemySpawn(RealmManagerServer mgr, Player player, ServerCommandMessage message) {
		log.info("Player {} spawn enemy {} at {}", player.getName(),
				message.getArgs().get(0), player.getPos());
		final Realm from = mgr.searchRealmsForPlayers(player.getId());

		final int enemyId = Integer.parseInt(message.getArgs().get(0));
		from.addEnemy(GameObjectUtils.getEnemyFromId(enemyId, player.getPos().clone()));
	}
	
	private static void invokeSetEffect(Player player, ServerCommandMessage message) throws Exception{
		if (message.getArgs().size() < 3)
			throw new IllegalArgumentException(
					"/effect requires arguments [{add | remove} {EFFECT_ID} {DURATION}");
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
