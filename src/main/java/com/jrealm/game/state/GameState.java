package com.jrealm.game.state;

import java.awt.Graphics2D;

import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

public abstract class GameState {

    public GameStateManager gsm;

    public GameState(GameStateManager gsm) {
	this.gsm = gsm;
    }

    public abstract void update(double time);

    public abstract void input(MouseHandler mouse, KeyHandler key);

    public abstract void render(Graphics2D g);
}
