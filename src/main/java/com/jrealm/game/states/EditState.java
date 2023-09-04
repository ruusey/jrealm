package com.jrealm.game.states;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jrealm.game.GamePanel;
import com.jrealm.game.entity.GameObject;
import com.jrealm.game.entity.enemy.TinyMon;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.ui.Button;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

public class EditState extends GameState {

	private BufferedImage imgButton;
	private Button btnEnemy1;
	private Button btnEnemy2;
	private boolean clicked = false;

	private GameObject gameObject = null;
	private PlayState ps;
	private Camera cam;

	private int selection = 0;
	private GameObject e_enemy1;
	private GameObject e_enemy2;
	private GameObject[] entityList = {this.gameObject, this.e_enemy1, this.e_enemy2};
	private int count = 0;


	public EditState(GameStateManager gsm, Camera cam) {
		super(gsm);
		this.imgButton = GameStateManager.ui.getSprite(0, 0, 128, 64).image;
		this.ps = (PlayState) gsm.getState(GameStateManager.PLAY);
		this.cam = cam;

		SpriteSheet enemySheet = new SpriteSheet("entity/enemy/minimonsters.png", 16, 16);

		this.btnEnemy1 = new Button("TinyMon", new Vector2f(64 + 24, 64 + 24), 32, 24, this.imgButton, new Vector2f(64, 64), 220, 75);
		this.btnEnemy1.addEvent(e -> {
			this.selection = 1;
			this.entityList[1] = new TinyMon(cam, new SpriteSheet(enemySheet.getSprite(0, 0, 128, 32), "tiny monster", 16, 16),
					new Vector2f(((GamePanel.width / 2) - 32) + 150, ((0 + (GamePanel.height / 2)) - 32) + 150), 64);
		});

		this.btnEnemy2 = new Button("TinyBoar", new Vector2f(64 + 24, (64 + 24) * 2), 32, 24, this.imgButton, new Vector2f(64, 64 + 85), 235, 75);
		this.btnEnemy2.addEvent(e -> {
			this.selection = 2;
			this.entityList[2] = new TinyMon(cam, new SpriteSheet(enemySheet.getSprite(0, 1, 128, 32), "tiny boar", 16, 16),
					new Vector2f(((GamePanel.width / 2) - 32) + 150, ((0 + (GamePanel.height / 2)) - 32) + 150), 64);
		});
	}

	@Override
	public void update(double time) {

	}

	@Override
	public void input(MouseHandler mouse, KeyHandler key) {
		this.btnEnemy1.input(mouse, key);
		this.btnEnemy2.input(mouse, key);

		if((mouse.getButton() == 1) && !this.clicked && (this.entityList[this.selection] != null) && !this.btnEnemy1.getHovering() && !this.btnEnemy2.getHovering()) {
			GameObject go = this.entityList[this.selection];
			go.setPos(new Vector2f((mouse.getX() - (go.getSize() / 2)) + this.cam.getPos().x + 64,
					(mouse.getY() - (go.getSize() / 2)) + this.cam.getPos().y + 64));

			if(!this.ps.getGameObjects().contains(go)) {
				this.count++;
				go.setName("enemy: " + Integer.toString(this.selection) + " count: " + Integer.toString(this.count));
				this.ps.getGameObjects().add(go.getBounds().distance(this.ps.getPlayerPos()), go);
				this.ps.getAABBObjects().insert(go);
			}

			this.clicked = true;
		} else if(mouse.getButton() == -1) {
			this.clicked = false;
		}
	}

	@Override
	public void render(Graphics2D g) {
		this.btnEnemy1.render(g);
		this.btnEnemy2.render(g);
	}
}