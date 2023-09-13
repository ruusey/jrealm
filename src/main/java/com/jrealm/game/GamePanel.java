package com.jrealm.game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.states.GameStateManager;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;
import com.jrealm.game.util.WorkerThread;


public class GamePanel extends JPanel implements Runnable {

	public static final long serialVersionUID = 1L;

	public static int width;
	public static int height;
	public static int oldFrameCount;
	public static int oldTickCount;
	public static int tickCount;

	private Thread thread;
	private boolean running = false;

	private BufferStrategy bs;
	private BufferedImage img;
	private Graphics2D g;

	private MouseHandler mouse;
	private KeyHandler key;

	private GameStateManager gsm;

	public GamePanel(BufferStrategy bs, int width, int height) {
		GamePanel.width = width;
		GamePanel.height = height;
		this.bs = bs;
		this.setPreferredSize(new Dimension(width, height));
		this.setFocusable(true);
		this.requestFocus();
	}

	@Override
	public void addNotify() {
		super.addNotify();

		if (this.thread == null) {
			this.thread = new Thread(this, "GameThread");
			this.thread.start();
		}
	}

	public void initGraphics() {
		this.img = new BufferedImage(GamePanel.width, GamePanel.height, BufferedImage.TYPE_INT_ARGB);
		this.g = (Graphics2D) this.img.getGraphics();
		this.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	public void init() {
		this.running = true;
		GameDataManager.loadGameData();
		this.initGraphics();

		this.mouse = new MouseHandler(this);
		this.key = new KeyHandler(this);

		this.gsm = new GameStateManager(this.g);
	}

	@Override
	public void run() {
		this.init();

		final double GAME_HERTZ = 64.0;
		final double TBU = 1000000000 / GAME_HERTZ; // Time Before Update

		final int MUBR = 3; // Must Update before render

		double lastUpdateTime = System.nanoTime();
		double lastRenderTime;

		final double TARGET_FPS = 1000;
		final double TTBR = 1000000000 / TARGET_FPS; // Total time before render

		int frameCount = 0;
		int lastSecondTime = (int) (lastUpdateTime / 1000000000);
		GamePanel.oldFrameCount = 0;

		GamePanel.tickCount = 0;
		GamePanel.oldTickCount = 0;

		while (this.running) {

			double now = System.nanoTime();
			int updateCount = 0;
			while (((now - lastUpdateTime) > TBU) && (updateCount < MUBR)) {
				this.update(now);
				this.input(this.mouse, this.key);
				lastUpdateTime += TBU;
				updateCount++;
				GamePanel.tickCount++;
				// (^^^^) We use this varible for the soul purpose of displaying it
			}

			if ((now - lastUpdateTime) > TBU) {
				lastUpdateTime = now - TBU;
			}
			Runnable input = ()->{
				this.input(this.mouse, this.key);
			};

			Runnable renderAndDraw = () -> {
				this.render();
				this.draw();
			};

			WorkerThread.submitAndRun(input, renderAndDraw);
			lastRenderTime = now;
			frameCount++;

			int thisSecond = (int) (lastUpdateTime / 1000000000);
			if (thisSecond > lastSecondTime) {
				if (frameCount != GamePanel.oldFrameCount) {
					System.out.println("NEW SECOND " + thisSecond + " " + frameCount);
					GamePanel.oldFrameCount = frameCount;
				}

				if (GamePanel.tickCount != GamePanel.oldTickCount) {
					// System.out.println("NEW SECOND (T) " + thisSecond + " " +
					// GamePanel.tickCount);
					GamePanel.oldTickCount = GamePanel.tickCount;
				}
				GamePanel.tickCount = 0;
				frameCount = 0;
				lastSecondTime = thisSecond;
			}

			while (((now - lastRenderTime) < TTBR) && ((now - lastUpdateTime) < TBU)) {
				Thread.yield();

				try {
					Thread.sleep(1);
				} catch (Exception e) {
					System.out.println("ERROR: yielding thread");
				}

				now = System.nanoTime();
			}

		}
	}

	public void update(double time) {
		this.gsm.update(time);
	}

	public void input(MouseHandler mouse, KeyHandler key) {
		this.gsm.input(mouse, key);
	}

	public void render() {
		if (this.g != null) {
			this.g.setColor(new Color(33, 30, 39));
			this.g.fillRect(0, 0, GamePanel.width, GamePanel.height);
			this.gsm.render(this.g);
		}
	}

	public void draw() {
		do {
			Graphics g2 = (Graphics) this.bs.getDrawGraphics();
			g2.drawImage(this.img, 3, 26, GamePanel.width + 10, GamePanel.height + 10, null); // true 8, 31
			g2.dispose();
			this.bs.show();
		} while(this.bs.contentsLost());

	}

}
