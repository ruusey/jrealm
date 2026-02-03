package com.jrealm.game.state;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.jrealm.util.KeyHandler;
import com.jrealm.util.MouseHandler;

public abstract class GameState {

    public GameStateManager gsm;

    public GameState(GameStateManager gsm) {
        this.gsm = gsm;
    }

    public abstract void update(double time);

    public abstract void input(MouseHandler mouse, KeyHandler key);

    public abstract void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font);

    public GameStateManager getGameStateManager() {
        return this.gsm;
    }
}
