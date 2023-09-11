package com.jrealm.game.util;


import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.jrealm.game.GamePanel;

public class MouseHandler implements MouseListener, MouseMotionListener {

	private static volatile int mouseX = -1;
	private static volatile int mouseY = -1;
	private static volatile int mouseB = -1;

	public MouseHandler(GamePanel game) {
		game.addMouseListener(this);
		game.addMouseMotionListener(this);
	}

	public int getX() {
		return MouseHandler.mouseX;
	}

	public int getY() {
		return MouseHandler.mouseY;
	}

	public int getButton() {
		return MouseHandler.mouseB;
	}


	@Override
	public void mouseClicked(MouseEvent e) {
		MouseHandler.mouseB = -1;

	}

	@Override
	public void mousePressed(MouseEvent e) {
		MouseHandler.mouseB = 1;

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		MouseHandler.mouseB = -1;
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		MouseHandler.mouseX = e.getX();
		MouseHandler.mouseY = e.getY();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		MouseHandler.mouseX = e.getX();
		MouseHandler.mouseY = e.getY();
	}
}
