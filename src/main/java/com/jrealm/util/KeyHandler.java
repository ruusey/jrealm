package com.jrealm.util;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

import lombok.Data;

@Data
public class KeyHandler implements InputProcessor {
    public boolean captureMode = false;
    public String content = "";
    public static List<Key> keys = new ArrayList<Key>();

    public static class Key {
        public int presses, absorbs;
        public boolean down, clicked;

        public Key() {
            KeyHandler.keys.add(this);
        }

        public void toggle(boolean pressed) {
            if (pressed != this.down) {
                this.down = pressed;
            }
            if (pressed) {
                this.presses++;
            }
        }

        public void tick() {
            if (this.absorbs < this.presses) {
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
    public Key seven = new Key();
    public Key eight = new Key();
    public Key zero = new Key();

    public Key q = new Key();
    public Key t = new Key();
    public Key m = new Key();
    public Key plus = new Key();
    public Key minus = new Key();

    public KeyHandler() {
        // No listener registration needed - we poll Gdx.input
    }

    public void releaseAll() {
        for (int i = 0; i < KeyHandler.keys.size(); i++) {
            KeyHandler.keys.get(i).down = false;
        }
    }

    public void tick() {
        for (int i = 0; i < KeyHandler.keys.size(); i++) {
            KeyHandler.keys.get(i).tick();
        }
    }

    public void update() {
        if (this.captureMode) {
            // In capture mode, handle text input differently
            // Check for backspace
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && this.content.length() > 0) {
                this.content = this.content.substring(0, this.content.length() - 1);
            }
            // Release all movement keys to prevent stuck movement when chat opens
            this.up.toggle(false);
            this.down.toggle(false);
            this.left.toggle(false);
            this.right.toggle(false);
            this.attack.toggle(false);
            // Enter key still toggles for the game logic
            this.enter.toggle(Gdx.input.isKeyPressed(Input.Keys.ENTER));
            return;
        }

        this.up.toggle(Gdx.input.isKeyPressed(Input.Keys.W));
        this.down.toggle(Gdx.input.isKeyPressed(Input.Keys.S));
        this.left.toggle(Gdx.input.isKeyPressed(Input.Keys.A));
        this.right.toggle(Gdx.input.isKeyPressed(Input.Keys.D));
        this.attack.toggle(Gdx.input.isKeyPressed(Input.Keys.SPACE));
        this.menu.toggle(Gdx.input.isKeyPressed(Input.Keys.E));
        this.enter.toggle(Gdx.input.isKeyPressed(Input.Keys.ENTER));
        this.escape.toggle(Gdx.input.isKeyPressed(Input.Keys.ESCAPE));
        this.f1.toggle(Gdx.input.isKeyPressed(Input.Keys.F1));
        this.f2.toggle(Gdx.input.isKeyPressed(Input.Keys.F2));
        this.shift.toggle(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));

        this.one.toggle(Gdx.input.isKeyPressed(Input.Keys.NUM_1));
        this.two.toggle(Gdx.input.isKeyPressed(Input.Keys.NUM_2));
        this.three.toggle(Gdx.input.isKeyPressed(Input.Keys.NUM_3));
        this.four.toggle(Gdx.input.isKeyPressed(Input.Keys.NUM_4));
        this.five.toggle(Gdx.input.isKeyPressed(Input.Keys.NUM_5));
        this.six.toggle(Gdx.input.isKeyPressed(Input.Keys.NUM_6));
        this.seven.toggle(Gdx.input.isKeyPressed(Input.Keys.NUM_7));
        this.eight.toggle(Gdx.input.isKeyPressed(Input.Keys.NUM_8));
        this.zero.toggle(Gdx.input.isKeyPressed(Input.Keys.NUM_0));

        this.q.toggle(Gdx.input.isKeyPressed(Input.Keys.Q));
        this.t.toggle(Gdx.input.isKeyPressed(Input.Keys.T));
        this.m.toggle(Gdx.input.isKeyPressed(Input.Keys.M));
        this.plus.toggle(Gdx.input.isKeyPressed(Input.Keys.PLUS) || Gdx.input.isKeyPressed(Input.Keys.EQUALS));
        this.minus.toggle(Gdx.input.isKeyPressed(Input.Keys.MINUS));
    }

    public void captureInput() {
        this.captureMode = true;
    }

    public String getCapturedInput() {
        String content = new String(this.content);
        this.content = "";
        this.captureMode = false;
        return content;
    }

    /**
     * Called by LibGDX InputProcessor when in capture mode.
     */
    public void appendChar(char c) {
        if (this.captureMode && c != '\n' && c != '\r' && c != '\b') {
            this.content += c;
        }
    }

    @Override
    public boolean keyTyped(char character) {
        this.appendChar(character);
        return this.captureMode;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
