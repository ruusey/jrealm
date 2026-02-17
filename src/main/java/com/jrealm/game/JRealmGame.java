package com.jrealm.game;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.graphics.ShaderManager;
import com.jrealm.game.state.GameStateManager;
import com.jrealm.util.KeyHandler;
import com.jrealm.util.MouseHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JRealmGame implements ApplicationListener {
    public static int width = 1920;
    public static int height = 1080;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private OrthographicCamera camera;
    private BitmapFont defaultFont;
    private GameStateManager gsm;
    private KeyHandler keyHandler;
    private MouseHandler mouseHandler;

    @Override
    public void create() {
        JRealmGame.log.info("Initializing LibGDX client...");

        this.batch = new SpriteBatch();
        this.shapes = new ShapeRenderer();

        // Y-down camera to match existing game math (0,0 at top-left)
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(true, width, height);

        this.defaultFont = new BitmapFont(true); // flipped for Y-down
        this.defaultFont.getData().setScale(1.8f);

        // Load sprite sheets as LibGDX Textures
        GameSpriteManager.loadSpriteImages(true);
        GameSpriteManager.loadTileSprites();
        GameSpriteManager.loadItemSprites();

        // Initialize shaders for sprite effects
        ShaderManager.init();

        // Set up input handlers
        this.keyHandler = new KeyHandler();
        Gdx.input.setInputProcessor(this.keyHandler);
        this.mouseHandler = new MouseHandler();

        // Initialize the game state manager and enter PlayState
        this.gsm = new GameStateManager(this.batch, this.shapes, this.defaultFont, this.camera);

        JRealmGame.log.info("LibGDX client initialized.");
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Clear screen with dark background
        Gdx.gl.glClearColor(33f / 255f, 30f / 255f, 39f / 255f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update input state
        this.keyHandler.update();
        this.mouseHandler.update();

        // Update game state
        this.gsm.update(delta);

        // Process input
        this.gsm.input(this.mouseHandler, this.keyHandler);

        // Render game state
        this.camera.update();
        this.batch.setProjectionMatrix(this.camera.combined);
        this.shapes.setProjectionMatrix(this.camera.combined);

        this.batch.begin();
        this.gsm.render(this.batch, this.shapes, this.defaultFont);
        this.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        JRealmGame.width = width;
        JRealmGame.height = height;
        this.camera.setToOrtho(true, width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        if (this.batch != null) this.batch.dispose();
        if (this.shapes != null) this.shapes.dispose();
        if (this.defaultFont != null) this.defaultFont.dispose();
        ShaderManager.dispose();
        GameSpriteManager.disposeAll();
    }
}
