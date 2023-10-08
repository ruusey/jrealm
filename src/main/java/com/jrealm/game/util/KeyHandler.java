package com.jrealm.game.util;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.GamePanel;

public class KeyHandler implements KeyListener{

	public static List<Key> keys = new ArrayList<Key>();

	public static class Key {
		public int presses, absorbs;
		public boolean down, clicked;

		public Key() {
			KeyHandler.keys.add(this);
		}

		public void toggle(boolean pressed) {
			if(pressed != this.down) {
				this.down = pressed;
			}
			if(pressed) {
				this.presses++;
			}
		}

		public void tick() {
			if(this.absorbs < this.presses) {
				this.absorbs++;
				this.clicked = true;
			} else {
				this.clicked = false;
			}
		}
	}

	public Key up = new Key();
	public Key down = new Key();
	public Key left = new Key();
	public Key right = new Key();
	public Key attack = new Key();
	public Key menu = new Key();
	public Key enter = new Key();
	public Key escape = new Key();
	public Key shift = new Key();
	public Key f1 = new Key();
	public Key f2 = new Key();

	public Key one = new Key();
	public Key two = new Key();
	public Key three = new Key();
	public Key four = new Key();
	public Key five = new Key();
	public Key six = new Key();
	public Key zero = new Key();

	public Key q = new Key();

	public KeyHandler(GamePanel game) {
		game.addKeyListener(this);
	}

	public void releaseAll() {
		for(int i = 0; i < KeyHandler.keys.size(); i++) {
			KeyHandler.keys.get(i).down = false;
		}
	}

	public void tick() {
		for(int i = 0; i < KeyHandler.keys.size(); i++) {
			KeyHandler.keys.get(i).tick();
		}
	}

	public void toggle(KeyEvent e, boolean pressed) {
		if(e.getKeyCode() == KeyEvent.VK_W) {
			this.up.toggle(pressed);
		}
		if(e.getKeyCode() == KeyEvent.VK_S) {
			this.down.toggle(pressed);
		}
		if(e.getKeyCode() == KeyEvent.VK_A) {
			this.left.toggle(pressed);
		}
		if(e.getKeyCode() == KeyEvent.VK_D) {
			this.right.toggle(pressed);
		}
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			this.attack.toggle(pressed);
		}
		if(e.getKeyCode() == KeyEvent.VK_E) {
			this.menu.toggle(pressed);
		}
		if(e.getKeyCode() == KeyEvent.VK_ENTER) {
			this.enter.toggle(pressed);
		}
		if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			this.escape.toggle(pressed);
		}
		if(e.getKeyCode() == KeyEvent.VK_F1) {
			this.f1.toggle(pressed);
		}
		if (e.getKeyCode() == KeyEvent.VK_F2) {
			this.f2.toggle(pressed);
		}
		if(e.getKeyCode() == KeyEvent.VK_SHIFT) {
			this.shift.toggle(pressed);
		}

		if (e.getKeyCode() == KeyEvent.VK_1) {
			this.one.toggle(pressed);
		}
		if (e.getKeyCode() == KeyEvent.VK_2) {
			this.two.toggle(pressed);
		}
		if (e.getKeyCode() == KeyEvent.VK_3) {
			this.three.toggle(pressed);
		}
		if (e.getKeyCode() == KeyEvent.VK_4) {
			this.four.toggle(pressed);
		}
		if (e.getKeyCode() == KeyEvent.VK_5) {
			this.five.toggle(pressed);
		}
		if (e.getKeyCode() == KeyEvent.VK_6) {
			this.six.toggle(pressed);
		}
		if (e.getKeyCode() == KeyEvent.VK_0) {
			this.zero.toggle(pressed);
		}

		if (e.getKeyCode() == KeyEvent.VK_Q) {
			this.q.toggle(pressed);
		}
	}


	@Override
	public void keyTyped(KeyEvent e) {
		// do nothing
	}

	@Override
	public void keyPressed(KeyEvent e) {
		this.toggle(e, true);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		this.toggle(e, false);
	}
}
