package com.jrealm.game.realm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.messaging.CommandType;
import com.jrealm.game.messaging.LoginRequestMessage;
import com.jrealm.game.messaging.LoginResponseMessage;
import com.jrealm.game.states.PlayState;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.TimedWorkerThread;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.EntityType;
import com.jrealm.net.Packet;
import com.jrealm.net.PacketType;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.HeartbeatPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("unused")
public class RealmManagerClient implements Runnable {
	private SocketClient client;
	private PlayState state;
	private Realm realm;
	private boolean shutdown = false;

	private final Map<Byte, BiConsumer<RealmManagerClient, Packet>> packetCallbacksClient = new HashMap<>();
	private List<Vector2f> shotDestQueue;
	private long lastUpdateTime;
	private long now;
	private long lastRenderTime;
	private long lastSecondTime;
	
	private int oldFrameCount;
	private int oldTickCount;
	private int tickCount;
	
	private long currentPlayerId;

	public RealmManagerClient(PlayState state, Realm realm) {
		this.registerPacketCallbacks();
		this.realm = realm;
		this.client = new SocketClient(2222);
		this.state = state;
		this.shotDestQueue = new ArrayList<>();
		WorkerThread.submitAndForkRun(this.client);
	}

	@Override
	public void run() {
		log.info("Starting JRealm Client");

		Runnable tick = ()->{
			this.tick();
			this.update(0);
		};
		
		TimedWorkerThread workerThread = new TimedWorkerThread(tick, 32);
		WorkerThread.submitAndForkRun(workerThread);
		
		log.info("RealmManager exiting run().");
	}
	
	private void tick() {
		try {
			Runnable processClientPackets = () -> {
				this.processClientPackets();
			};
			
			WorkerThread.submitAndRun(processClientPackets);
		} catch (Exception e) {
			log.error("Failed to sleep");
		}
	}
	

	public void processClientPackets() {
		while (!this.getClient().getPacketQueue().isEmpty()) {
			Packet toProcess = this.getClient().getPacketQueue().remove();
			try {
				Packet created = Packet.newInstance(toProcess.getId(), toProcess.getData());
				this.packetCallbacksClient.get(created.getId()).accept(this, created);
			} catch (Exception e) {
				log.error("Failed to process server packets {}", e);
			}
		}
	}

	private void registerPacketCallbacks() {
		this.registerPacketCallback(PacketType.UPDATE.getPacketId(), RealmManagerClient::handleUpdateClient);
		this.registerPacketCallback(PacketType.OBJECT_MOVE.getPacketId(), RealmManagerClient::handleObjectMoveClient);
		this.registerPacketCallback(PacketType.TEXT.getPacketId(), RealmManagerClient::handleTextClient);
		this.registerPacketCallback(PacketType.COMMAND.getPacketId(), RealmManagerClient::handleCommandClient);
		this.registerPacketCallback(PacketType.LOAD.getPacketId(), RealmManagerClient::handleLoadClient);

	}

	private void registerPacketCallback(byte packetId, BiConsumer<RealmManagerClient, Packet> callback) {
		this.packetCallbacksClient.put(packetId, callback);
	}
	
	public void update(double time) {
		this.state.update(time);	
	}

	public void startHeartbeatThread() {
		Runnable sendHeartbeat = () -> {
			while (!this.shutdown) {
				try {
					long currentTime = Instant.now().toEpochMilli();
					long playerId = this.currentPlayerId;

					HeartbeatPacket pack = HeartbeatPacket.from(playerId, currentTime);
					this.client.sendRemote(pack);
					Thread.sleep(1000);
				} catch (Exception e) {
					log.error("Failed to send Heartbeat packet. Reason: {}", e);
				}
			}
		};
		WorkerThread.submitAndForkRun(sendHeartbeat);
	}
	
	public static void handleLoadClient(RealmManagerClient cli, Packet packet) {
		LoadPacket textPacket = (LoadPacket) packet;
		try {
			
			for(Player p : textPacket.getPlayers()) {
				if(p.getId()==cli.getCurrentPlayerId()) continue;
				cli.getRealm().addPlayerIfNotExists(p);
			}
			for(LootContainer lc : textPacket.getContainers()) {
				cli.getRealm().addLootContainerIfNotExists(lc);
			}
			
			for(Bullet b : textPacket.getBullets()) {
				cli.getRealm().addBulletIfNotExists(b);
			}
			
			for(Enemy e : textPacket.getEnemies()) {
				cli.getRealm().addEnemyIfNotExists(e);
			}

		}catch(Exception e) {
			log.error("Failed to handle Load Packet. Reason: {}", e);
		}	
	}
	
