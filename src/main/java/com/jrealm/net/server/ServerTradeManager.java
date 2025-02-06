package com.jrealm.net.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jrealm.game.entity.Player;
import com.jrealm.game.util.CommandHandler;
import com.jrealm.net.client.packet.InitTradeRequestPacket;
import com.jrealm.net.client.packet.RequestTradePacket;
import com.jrealm.net.messaging.ServerCommandMessage;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerTradeManager {

	public static RealmManagerServer mgr;

	private static Map<Long, Long> playerActiveTrades;
	private static Map<Long, Long> playerRequestedTrades;

	static {
		playerActiveTrades = new HashMap<>();
		playerRequestedTrades = new HashMap<>();
	}
	
	private static Map<Long, Long> inverseRequestedTrades(){
		Map<Long, Long> trades = new HashMap<>();
		for(Entry<Long, Long> entry : playerRequestedTrades.entrySet()) {
			trades.put(entry.getValue(), entry.getKey());
		}
		return trades;
	}
	
	@CommandHandler(value = "trade", description = "Initiated a trade")
	public static void invokeTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message) throws Exception {
		if (message.getArgs() == null || message.getArgs().size() != 1)
			throw new IllegalArgumentException("Usage: /trade {PLAYER_NAME}");
		final Realm playerRealm = mgr.findPlayerRealm(target.getId());
		final Player playerTarget = resolveByName(message.getArgs().get(0));
		RequestTradePacket packet = new RequestTradePacket(target.getName());
		mgr.enqueueServerPacket(playerTarget, packet);
		playerRequestedTrades.put(target.getId(), playerTarget.getId());
		log.info("Player {} requested trade with Player {}", target.getName(), playerTarget.getName(), target.getPos());
	}

	@CommandHandler(value = "accept", description = "Accepts trade proposed to user")
	public static void acceptTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message)
			throws Exception {
		Player toRespond = mgr.getPlayerById(inverseRequestedTrades().get(target.getId()));
		Player from = mgr.getPlayerById(playerRequestedTrades.get(toRespond.getId()));
		
		mgr.enqueueServerPacket(toRespond, TextPacket.create(from.getName(), toRespond.getName(), from.getName()+" has accepted your trade request"));
	}

	public static Player resolveByName(String name) {
		Player result = null;
		for (Realm realm : mgr.getRealms().values()) {
			for (Player player : realm.getPlayers().values()) {
				if (player.getName().equals(name)) {
					result = player;
					break;
				}
			}
			if (result != null) {
				break;
			}
		}
		return result;
	}

	public static void initTrade(Player p, InitTradeRequestPacket request) throws IllegalArgumentException {
		Player target = resolveByName(request.getPlayerName());
		if (target == null) {
			throw new IllegalArgumentException("Unknown player " + request.getPlayerName());

		}
		if (!isTradeReqPending(p.getId()) && !isTrading(p.getId(), target.getId())) {
			playerActiveTrades.put(p.getId(), null);
			Player toTradeWith = resolveByName(request.getPlayerName());
		}
	}

	public static boolean isTradeReqPending(Long srcPlayerId) {
		return playerActiveTrades.get(srcPlayerId) == null;
	}

	public static boolean isTrading(long playerId0, long playerId1) {
		return checkKeysBidirectional(playerId0, playerId1);
	}

	private static boolean checkKeysBidirectional(long playerId0, long playerId1) {
		Long target0 = playerActiveTrades.get(playerId0);
		if (target0 == null)
			return false;
		Long target1 = playerActiveTrades.get(playerId1);
		if (target1 == null)
			return false;
		return ((target0 == playerId1) && (target1 == playerId0)) || ((target1 == playerId0) && (target0 == playerId1));

	}
}
