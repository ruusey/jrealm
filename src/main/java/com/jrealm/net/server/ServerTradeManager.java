package com.jrealm.net.server;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Rectangle;
import com.jrealm.net.client.packet.UpdateTradePacket;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.AcceptTradeRequestPacket;
import com.jrealm.net.client.packet.RequestTradePacket;
import com.jrealm.net.client.packet.UpdatePlayerTradeSelectionPacket;
import com.jrealm.net.entity.NetInventorySelection;
import com.jrealm.net.entity.NetTradeSelection;
import com.jrealm.net.messaging.ServerCommandMessage;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.util.CommandHandler;
import com.jrealm.util.PacketHandlerServer;
import com.jrealm.util.WorkerThread;

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
							// Expire the trade request
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

		if(playerTarget==null) {
			throw new IllegalArgumentException("Unable to find player "+message.getArgs().get(0));
		}else if(playerTarget.getId()==target.getId()) {
			throw new IllegalArgumentException("You cannot trade with yourself!");
		}
		Realm playerRealm = mgr.findPlayerRealm(target.getId());
		Rectangle rect = playerRealm.getTileManager().getRenderViewPort(target);
		if(!rect.inside((int)playerTarget.getPos().x, (int)playerTarget.getPos().y)) {
			throw new IllegalArgumentException("Player "+message.getArgs().get(0)+" is too far away to trade");
		}
		final RequestTradePacket packet = new RequestTradePacket(target.getName());
		mgr.enqueueServerPacket(playerTarget, packet);

		// Set both parties confirmation to 0
		ServerTradeManager.playerTradeConfirmation.put(target.getId(), (short) 0);
		ServerTradeManager.playerTradeConfirmation.put(playerTarget.getId(), (short) 0);

		ServerTradeManager.playerRequestedTrades.put(target.getId(), playerTarget.getId());
		ServerTradeManager.playerTradeTtl.put(target.getId(), Instant.now().toEpochMilli());
		log.info("Player {} requested trade with Player {}", target.getName(), playerTarget.getName());
	}

	@CommandHandler(value = "accept", description = "Accepts trade proposed to user")
	public static void acceptTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message)
			throws Exception {

		Long otherTraderId = ServerTradeManager.findTradePartner(target.getId());
		if (otherTraderId == null) {
			throw new Exception("No trade requests to accept");
		}

		final Player toRespond = mgr.getPlayerById(otherTraderId);
		if (toRespond == null) {
			throw new Exception("No trade requests to accept");
		}
		try {
			mgr.enqueueServerPacket(toRespond, TextPacket.create(target.getName(), toRespond.getName(),
					target.getName() + " has accepted your trade request"));
			mgr.enqueueServerPacket(target,
					TextPacket.create("SYSTEM", target.getName(), "Now trading with player " + toRespond.getName()));
			mgr.enqueueServerPacket(target, new AcceptTradeRequestPacket(true, target, toRespond));
			mgr.enqueueServerPacket(toRespond, new AcceptTradeRequestPacket(true, toRespond, target));
			ServerTradeManager.playerActiveTrades.put(toRespond.getId(), target.getId());
			ServerTradeManager.playerActiveTrades.put(target.getId(), toRespond.getId());

			playerTradeSelections.put(target.getId(),
					new NetInventorySelection(target.getId(), new Boolean[8], target.getInventoryAsNetGameItemRefs()));
			playerTradeSelections.put(toRespond.getId(), new NetInventorySelection(toRespond.getId(), new Boolean[8],
					toRespond.getInventoryAsNetGameItemRefs()));

			// Reset confirmations for the active trade
			ServerTradeManager.playerTradeConfirmation.put(target.getId(), (short) 0);
			ServerTradeManager.playerTradeConfirmation.put(toRespond.getId(), (short) 0);

			final NetTradeSelection selection = NetTradeSelection.getTradeSelection(target, toRespond, new Boolean[8],
					new Boolean[8]);

			final UpdateTradePacket packet = new UpdateTradePacket(selection);

			mgr.enqueueServerPacket(target, packet);
			mgr.enqueueServerPacket(toRespond, packet);
		} catch (Exception e) {
			log.error("Failed to accept trade. Reason: {}", e.getMessage());
			throw new Exception("Failed to accept trade");
		}
	}

	@CommandHandler(value = "decline", description = "Decline a trade request")
	public static void declineTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
		Long otherTraderId = ServerTradeManager.findTradePartner(target.getId());
		if (otherTraderId == null) {
			return;
		}
		final Player toRespond = mgr.getPlayerById(otherTraderId);
		if (toRespond == null) {
			return;
		}

		mgr.enqueueServerPacket(toRespond, TextPacket.create(target.getName(), toRespond.getName(),
				target.getName() + " has declined the trade"));
		mgr.enqueueServerPacket(target, TextPacket.create("SYSTEM", target.getName(), "Trade cancelled"));

		// Close trade UI on both clients
		mgr.enqueueServerPacket(target, new AcceptTradeRequestPacket(false, target, toRespond));
		mgr.enqueueServerPacket(toRespond, new AcceptTradeRequestPacket(false, toRespond, target));

		clearTrade(target.getId(), toRespond.getId());
	}

	@CommandHandler(value = "confirm", description = "Confirm the current trade")
	public static void finalizeTrade(RealmManagerServer mgr, Player target, ServerCommandMessage message)
			throws Exception {
		if (message.getArgs() == null || message.getArgs().size() != 1)
			throw new IllegalArgumentException("Usage: /confirm {true | false}");

		Long otherTraderId = ServerTradeManager.findTradePartner(target.getId());
		if (otherTraderId == null) {
			throw new IllegalArgumentException("You are not in an active trade");
		}
		final Player toRespond = mgr.getPlayerById(otherTraderId);
		if (toRespond == null) {
			throw new IllegalArgumentException("Trade partner not found");
		}

		if (Boolean.parseBoolean(message.getArgs().get(0))) {
			ServerTradeManager.playerTradeConfirmation.put(target.getId(), (short) 1);
			mgr.enqueueServerPacket(toRespond, TextPacket.create("SYSTEM", toRespond.getName(),
					target.getName() + " has confirmed the trade"));

			if (!isTradeConfirmed(target, toRespond)) {
				mgr.enqueueServerPacket(target, TextPacket.create("SYSTEM", target.getName(),
						"Waiting for " + toRespond.getName() + " to confirm"));
				return;
			}

			// Both confirmed - execute the trade
			final NetInventorySelection selection0 = ServerTradeManager.playerTradeSelections
					.get(target.getId());
			final NetInventorySelection selection1 = ServerTradeManager.playerTradeSelections
					.get(toRespond.getId());

			if (selection0 != null && selection1 != null) {
				final GameItem[] p0Selection = target.selectGameItems(selection0.getSelection());
				final GameItem[] p0Cloned = Stream.of(p0Selection).map(GameItem::clone).collect(Collectors.toList())
						.toArray(new GameItem[0]);
				final GameItem[] p1Selection = toRespond.selectGameItems(selection1.getSelection());
				final GameItem[] p1Cloned = Stream.of(p1Selection).map(GameItem::clone).collect(Collectors.toList())
						.toArray(new GameItem[0]);

				target.removeItems(p0Selection);
				toRespond.removeItems(p1Selection);

				// Swap: target gets toRespond's items, toRespond gets target's items
				target.addItems(p1Cloned);
				toRespond.addItems(p0Cloned);
			}

			mgr.enqueueServerPacket(target,
					TextPacket.create("SYSTEM", target.getName(), "Trade completed!"));
			mgr.enqueueServerPacket(toRespond,
					TextPacket.create("SYSTEM", toRespond.getName(), "Trade completed!"));

			// Close trade UI on both clients
			mgr.enqueueServerPacket(target, new AcceptTradeRequestPacket(false, target, toRespond));
			mgr.enqueueServerPacket(toRespond, new AcceptTradeRequestPacket(false, toRespond, target));

			clearTrade(target.getId(), toRespond.getId());
		} else {
			// Player declined via /confirm false
			mgr.enqueueServerPacket(toRespond, TextPacket.create(target.getName(), toRespond.getName(),
					target.getName() + " has cancelled the trade"));
			mgr.enqueueServerPacket(target, TextPacket.create("SYSTEM", target.getName(), "Trade cancelled"));

			// Close trade UI on both clients
			mgr.enqueueServerPacket(target, new AcceptTradeRequestPacket(false, target, toRespond));
			mgr.enqueueServerPacket(toRespond, new AcceptTradeRequestPacket(false, toRespond, target));

			clearTrade(target.getId(), toRespond.getId());
		}
	}

	@PacketHandlerServer(UpdatePlayerTradeSelectionPacket.class)
	public static void handleUpdateTrade(RealmManagerServer mgr, Packet packet) {
		final UpdatePlayerTradeSelectionPacket updateTrade = (UpdatePlayerTradeSelectionPacket) packet;
		final NetInventorySelection selection = updateTrade.getSelection();
		Long otherTraderId = ServerTradeManager.findTradePartner(selection.getPlayerId());
		if (otherTraderId == null) {
			return;
		}
		final Player toRespond = mgr.getPlayerById(otherTraderId);
		final Player updateSource = mgr.getPlayerById(selection.getPlayerId());
		if (toRespond == null || updateSource == null) {
			return;
		}

		// Store the updated selection
		NetInventorySelection storedSelection = ServerTradeManager.playerTradeSelections.get(selection.getPlayerId());
		if (storedSelection != null) {
			storedSelection.setSelection(selection.getSelection());
		} else {
			ServerTradeManager.playerTradeSelections.put(selection.getPlayerId(), selection);
		}

		// Reset confirmations when selection changes
		ServerTradeManager.playerTradeConfirmation.put(selection.getPlayerId(), (short) 0);
		ServerTradeManager.playerTradeConfirmation.put(toRespond.getId(), (short) 0);

		final NetInventorySelection otherSelection = ServerTradeManager.playerTradeSelections.get(toRespond.getId());
		final NetTradeSelection playersSelections = new NetTradeSelection(selection, otherSelection);
		final UpdateTradePacket toSendToTraders = new UpdateTradePacket(playersSelections);
		mgr.enqueueServerPacket(toRespond, toSendToTraders);
		mgr.enqueueServerPacket(updateSource, toSendToTraders);
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

	public static void clearTrade(long p0Id, long p1Id) {
		ServerTradeManager.playerActiveTrades.remove(p0Id);
		ServerTradeManager.playerActiveTrades.remove(p1Id);
		ServerTradeManager.playerRequestedTrades.remove(p0Id);
		ServerTradeManager.playerRequestedTrades.remove(p1Id);
		ServerTradeManager.playerTradeSelections.remove(p0Id);
		ServerTradeManager.playerTradeSelections.remove(p1Id);
		ServerTradeManager.playerTradeConfirmation.remove(p0Id);
		ServerTradeManager.playerTradeConfirmation.remove(p1Id);
		ServerTradeManager.playerTradeTtl.remove(p0Id);
		ServerTradeManager.playerTradeTtl.remove(p1Id);
	}

	private static boolean isTradeConfirmed(Player p0, Player p1) {
		Short p0Conf = playerTradeConfirmation.get(p0.getId());
		Short p1Conf = playerTradeConfirmation.get(p1.getId());
		return p0Conf != null && p1Conf != null && p0Conf == 1 && p1Conf == 1;
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

	/**
	 * Find the trade partner for a given player. Checks both playerRequestedTrades
	 * (as key and value) and playerActiveTrades.
	 */
	public static Long findTradePartner(long playerId) {
		// Check if this player initiated a trade request
		Long partner = ServerTradeManager.playerRequestedTrades.get(playerId);
		if (partner != null) {
			return partner;
		}
		// Check if this player is the target of a trade request (inverse lookup)
		for (Entry<Long, Long> entry : ServerTradeManager.playerRequestedTrades.entrySet()) {
			if (entry.getValue() == playerId) {
				return entry.getKey();
			}
		}
		// Check active trades
		partner = ServerTradeManager.playerActiveTrades.get(playerId);
		if (partner != null) {
			return partner;
		}
		return null;
	}
}
