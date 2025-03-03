package com.jrealm.net.server;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.util.CommandHandler;
import com.jrealm.game.util.PacketHandlerServer;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.packet.UpdateTradePacket;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.AcceptTradeRequestPacket;
import com.jrealm.net.client.packet.RequestTradePacket;
import com.jrealm.net.client.packet.UpdatePlayerTradeSelectionPacket;
import com.jrealm.net.entity.NetInventorySelection;
import com.jrealm.net.entity.NetTradeSelection;
import com.jrealm.net.messaging.ServerCommandMessage;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerTradeManager {
	public static RealmManagerServer mgr;
	public static boolean shutdown = false;

	private static Map<Long, Long> playerActiveTrades;
	private static Map<Long, Long> playerRequestedTrades;
	private static Map<Long, Short> playerTradeConfirmation;
	private static Map<Long, NetInventorySelection> playerTradeSelections;
	private static Map<Long, Long> playerTradeTtl;

	static {
		ServerTradeManager.playerActiveTrades = new HashMap<>();
		ServerTradeManager.playerRequestedTrades = new HashMap<>();
		ServerTradeManager.playerTradeSelections = new HashMap<>();
		ServerTradeManager.playerTradeTtl = new HashMap<>();
		ServerTradeManager.playerTradeConfirmation = new HashMap<>();
	}

	public static void runTradeExpiryCheck() {
		Runnable check = () -> {
			while (!ServerTradeManager.shutdown) {
				try {
					for (Entry<Long, Long> entry : ServerTradeManager.playerTradeTtl.entrySet()) {
						if ((Instant.now().toEpochMilli() - entry.getValue()) > 15000) {
							ServerTradeManager.playerRequestedTrades.remove(entry.getKey());
							// Expirie the trade request
						}
					}
					Thread.sleep(500);
				} catch (Exception e) {
					log.error("Failed to expire player trade request. Reason: {}", e.getMessage());
				}
			}
		};
		WorkerThread.submitAndForkRun(check);
	}

	@CommandHandler(value = "trade", description = "Initiate a trade with a player")
	public static void invokeTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message)
			throws Exception {
		if (message.getArgs() == null || message.getArgs().size() != 1)
			throw new IllegalArgumentException("Usage: /trade {PLAYER_NAME}");

		if (isTradeReqPending(target.getId())) {
			throw new Exception("You already have a pending trade request");
		}
		final Player playerTarget = mgr.findPlayerByName(message.getArgs().get(0));
		final RequestTradePacket packet = new RequestTradePacket(target.getName());
		mgr.enqueueServerPacket(playerTarget, packet);
		// Set both parties confirmation to 0
		ServerTradeManager.playerTradeConfirmation.put(target.getId(), (short) 0);
		ServerTradeManager.playerTradeConfirmation.put(playerTarget.getId(), (short) 0);

		ServerTradeManager.playerRequestedTrades.put(target.getId(), playerTarget.getId());
		ServerTradeManager.playerTradeTtl.put(target.getId(), Instant.now().toEpochMilli());
		log.info("Player {} requested trade with Player {}", target.getName(), playerTarget.getName(), target.getPos());
	}

	@CommandHandler(value = "accept", description = "Accepts trade proposed to user")
	public static void acceptTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message)
			throws Exception {

		long otherTraderId = ServerTradeManager.inverseRequestedTrades().get(target.getId());

		final Player toRespond = mgr.getPlayerById(otherTraderId);
		if (toRespond == null) {
			throw new Exception("No trade requests to accept");
		}
		// Simulate receiving an AcceptTradeRequesstPacket
		final Player from = toRespond;
		try {
			mgr.enqueueServerPacket(toRespond, TextPacket.create(from.getName(), toRespond.getName(),
					from.getName() + " has accepted your trade request"));
			mgr.enqueueServerPacket(target,
					TextPacket.create("SYSTEM", target.getName(), "Now trading with player" + toRespond.getName()));
			mgr.enqueueServerPacket(target, new AcceptTradeRequestPacket(true));
			mgr.enqueueServerPacket(toRespond, new AcceptTradeRequestPacket(true));
			ServerTradeManager.playerActiveTrades.put(toRespond.getId(), target.getId());
			ServerTradeManager.playerActiveTrades.put(target.getId(), toRespond.getId());

			playerTradeSelections.put(target.getId(),
					new NetInventorySelection(target.getId(), new Boolean[8], target.getInventoryAsNetGameItemRefs()));
			playerTradeSelections.put(toRespond.getId(), new NetInventorySelection(toRespond.getId(), new Boolean[8],
					toRespond.getInventoryAsNetGameItemRefs()));

			final NetTradeSelection selection = NetTradeSelection.getTradeSelection(target, toRespond, new Boolean[8],
					new Boolean[8]);

			final UpdateTradePacket packet = new UpdateTradePacket(selection);

			mgr.enqueueServerPacket(target, packet);
			mgr.enqueueServerPacket(toRespond, packet);
		} catch (Exception e) {
			throw new Exception("Unparseable boolean value " + message.getArgs().get(0));
		}
	}

	@CommandHandler(value = "decline", description = "Decline a trade request")
	public static void declineTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
		final long otherTraderId = ServerTradeManager.inverseRequestedTrades().get(target.getId());
		final Player toRespond = mgr.getPlayerById(otherTraderId);

		mgr.enqueueServerPacket(toRespond, TextPacket.create(toRespond.getName(), toRespond.getName(),
				toRespond.getName() + " has rejected your trade request"));
		ServerTradeManager.playerActiveTrades.remove(toRespond.getId());
		ServerTradeManager.playerRequestedTrades.remove(target.getId());
		ServerTradeManager.playerTradeSelections.remove(toRespond.getId());
		ServerTradeManager.playerTradeSelections.remove(target.getId());
		ServerTradeManager.playerTradeConfirmation.remove(toRespond.getId());
		ServerTradeManager.playerTradeConfirmation.remove(toRespond.getId());
	}

	@CommandHandler(value = "confirm", description = "Accepts trade proposed to user")
	public static void finalizeTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message)
			throws Exception {
		if (message.getArgs() == null || message.getArgs().size() != 1)
			throw new IllegalArgumentException("Usage: /confirm {true | false}");

		try {
			final Player toRespond = mgr.getPlayerById(ServerTradeManager.inverseRequestedTrades().get(target.getId()));

			if (Boolean.parseBoolean(message.getArgs().get(0))) {
				ServerTradeManager.playerTradeConfirmation.put(target.getId(), (short) 1);
				if (!isTradeConfirmed(target, toRespond)) {
					throw new IllegalArgumentException(
							"Waiting on other player to confirm trade: /confirm {true | false}");
				}
				if (ServerTradeManager.playerTradeSelections.get(target.getId()) != null
						|| ServerTradeManager.playerTradeSelections.get(toRespond.getId()) != null) {
					final NetInventorySelection selection0 = ServerTradeManager.playerTradeSelections
							.get(target.getId());
					final NetInventorySelection selection1 = ServerTradeManager.playerTradeSelections
							.get(toRespond.getId());
					final GameItem[] p0Selection = target.selectGameItems(selection0.getSelection());
					final GameItem[] p0Cloned = Stream.of(p0Selection).map(GameItem::clone).collect(Collectors.toList())
							.toArray(new GameItem[0]);
					final GameItem[] p1Selection = toRespond.selectGameItems(selection1.getSelection());
					final GameItem[] p1Cloned = Stream.of(p1Selection).map(GameItem::clone).collect(Collectors.toList())
							.toArray(new GameItem[0]);
					target.removeItems(p0Selection);
					toRespond.removeItems(p1Selection);

					target.addItems(p1Cloned);
					target.addItems(p0Cloned);
				}

				final NetTradeSelection selection = NetTradeSelection.getTradeSelection(target, toRespond,
						new Boolean[8], new Boolean[8]);
				final UpdateTradePacket packet = new UpdateTradePacket(selection);
				ServerTradeManager.finalizeTrade(packet);
			} else {
				ServerTradeManager.playerActiveTrades.remove(toRespond.getId());
				ServerTradeManager.playerRequestedTrades.remove(target.getId());
				ServerTradeManager.playerTradeSelections.remove(toRespond.getId());
				ServerTradeManager.playerTradeSelections.remove(target.getId());
				ServerTradeManager.playerTradeConfirmation.remove(target.getId());
				ServerTradeManager.playerTradeConfirmation.remove(toRespond.getId());
				mgr.enqueueServerPacket(toRespond, TextPacket.create(target.getName(), toRespond.getName(),
						target.getName() + " has rejected your trade request"));
			}
		} catch (Exception e) {
			throw e;
		}
	}

	@PacketHandlerServer(UpdatePlayerTradeSelectionPacket.class)
	public static void handleUpdateTrade(RealmManagerServer mgr, Packet packet) {
		final UpdatePlayerTradeSelectionPacket updateTrade = (UpdatePlayerTradeSelectionPacket) packet;
		final NetInventorySelection selection = updateTrade.getSelection();
		final Player toRespond = mgr
				.getPlayerById(ServerTradeManager.inverseRequestedTrades().get(selection.getPlayerId()));
		final Player updateSource = mgr.getPlayerById(selection.getPlayerId());
		final NetInventorySelection otherSelection = ServerTradeManager.playerTradeSelections.get(toRespond.getId());
		// ServerTradeManager.playerTradeSelections.put(selection.getPlayerId(),
		// selection);
		final NetTradeSelection playersSelections = new NetTradeSelection(selection, otherSelection);
		final UpdateTradePacket toSendToTraders = new UpdateTradePacket(playersSelections);
		mgr.enqueueServerPacket(toRespond, toSendToTraders);
		mgr.enqueueServerPacket(updateSource, toSendToTraders);

	}

	@SuppressWarnings("unused")
	public static void finalizeTrade(UpdateTradePacket finalizedTrade) {
		final Player source = mgr.getPlayerById(finalizedTrade.getSelections().getPlayer0Selection().getPlayerId());
		final Player target = mgr.getPlayerById(finalizedTrade.getSelections().getPlayer1Selection().getPlayerId());
		playerActiveTrades.remove(source.getId());
		playerRequestedTrades.remove(source.getId());
		playerActiveTrades.remove(target.getId());
		playerRequestedTrades.remove(target.getId());
		mgr.enqueueServerPacket(source,
				TextPacket.create(target.getName(), source.getName(), target.getName() + " finalized the trade"));
		mgr.enqueueServerPacket(target,
				TextPacket.create(source.getName(), target.getName(), source.getName() + " finalized the trade"));
	}

	public static void initTrade(Player requestor, RequestTradePacket request) throws IllegalArgumentException {
		final Player target = mgr.findPlayerByName(request.getRequestingPlayerName());
		if (target == null) {
			throw new IllegalArgumentException("Unknown player " + request.getRequestingPlayerName());
		} else if (!isTradeReqPending(requestor.getId()) && !isTrading(requestor.getId(), target.getId())) {
			ServerTradeManager.playerRequestedTrades.put(requestor.getId(), target.getId());
		}
	}

	public static boolean isTradeReqPending(Long srcPlayerId) {
		return ServerTradeManager.playerRequestedTrades.get(srcPlayerId) != null;
	}

	public static boolean isTrading(long playerId0, long playerId1) {
		return checkKeysBidirectional(playerId0, playerId1);
	}

	private static boolean isTradeConfirmed(Player p0, Player p1) {
		return playerTradeConfirmation.get(p0.getId()) == 1 && playerTradeConfirmation.get(p1.getId()) == 1;
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
