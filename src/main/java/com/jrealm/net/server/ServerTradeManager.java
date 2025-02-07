package com.jrealm.net.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jrealm.game.entity.Player;
import com.jrealm.game.util.CommandHandler;
import com.jrealm.net.client.packet.FinalizeTradePacket;
import com.jrealm.net.client.packet.RequestTradePacket;
import com.jrealm.net.entity.NetTradeSelection;
import com.jrealm.net.messaging.ServerCommandMessage;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerTradeManager {
	public static RealmManagerServer mgr;

	private static Map<Long, Long> playerActiveTrades;
	private static Map<Long, Long> playerRequestedTrades;

	static {
		ServerTradeManager.playerActiveTrades = new HashMap<>();
		ServerTradeManager.playerRequestedTrades = new HashMap<>();
	}

	@CommandHandler(value = "trade", description = "Initiated a trade")
	public static void invokeTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message)
			throws Exception {
		if (message.getArgs() == null || message.getArgs().size() != 1)
			throw new IllegalArgumentException("Usage: /trade {PLAYER_NAME}");
		
		if(isTradeReqPending(target.getId())) {
			throw new Exception("You already have a pending trade request");
		}
		final Player playerTarget = mgr.findPlayerByName(message.getArgs().get(0));
		final RequestTradePacket packet = new RequestTradePacket(target.getName());
		mgr.enqueueServerPacket(playerTarget, packet);

		ServerTradeManager.playerRequestedTrades.put(target.getId(), playerTarget.getId());
		log.info("Player {} requested trade with Player {}", target.getName(), playerTarget.getName(), target.getPos());
	}
	// TODO: Remove and move to client side 
	@CommandHandler(value = "accept", description = "Accepts trade proposed to user")
	public static void acceptTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message)
			throws Exception {
		if (message.getArgs() == null || message.getArgs().size() != 1)
			throw new IllegalArgumentException("Usage: /accept {true | false}");
		final Player toRespond = mgr.getPlayerById(ServerTradeManager.inverseRequestedTrades().get(target.getId()));
		if(toRespond== null) {
			throw new Exception("No trade requests to accept");
		}
		// Simulate receiving an AcceptTradeRequesstPacket
		final Player from = mgr.getPlayerById(ServerTradeManager.playerRequestedTrades.get(toRespond.getId()));
		try {
			if (Boolean.parseBoolean(message.getArgs().get(0))) {
				
				mgr.enqueueServerPacket(toRespond, TextPacket.create(from.getName(), toRespond.getName(),
						from.getName() + " has accepted your trade request"));
				ServerTradeManager.playerActiveTrades.put(toRespond.getId(), target.getId());
			} else {
				mgr.enqueueServerPacket(toRespond, TextPacket.create(from.getName(), toRespond.getName(),
						from.getName() + " has rejected your trade request"));
				playerActiveTrades.remove(from.getId());
				playerRequestedTrades.remove(from.getId());
			}
		} catch (Exception e) {
			throw new Exception("Unparseable boolean value " + message.getArgs().get(0));
		}
	}
	
	@CommandHandler(value = "finalize", description = "Accepts trade proposed to user")
	public static void finalizeTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message)
			throws Exception {
		if (message.getArgs() == null || message.getArgs().size() != 1)
			throw new IllegalArgumentException("Usage: /finalize {true | false}");
		
		try {
			final Player toRespond = mgr.getPlayerById(ServerTradeManager.inverseRequestedTrades().get(target.getId()));
			if (Boolean.parseBoolean(message.getArgs().get(0))) {
				final NetTradeSelection selection = NetTradeSelection.getTradeSelection(target, toRespond, new boolean[8], new boolean[8]);
				final FinalizeTradePacket packet = new FinalizeTradePacket(selection);
				ServerTradeManager.finalizeTrade(packet);
			}else {
				mgr.enqueueServerPacket(toRespond, TextPacket.create(target.getName(), toRespond.getName(),
						target.getName() + " has rejected your trade request"));
			}
		}catch(Exception e) {
			throw new Exception(e.getMessage());
		}
	}
	
	@SuppressWarnings("unused")
	public static void finalizeTrade(FinalizeTradePacket finalizedTrade) {
		final Player source = mgr.getPlayerById(finalizedTrade.getSelection().getPlayer0Selection().getPlayerId());
		final Player target = mgr.getPlayerById(finalizedTrade.getSelection().getPlayer1Selection().getPlayerId());
		playerActiveTrades.remove(source.getId());
		playerRequestedTrades.remove(source.getId());
		playerActiveTrades.remove(target.getId());
		playerRequestedTrades.remove(target.getId());
		mgr.enqueueServerPacket(source, TextPacket.create(target.getName(), source.getName(),
				target.getName() + " finalized the trade"));
		mgr.enqueueServerPacket(target, TextPacket.create(source.getName(), target.getName(),
				source.getName() + " finalized the trade"));
	}

	

	public static void initTrade(Player requestor, RequestTradePacket request) throws IllegalArgumentException {
		final Player target = mgr.findPlayerByName(request.getRequestingPlayerName());
		if (target == null) {
			throw new IllegalArgumentException("Unknown player " + request.getRequestingPlayerName());
		}else if (!isTradeReqPending(requestor.getId()) && !isTrading(requestor.getId(), target.getId())) {
			ServerTradeManager.playerRequestedTrades.put(requestor.getId(), target.getId());
		}
	}

	public static boolean isTradeReqPending(Long srcPlayerId) {
		return ServerTradeManager.playerActiveTrades.get(srcPlayerId) == null;
	}

	public static boolean isTrading(long playerId0, long playerId1) {
		return checkKeysBidirectional(playerId0, playerId1);
	}

	private static boolean checkKeysBidirectional(long playerId0, long playerId1) {
		final Long target0 = ServerTradeManager.playerActiveTrades.get(playerId0);
		if (target0 == null)
			return false;
		final Long target1 = ServerTradeManager.playerActiveTrades.get(playerId1);
		if (target1 == null)
			return false;
		return ((target0 == playerId1) && (target1 == playerId0)) || ((target1 == playerId0) && (target0 == playerId1));

	}
	
	private static Map<Long, Long> inverseRequestedTrades() {
		final Map<Long, Long> trades = new HashMap<>();
		for (Entry<Long, Long> entry : ServerTradeManager.playerRequestedTrades.entrySet()) {
			trades.put(entry.getValue(), entry.getKey());
		}
		return trades;
	}
}
