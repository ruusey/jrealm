package com.jrealm.game.util;


import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.jrealm.game.GamePanel;

public class MouseHandler implements MouseListener, MouseMotionListener {

	private static volatile int mouseX = -1;
	private static volatile int mouseY = -1;

	private static volatile int[] mouseButtonStates = new int[] { -1, -1, -1 };

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


	public boolean isPressed(int mouseButton) {
		if ((mouseButton - 1) < 0)
			return false;
		return MouseHandler.mouseButtonStates[mouseButton-1]>-1;
	}


	@Override
	public void mouseClicked(MouseEvent e) {
		//		if (e.getButton() == MouseEvent.BUTTON1) {
		//			MouseHandler.mouseButtonStates[0] = 1;
		//		}
		//		if (e.getButton() == MouseEvent.BUTTON2) {
		//			MouseHandler.mouseButtonStates[1] = 1;
		//		}
		//		if (e.getButton() == MouseEvent.BUTTON3) {
		//			MouseHandler.mouseButtonStates[2] = 1;
		//		}

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			MouseHandler.mouseButtonStates[0] = 1;
		}
		if (e.getButton() == MouseEvent.BUTTON2) {
			MouseHandler.mouseButtonStates[1] = 1;
		}
		if (e.getButton() == MouseEvent.BUTTON3) {
			MouseHandler.mouseButtonStates[2] = 1;
		}

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			MouseHandler.mouseButtonStates[0] = -1;
		}
		if (e.getButton() == MouseEvent.BUTTON2) {
			MouseHandler.mouseButtonStates[1] = -1;
		}
		if (e.getButton() == MouseEvent.BUTTON3) {
			MouseHandler.mouseButtonStates[2] = -1;
		}
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
