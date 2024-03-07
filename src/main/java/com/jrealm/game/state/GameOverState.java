package com.jrealm.game.state;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jrealm.game.GamePanel;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.ui.Button;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

public class GameOverState extends GameState {

	private BufferedImage imgButton;
	private BufferedImage imgHover;
	private Button btnReset;
	private Button btnQuit;
	private Font font;

	public GameOverState(GameStateManager gsm) {
		super(gsm);

		this.imgButton = GameStateManager.button.cropImage(0, 0, 121, 26);
		this.imgHover = GameStateManager.button.cropImage(0, 29, 122, 28);

		this.font = new Font("MeatMadness", Font.PLAIN, 48);
		this.btnReset = new Button("RESTART", this.imgButton, this.font, new Vector2f(GamePanel.width / 2, (GamePanel.height / 2) - 48), 32, 16);
		this.btnQuit = new Button("QUIT", this.imgButton, this.font, new Vector2f(GamePanel.width / 2, (GamePanel.height / 2) + 48), 32, 16);

		this.btnReset.addHoverImage(this.btnReset.createButton("RESTART", this.imgHover, this.font, this.btnReset.getWidth(), this.btnReset.getHeight(), 32, 20));
		this.btnQuit.addHoverImage(this.btnQuit.createButton("QUIT", this.imgHover, this.font, this.btnQuit.getWidth(), this.btnQuit.getHeight(), 32, 20));

		this.btnReset.onMouseDown(e -> {
			gsm.pop(GameStateManager.GAMEOVER);
			gsm.add(GameStateManager.PLAY);
		});

		this.btnQuit.onMouseDown(e -> {
			System.exit(0);
		});
	}

	@Override
	public void update(double time) {

	}

	@Override
	public void input(MouseHandler mouse, KeyHandler key) {
		key.escape.tick();

		this.btnReset.input(mouse, key);
		this.btnQuit.input(mouse, key);

		if (key.escape.clicked) {
			System.exit(0);
		}
	}

	@Override
	public void render(Graphics2D g) {
		// SpriteSheet.drawArray(g, gameover, new Vector2f(GamePanel.width / 2 -
		// gameover.length() * (32 / 2), GamePanel.height / 2 - 32 / 2), 32, 32, 32);
		this.btnReset.render(g);
		this.btnQuit.render(g);
	}
}
