package com.openrealm.game.state;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.openrealm.game.OpenRealmGame;
import com.openrealm.net.client.ClientGameLogic;
import com.openrealm.util.KeyHandler;
import com.openrealm.util.MouseHandler;

public class GameOverState extends GameState {

    public GameOverState(GameStateManager gsm) {
        super(gsm);
    }

    @Override
    public void update(double time) {
    }

    @Override
    public void input(MouseHandler mouse, KeyHandler key) {
        key.escape.tick();
        key.enter.tick();

        if (key.enter.clicked) {
            ClientGameLogic.GAME_OVER = false;
            this.gsm.pop(GameStateManager.GAMEOVER);
            this.gsm.add(GameStateManager.PLAY);
        }

        if (key.escape.clicked) {
            System.exit(0);
        }
    }

    @Override
    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        // Black background
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.BLACK);
        shapes.rect(0, 0, OpenRealmGame.width, OpenRealmGame.height);
        shapes.end();
        batch.begin();

        font.setColor(Color.RED);
        font.draw(batch, "GAME OVER", OpenRealmGame.width / 2f - 60, OpenRealmGame.height / 2f - 32);
        font.setColor(Color.WHITE);
        font.draw(batch, "Press ENTER to restart or ESC to quit", OpenRealmGame.width / 2f - 180, OpenRealmGame.height / 2f + 16);
    }
}
