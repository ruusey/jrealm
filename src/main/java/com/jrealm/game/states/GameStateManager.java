package com.jrealm.game.states;

import java.awt.Graphics2D;

import com.jrealm.game.GamePanel;
import com.jrealm.game.graphics.Font;
import com.jrealm.game.graphics.Fontf;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

public class GameStateManager {

	private GameState states[];

	public static Vector2f map;

	public static final int MENU = 0;
	public static final int PLAY = 1;
	public static final int PAUSE = 2;
	public static final int GAMEOVER = 3;
	public static final int EDIT = 4;

	public static Font font;
	public static Fontf fontf;
	public static SpriteSheet ui;
	public static SpriteSheet button;
	public static Camera cam;
	public static Graphics2D g;

	public GameStateManager(Graphics2D g) {
		GameStateManager.g = g;
		GameStateManager.map = new Vector2f(GamePanel.width, GamePanel.height);
		Vector2f.setWorldVar(GameStateManager.map.x, GameStateManager.map.y);

		this.states = new GameState[5];

		GameStateManager.font = new Font("font/font.png", 10, 10);
		GameStateManager.fontf = new Fontf();
		GameStateManager.fontf.loadFont("font/Stackedpixel.ttf", "MeatMadness");
		GameStateManager.fontf.loadFont("font/GravityBold8.ttf", "GravityBold8");
		SpriteSheet.currentFont = GameStateManager.font;

		GameStateManager.ui = new SpriteSheet("ui/ui.png", 64, 64, 0);
		GameStateManager.button = new SpriteSheet("ui/buttons.png", 122, 57, 0);


		GameStateManager.cam = new Camera(new AABB(new Vector2f(-64, -64), GamePanel.width + 128, GamePanel.height + 128));

		//this.states[GameStateManager.PLAY] = new PlayState(this, GameStateManager.cam);
	}

	public boolean isStateActive(int state) {
		return this.states[state] != null;
	}

	public GameState getState(int state) {
		return this.states[state];
	}

	public void pop(int state) {
		this.states[state] = null;
	}

	public void add(int state) {
		if (this.states[state] != null)
			return;

		switch (state) {
		case GameStateManager.PLAY:
			GameStateManager.cam = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
			this.states[GameStateManager.PLAY] = new PlayState(this, GameStateManager.cam);
			break;
		case GameStateManager.MENU:
			this.states[GameStateManager.MENU] = new MenuState(this);
			break;
		case GameStateManager.PAUSE:
			this.states[GameStateManager.PAUSE] = new PauseState(this);
			break;
		case GameStateManager.GAMEOVER:
			this.states[GameStateManager.GAMEOVER] = new GameOverState(this);
			break;
		case GameStateManager.EDIT:
			if(this.states[GameStateManager.PLAY] != null) {
				this.states[GameStateManager.EDIT] = new EditState(this, GameStateManager.cam);
			}
			break;
		default:
			break;
		}
	}

	public void addAndpop(int state) {
		this.addAndpop(state, 0);
	}

	public void addAndpop(int state, int remove) {
		this.pop(state);
		this.add(state);
	}

	public void update(double time) {
		for (int i = 0; i < this.states.length; i++) {
			if (this.states[i] != null) {
				this.states[i].update(time);
			}
		}
	}

	public void input(MouseHandler mouse, KeyHandler key) {

		for (int i = 0; i < this.states.length; i++) {
			if (this.states[i] != null) {
				this.states[i].input(mouse, key);
			}
		}
	}

	public void render(Graphics2D g) {
		g.setFont(GameStateManager.fontf.getFont("MeatMadness"));
		for (int i = 0; i < this.states.length; i++) {
			if (this.states[i] != null) {
				this.states[i].render(g);
			}
		}
	}

}
