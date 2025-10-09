package com.jrealm.game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import com.jrealm.game.graphics.ImageUtils;
import com.jrealm.game.state.GameStateManager;
import com.jrealm.util.KeyHandler;
import com.jrealm.util.MouseHandler;
import com.jrealm.util.TimedWorkerThread;
import com.jrealm.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class GamePanel extends JPanel implements Runnable {

	public static final long serialVersionUID = 1L;

	public static int width = 1920;
	public static int height = 1080;

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
		ImageUtils.applyQualityRenderingHints(this.g);
		RenderingHints rh = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		this.g.setRenderingHints(rh);
	}

	public void init() {
		this.running = true;
		this.initGraphics();
		this.mouse = new MouseHandler(this);
		this.key = new KeyHandler(this);
		this.gsm = new GameStateManager(this.g);
	}

	@SuppressWarnings("unused")
	@Override
	public void run() {
		this.init();

		final Runnable input = () -> {
			while (this.running) {
				this.input(this.mouse, this.key);
			}
		};

		final Runnable renderAndDraw = () -> {
			while (this.running) {
				this.render();
				this.draw();
			}
		};

		final TimedWorkerThread timedThread0 = new TimedWorkerThread(input, 64.0);
		final TimedWorkerThread timedThread1 = new TimedWorkerThread(renderAndDraw, 128.0);
		WorkerThread.submitAndForkRun(timedThread0, timedThread1);

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
		} while (this.bs.contentsLost());
	}
}