	public static void handleTextClient(RealmManagerClient cli, Packet packet) {
		TextPacket textPacket = (TextPacket) packet;
		log.info("[CLIENT] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
				textPacket.getFrom(), textPacket.getMessage());
		log.info("Responding with LoginRequest");
		LoginRequestMessage login = LoginRequestMessage.builder().username("ruusey").password("password123").build();
		try {
			CommandPacket loginPacket = CommandPacket.from(CommandType.LOGIN_REQUEST, login);
			cli.getClient().sendRemote(loginPacket);
		}catch(Exception e) {
			log.error("Failed to response to initial text packet. Reason: {}", e.getMessage());
		}	
	}
	
	public static void handleCommandClient(RealmManagerClient cli, Packet packet) {
		CommandPacket commandPacket = (CommandPacket) packet;
		log.info("[CLIENT] Recieved Command Packet for Player {} Command={}", commandPacket.getPlayerId(), commandPacket.getCommand());
		try {
			switch(commandPacket.getCommandId()) {
			case 2:
				LoginResponseMessage loginResponse = CommandType.fromPacket(commandPacket);
				doLoginResponse(cli, loginResponse);
				break;
			}
		}catch(Exception e) {
			log.error("Failed to handle client command packet. Reason: {}", e.getMessage());
		}
	}

	public static void handleObjectMoveClient(RealmManagerClient cli, Packet packet) {
		ObjectMovePacket objectMovePacket = (ObjectMovePacket) packet;
		switch(objectMovePacket.getTargetEntityType()) {
		case PLAYER:
			Player playerToUpdate = cli.getRealm().getPlayer(objectMovePacket.getEntityId());
			if(playerToUpdate==null) break;
			playerToUpdate.applyMovement(objectMovePacket);
			break;
		case ENEMY:
			Enemy enemyToUpdate = cli.getRealm().getEnemy(objectMovePacket.getEntityId());
			if(enemyToUpdate == null) break;
			enemyToUpdate.applyMovement(objectMovePacket);
			break;
		case BULLET:
			Bullet bulletToUpdate = cli.getRealm().getBullet(objectMovePacket.getEntityId());
			bulletToUpdate.applyMovement(objectMovePacket);
			break;
		default:
			break;
		}
//		log.info("[CLIENT] Recieved ObjectMove Packet for Game Object {} ID {}",
//				EntityType.valueOf(objectMovePacket.getEntityType()), objectMovePacket.getEntityId());
	}

	public static void handleUpdateClient(RealmManagerClient cli, Packet packet) {
		UpdatePacket updatePacket = (UpdatePacket) packet;
		if(updatePacket.getPlayerId()!=cli.getCurrentPlayerId()) return;
		Player toUpdate = cli.getRealm().getPlayer((updatePacket.getPlayerId()));
		toUpdate.applyUpdate(updatePacket);
//		log.info("[CLIENT] Recieved PlayerUpdate Packet for Player ID {}", updatePacket.getPlayerId());
	}
	
	private static void doLoginResponse(RealmManagerClient cli, LoginResponseMessage loginResponse) {
		try {
			if(loginResponse.isSuccess()) {
				Camera c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
				CharacterClass cls = CharacterClass.ROGUE;
				Player player = new Player(loginResponse.getPlayerId(), c, GameDataManager.loadClassSprites(cls),
						new Vector2f((0 + (GamePanel.width / 2)) - GlobalConstants.PLAYER_SIZE - 350,
								(0 + (GamePanel.height / 2)) - GlobalConstants.PLAYER_SIZE),
						GlobalConstants.PLAYER_SIZE, cls);
				log.info("Login succesful, added Player ID {}", player.getId());
				cli.getState().loadClass(player, cls, true);
				cli.setCurrentPlayerId(player.getId());
				cli.getState().setPlayerId(player.getId());
				cli.startHeartbeatThread();
			}
		}catch(Exception e) {
			log.error("Failed to response to login response. Reason: {}", e.getMessage());
		}
	}
}