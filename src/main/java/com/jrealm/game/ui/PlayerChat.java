package com.jrealm.game.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import com.jrealm.game.GamePanel;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;
import com.jrealm.net.client.SocketClient;
import com.jrealm.net.server.packet.TextPacket;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PlayerChat {
	private static final int CHAT_SIZE = 10;
	private Map<String, TextPacket> playerChat;
	private String currentMessage;
	private boolean chatOpen;
	private boolean releasedEnter;
	private boolean pressedEnter;
	public PlayerChat() {
		this.currentMessage = "";
		this.chatOpen = false;
		this.releasedEnter = false;
		this.pressedEnter = false;
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
		if(key.captureMode) {
			this.currentMessage = key.getContent();
		}

		if (key.enter.down && !this.pressedEnter) {
			this.pressedEnter = true;
			return;
		}

		if (this.pressedEnter && !key.enter.down) {
			this.releasedEnter = true;
			return;
		}

		if (this.pressedEnter && this.releasedEnter) {
			this.chatOpen = !this.chatOpen;
			key.setCaptureMode(this.chatOpen);
			this.pressedEnter = false;
			this.releasedEnter = false;
			if (!this.chatOpen && !key.getContent().isBlank()) {
				try {
					String messageToSend = key.getCapturedInput();
					messageToSend = messageToSend.replace("\n", "").replace("\r", "");
					TextPacket packet = TextPacket.create(SocketClient.PLAYER_USERNAME, "SYSTEM", messageToSend);
					client.sendRemote(packet);
				}catch(Exception e) {
					PlayerChat.log.error("Failed to send PlayerChat to server. Reason: {}", e);
				}
			}
		}
	}

	public void render(Graphics2D g) {
		g.setColor(Color.WHITE);
		java.awt.Font currentFont = g.getFont();
		float newSize = currentFont.getSize() * 0.50F;
		java.awt.Font newFont = currentFont.deriveFont(newSize);
		g.setFont(newFont);

		int index = PlayerChat.CHAT_SIZE;
		for (Map.Entry<String, TextPacket> packet : this.playerChat.entrySet()) {
			int y = (int) ((GamePanel.height - (index * newSize)) - 100);
			g.drawString(packet.getKey(), 8, y);
			index--;
		}


		if (this.chatOpen) {
			g.setColor(Color.WHITE);
			g.drawString(this.currentMessage, 8,  (int) (GamePanel.height - (newSize)));
			g.setColor(Color.GRAY);
			g.drawRect(8, (int) (GamePanel.height - (newSize * 2)), 600, (int) newSize);
		}
		g.setFont(currentFont);

	}
}
