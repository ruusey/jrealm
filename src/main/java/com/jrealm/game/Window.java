package com.jrealm.game;

import java.awt.image.BufferStrategy;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class Window extends JFrame {
	public static final long serialVersionUID = 1L;

	private BufferStrategy bs;
	private GamePanel gp;

	public Window() {
		this.setTitle("JRealm");
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setIgnoreRepaint(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setVisible(true);
	}

	@Override
	public void addNotify() {
		super.addNotify();

		this.createBufferStrategy(2);
		this.bs = this.getBufferStrategy();

		this.gp = new GamePanel(this.bs, 1280, 720);
		//add(gp);
		this.setContentPane(this.gp);

	}

}
