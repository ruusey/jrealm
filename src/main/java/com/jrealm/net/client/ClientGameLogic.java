package com.jrealm.net.client;

import com.jrealm.account.service.JrealmClientDataService;
import com.jrealm.game.GameLauncher;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EntityType;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.contants.TextEffect;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.Portal;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.state.GameStateManager;
import com.jrealm.game.ui.EffectText;
import com.jrealm.net.Packet;
import com.jrealm.net.client.packet.LoadMapPacket;
import com.jrealm.net.client.packet.LoadPacket;
import com.jrealm.net.client.packet.ObjectMovePacket;
import com.jrealm.net.client.packet.ObjectMovement;
import com.jrealm.net.client.packet.PlayerDeathPacket;
import com.jrealm.net.client.packet.TextEffectPacket;
import com.jrealm.net.client.packet.UnloadPacket;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.messaging.CommandType;
import com.jrealm.net.messaging.LoginResponseMessage;
import com.jrealm.net.messaging.PlayerAccountMessage;
import com.jrealm.net.messaging.ServerErrorMessage;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerClient;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientGameLogic {
    public static JrealmClientDataService DATA_SERVICE = null;

	
    public static void handlePlayerDeathClient(RealmManagerClient cli, Packet packet) {
        @SuppressWarnings("unused")
        // Unused until this contains user spefic death data.
        final PlayerDeathPacket playerDeath = (PlayerDeathPacket) packet;
        try {
            cli.getState().getRealmManager().getClient().setShutdown(true);
            cli.getState().getRealmManager().getWorkerThread().setShutdown(true);
            cli.getState().gsm.add(GameStateManager.GAMEOVER);
            cli.getState().gsm.pop(GameStateManager.PLAY);
        } catch (Exception e) {
            ClientGameLogic.log.error("Failed to handle LoadMap Packet. Reason: {}", e);
        }
    }

    public static void handleTextEffectClient(RealmManagerClient cli, Packet packet) {
        final TextEffectPacket textEffect = (TextEffectPacket) packet;
        try {
            final Realm clientRealm = cli.getState().getRealmManager().getRealm();
            Vector2f targetPos = null;
            try {
                switch (EntityType.valueOf(textEffect.getEntityType())) {
                case BULLET:
                    final Bullet b = clientRealm.getBullet(textEffect.getTargetEntityId());
                    if (b == null) {
                        ClientGameLogic.log.warn("[CLIENT] Bullet with id {} was not found for targeted TextEffect",
                                textEffect.getTargetEntityId());
                        return;
                    }
                    targetPos = b.getPos();

                    break;
                case ENEMY:
                    final Enemy e = clientRealm.getEnemy(textEffect.getTargetEntityId());
                    if (e == null) {
                        ClientGameLogic.log.warn("[CLIENT] Enemy with id {} was not found for targeted TextEffect",
                                textEffect.getTargetEntityId());
                        return;
                    }
                    targetPos = e.getPos();
                    break;
                case PLAYER:
                    final Player p = clientRealm.getPlayer(textEffect.getTargetEntityId());
                    if (p == null) {
                        ClientGameLogic.log.warn("[CLIENT] Player with id {} was not found for targeted TextEffect",
                                textEffect.getTargetEntityId());
                        return;
                    }
                    targetPos = p.getPos();
                    break;
                default:
                    break;
                }

                final EffectText hitText = EffectText.builder().damage(textEffect.getText())
                        .effect(TextEffect.from(textEffect.getTextEffectId())).sourcePos(targetPos).build();
                cli.getState().getDamageText().add(hitText);
            } catch (Exception e) {
                ClientGameLogic.log.error("[CLIENT] Failed to create client TextEffect. Reason: {}", e);
            }

        } catch (Exception e) {
            ClientGameLogic.log.error("[CLIENT] Failed to handle TextEffect Packet. Reason: {}", e);
        }
    }

    public static void handleLoadMapClient(RealmManagerClient cli, Packet packet) {
        final LoadMapPacket loadPacket = (LoadMapPacket) packet;
        try {
            cli.getState().getPui().getMinimap().initializeMap((int) loadPacket.getMapId());
            cli.getRealm().setRealmId(loadPacket.getRealmId());
            cli.getRealm().setMapId(loadPacket.getMapId());
            cli.getRealm().getTileManager().mergeMap(loadPacket);
        } catch (Exception e) {
            ClientGameLogic.log.error("[CLIENT] Failed to handle LoadMap Packet. Reason: {}", e);
        }
    }

    public static void handleLoadClient(RealmManagerClient cli, Packet packet) {
        final LoadPacket loadPacket = (LoadPacket) packet;
        try {
            for (final Player p : loadPacket.getPlayers()) {
                if (p.getId() == cli.getCurrentPlayerId()) {
                    continue;
                }
                cli.getRealm().addPlayerIfNotExists(p);
            }
            for (final LootContainer lc : loadPacket.getContainers()) {
                if (lc.getContentsChanged()) {
                    LootContainer current = cli.getRealm().getLoot().get(lc.getLootContainerId());
                    current.setContentsChanged(true);
                    current.setItemsUncondensed(LootContainer.getCondensedItems(lc));
                } else {
                    cli.getRealm().addLootContainerIfNotExists(lc);
                }
            }

            for (final Bullet b : loadPacket.getBullets()) {
                cli.getRealm().addBulletIfNotExists(b);
            }

            for (final Enemy e : loadPacket.getEnemies()) {
                cli.getRealm().addEnemyIfNotExists(e);
            }

            for (final Portal p : loadPacket.getPortals()) {
                cli.getRealm().addPortalIfNotExists(p);
            }
        } catch (Exception e) {
            ClientGameLogic.log.error("[CLIENT] Failed to handle Load Packet. Reason: {}", e);
        }
    }

    public static void handleUnloadClient(RealmManagerClient cli, Packet packet) {
        final UnloadPacket unloadPacket = (UnloadPacket) packet;
        try {
            for (final Long p : unloadPacket.getPlayers()) {
                if (p == cli.getCurrentPlayerId()) {
                    continue;
                }
                final Player removed = cli.getRealm().getPlayers().remove(p);
                if (removed == null) {
                    ClientGameLogic.log.error("[CLIENT] Player {} does not exist", p);
                }
            }
            for (final Long lc : unloadPacket.getContainers()) {
                final LootContainer removed = cli.getRealm().getLoot().remove(lc);
                if (removed == null) {
                    ClientGameLogic.log.error("[CLIENT] LootContainer {} does not exist", lc);
                }
            }
            for (final Long b : unloadPacket.getBullets()) {
                final Bullet removed = cli.getRealm().getBullets().remove(b);
                if (removed == null) {
                    ClientGameLogic.log.error("[CLIENT] Bullet {} does not exist", b);
                }
            }
            for (final Long e : unloadPacket.getEnemies()) {
                final Enemy removed = cli.getRealm().getEnemies().remove(e);
                if (removed == null) {
                    ClientGameLogic.log.error("[CLIENT] Enemy {} does not exist", e);
                }
            }
            for (final Long p : unloadPacket.getPortals()) {
                final Portal removed = cli.getRealm().getPortals().remove(p);
                if (removed == null) {
                    ClientGameLogic.log.error("[CLIENT] Portal {} does not exist", p);
                }
            }

        } catch (Exception e) {
            ClientGameLogic.log.error("[CLIENT] Failed to handle Unload Packet. Reason: {}", e);
        }
    }

    public static void handleTextClient(RealmManagerClient cli, Packet packet) {
        final TextPacket textPacket = (TextPacket) packet;
        ClientGameLogic.log.info("[CLIENT] Recieved Text Packet \nTO: {}\nFROM: {}\nMESSAGE: {}", textPacket.getTo(),
                textPacket.getFrom(), textPacket.getMessage());
        try {
            cli.getState().getPui().enqueueChat(textPacket.clone());
        } catch (Exception e) {
            ClientGameLogic.log.error("[CLIENT] Failed to response to initial text packet. Reason: {}", e.getMessage());
        }
    }

    public static void handleCommandClient(RealmManagerClient cli, Packet packet) {
        final CommandPacket commandPacket = (CommandPacket) packet;
        ClientGameLogic.log.info("[CLIENT] Recieved Command Packet for Player {} Command={}",
                commandPacket.getPlayerId(), commandPacket.getCommand());
        try {
            switch (commandPacket.getCommandId()) {
            case 2:
                final LoginResponseMessage loginResponse = CommandType.fromPacket(commandPacket);
                ClientGameLogic.doLoginResponse(cli, loginResponse);
                break;
            case 4:
                final ServerErrorMessage serverError = CommandType.fromPacket(commandPacket);
                ClientGameLogic.handleServerError(cli, serverError);
                break;
            case 5:
                final PlayerAccountMessage playerAccount = CommandType.fromPacket(commandPacket);
                cli.getState().setAccount(playerAccount.getAccount());
                //cli.getState().getGameStateManager().add(GameStateManager.PAUSE);
                break;
                
            }
        } catch (Exception e) {
            ClientGameLogic.log.error("[CLIENT] Failed to handle client command packet. Reason: {}", e.getMessage());
        }
    }

    public static void handleObjectMoveClient(RealmManagerClient cli, Packet packet) {
        final ObjectMovePacket objectMovePacket = (ObjectMovePacket) packet;
        for (ObjectMovement movement : objectMovePacket.getMovements()) {
            final EntityType type = movement.getTargetEntityType();
            if (type == null) {
                continue;
            }
            switch (type) {
            case PLAYER:
                final Player playerToUpdate = cli.getRealm().getPlayer(movement.getEntityId());
                if (playerToUpdate == null) {
                    break;
                }
                if (cli.getCurrentPlayerId() == movement.getEntityId()) {
                    playerToUpdate.applyMovementLerp(movement, 1.0f);
                } else {
                    playerToUpdate.applyMovementLerp(movement, 0.55f);
                }
                break;
            case ENEMY:
                final Enemy enemyToUpdate = cli.getRealm().getEnemy(movement.getEntityId());
                if (enemyToUpdate == null) {
                    break;
                }
                enemyToUpdate.applyMovementLerp(movement);
                break;
            case BULLET:
                final Bullet bulletToUpdate = cli.getRealm().getBullet(movement.getEntityId());
                if (bulletToUpdate == null) {
                    break;
                }
                bulletToUpdate.applyMovementLerp(movement);
                break;
            default:
                break;
            }
        }
    }

    public static void handleUpdateClient(RealmManagerClient cli, Packet packet) {
        UpdatePacket updatePacket = (UpdatePacket) packet;
        Player toUpdate = cli.getRealm().getPlayer((updatePacket.getPlayerId()));
        if (toUpdate == null)
            return;
        toUpdate.applyUpdate(updatePacket, cli.getState());
    }

    private static void handleServerError(RealmManagerClient cli, ServerErrorMessage message) {
        ClientGameLogic.log.error("[CLIENT] Recieved Server Error ***{}", message);
        cli.getState().getPui().enqueueChat(TextPacket.create("SYSTEM", "", message.toString()));
    }

    private static void doLoginResponse(RealmManagerClient cli, LoginResponseMessage loginResponse) {
        try {
            if (loginResponse.isSuccess()) {
                CharacterClass cls = CharacterClass.valueOf(loginResponse.getClassId());
                Player player = new Player(loginResponse.getPlayerId(),
                        new Vector2f(loginResponse.getSpawnX(), loginResponse.getSpawnY()), GlobalConstants.PLAYER_SIZE,
                        cls);
                ClientGameLogic.log.info("[CLIENT] Login succesful, added Player ID {}", player.getId());
                player.setSpriteSheet(GameSpriteManager.loadClassSprites(cls));
                ClientGameLogic.DATA_SERVICE.setSessionToken(loginResponse.getToken());
                cli.getState().setAccount(loginResponse.getAccount());
                cli.getState().loadClass(player, cls, true);
                cli.setCurrentPlayerId(player.getId());
                cli.getState().setPlayerId(player.getId());
                cli.startHeartbeatThread();
                TextPacket packet = TextPacket.create("SYSTEM", "Player",
                        "Welcome to JRealm " + GameLauncher.GAME_VERSION + "!");
                cli.getState().getPui().enqueueChat(packet);
            }
        } catch (Exception e) {
            ClientGameLogic.log.error("[CLIENT] Failed to response to login response. Reason: {}", e.getMessage());
        }
    }
}
