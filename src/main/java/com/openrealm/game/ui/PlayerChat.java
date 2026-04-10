package com.openrealm.game.ui;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.openrealm.game.OpenRealmGame;
import com.openrealm.game.state.PlayState;
import com.openrealm.net.client.SocketClient;
import com.openrealm.net.messaging.CommandType;
import com.openrealm.net.messaging.ServerCommandMessage;
import com.openrealm.net.server.packet.CommandPacket;
import com.openrealm.net.server.packet.TextPacket;
import com.openrealm.util.KeyHandler;
import com.openrealm.util.MouseHandler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PlayerChat {
    private static final int CHAT_SIZE = 15;
    private Map<String, TextPacket> playerChat;
    private String currentMessage;
    private boolean chatOpen;
    private boolean releasedEnter;
    private boolean pressedEnter;
    private PlayState state;

    public PlayerChat(PlayState state) {
        this.currentMessage = "";
        this.chatOpen = false;
        this.releasedEnter = false;
        this.pressedEnter = false;
        this.state = state;
        this.playerChat = new LinkedHashMap<String, TextPacket>() {
            private static final long serialVersionUID = 4568387673008726309L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, TextPacket> eldest) {
                return this.size() > PlayerChat.CHAT_SIZE;
            }
        };
    }

    public void addChatMessage(final TextPacket packet) {
        String message = "[{0}]: {1}";
        message = MessageFormat.format(message, packet.getFrom(), packet.getMessage());
        this.playerChat.put(message, packet);
    }

    public void input(MouseHandler mouse, KeyHandler key, SocketClient client) {
        if (key.captureMode) {
            this.currentMessage = key.getContent();
        }

        if (key.enter.down && !this.pressedEnter) {
            this.pressedEnter = true;
        }

        if (this.pressedEnter && this.releasedEnter) {
            this.chatOpen = !this.chatOpen;
            key.setCaptureMode(this.chatOpen);
            this.pressedEnter = false;
            this.releasedEnter = false;
            if (!this.chatOpen && !key.getContent().isBlank()) {
                try {
                    String messageToSend = key.getCapturedInput();
                    messageToSend = messageToSend.replace("\n", "").replace("\r", "").trim();
                    if (messageToSend.startsWith("/")) {
                        if (messageToSend.equalsIgnoreCase("/debug")) {
                            this.state.setDebugMode(!this.state.isDebugMode());
                            String status = this.state.isDebugMode() ? "ON" : "OFF";
                            TextPacket debugMsg = TextPacket.create("SYSTEM", "SYSTEM", "Debug mode: " + status);
                            this.addChatMessage(debugMsg);
                        } else if (messageToSend.equalsIgnoreCase("/clear")) {
                            this.playerChat = new LinkedHashMap<String, TextPacket>() {
                                private static final long serialVersionUID = 4568387673008726309L;

                                @Override
                                protected boolean removeEldestEntry(Map.Entry<String, TextPacket> eldest) {
                                    return this.size() > PlayerChat.CHAT_SIZE;
                                }
                            };
                        } else {
                            ServerCommandMessage serverCommand = ServerCommandMessage.parseFromInput(messageToSend);
                            CommandPacket packet = CommandPacket.create(this.state.getPlayer(), CommandType.SERVER_COMMAND,
                                    serverCommand);
                            client.sendRemote(packet);
                        }
                    } else {
                        TextPacket packet = TextPacket.create(this.state.getPlayer().getName(), "SYSTEM",
                                messageToSend);
                        client.sendRemote(packet);
                    }
                } catch (Exception e) {
                    PlayerChat.log.error("Failed to send PlayerChat to server. Reason: {}", e);
                }
            }
        }

        if (this.pressedEnter && !key.enter.down) {
            this.releasedEnter = true;
            return;
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        // Use unscaled font for chat text
        float originalScale = font.getData().scaleX;
        font.getData().setScale(1.0f);

        float lineHeight = 14f;
        font.setColor(Color.WHITE);

        int index = PlayerChat.CHAT_SIZE;
        for (Map.Entry<String, TextPacket> packet : this.playerChat.entrySet()) {
            float y = OpenRealmGame.height - (index * lineHeight) - 100;
            font.draw(batch, packet.getKey(), 8, y);
            index--;
        }

        if (this.chatOpen) {
            // Draw dark semi-transparent background behind chat input
            float inputY = OpenRealmGame.height - lineHeight - 4;
            float inputHeight = lineHeight + 8;
            float inputWidth = OpenRealmGame.width / 2f;
            batch.end();
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(new Color(0f, 0f, 0f, 0.6f));
            shapes.rect(4, inputY, inputWidth, inputHeight);
            shapes.end();
            batch.begin();

            font.setColor(Color.WHITE);
            font.draw(batch, "> " + this.currentMessage + "_", 8, OpenRealmGame.height - lineHeight);
        }

        // Restore original scale
        font.getData().setScale(originalScale);
    }
}
