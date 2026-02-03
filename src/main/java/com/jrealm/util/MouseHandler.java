package com.jrealm.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class MouseHandler {

    private static volatile int mouseX = -1;
    private static volatile int mouseY = -1;
    private static volatile int[] mouseButtonStates = new int[] { -1, -1, -1 };

    public MouseHandler() {
        // No listener registration needed - we poll Gdx.input
    }

    public void update() {
        MouseHandler.mouseX = Gdx.input.getX();
        MouseHandler.mouseY = Gdx.input.getY();

        // LibGDX button indices: LEFT=0, RIGHT=1, MIDDLE=2
        // Map to match old AWT convention: BUTTON1=left(index 0), BUTTON2=middle(index 1), BUTTON3=right(index 2)
        MouseHandler.mouseButtonStates[0] = Gdx.input.isButtonPressed(Input.Buttons.LEFT) ? 1 : -1;
        MouseHandler.mouseButtonStates[1] = Gdx.input.isButtonPressed(Input.Buttons.MIDDLE) ? 1 : -1;
        MouseHandler.mouseButtonStates[2] = Gdx.input.isButtonPressed(Input.Buttons.RIGHT) ? 1 : -1;
    }

    public int getX() {
        return MouseHandler.mouseX;
    }

    public int getY() {
        return MouseHandler.mouseY;
    }

    /**
     * @param mouseButton AWT-style button number: 1=LEFT, 2=MIDDLE, 3=RIGHT
     */
    public boolean isPressed(int mouseButton) {
        if ((mouseButton - 1) < 0)
            return false;
        return MouseHandler.mouseButtonStates[mouseButton - 1] > -1;
    }
}
