package com.jrealm.game.state;

import java.awt.Color;
import java.awt.Graphics2D;

import com.jrealm.game.GamePanel;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

public class MenuState extends GameState {
    private String currentMessage;
    private boolean releasedEnter;
    private boolean pressedEnter;
    private PlayState state;
    public MenuState(GameStateManager gsm) {
        super(gsm);
        this.currentMessage="";
    }

    @Override
    public void update(double time) {

    }

    @Override
    public void input(MouseHandler mouse, KeyHandler key) {
        key.escape.tick();
        key.f1.tick();
        key.f2.tick();
        key.shift.tick();
        key.t.tick();
        key.enter.tick();
        key.one.tick();
        key.two.tick();
        key.three.tick();
        key.four.tick();
        key.five.tick();
        key.six.tick();
        key.seven.tick();
        key.eight.tick();

    	if(key.getContent()==null || key.getContent().isEmpty()) return;
    	System.out.println();
        this.currentMessage += key.getContent();

        if (key.enter.down && !this.pressedEnter) {
            this.pressedEnter = true;
        }

        if (this.pressedEnter && this.releasedEnter) {
            this.pressedEnter = false;
            this.releasedEnter = false;
            
            this.gsm.add(GameStateManager.PLAY);

        }
        
        if (this.pressedEnter && !key.enter.down) {
            this.releasedEnter = true;
            return;
        }
    }

    @Override
    public void render(Graphics2D g) {
    	g.setColor(Color.black);
    	g.fillRect(0, 0, GamePanel.width, GamePanel.height);
        g.setColor(Color.WHITE);
        g.drawString(this.currentMessage, (int) (GamePanel.width - (GamePanel.width /2 )), (int) (GamePanel.height - (GamePanel.height /2 )));
        g.setColor(Color.GRAY);
        g.drawRect((int) (GamePanel.width - (GamePanel.width /2 )), (int) (GamePanel.height - (GamePanel.height /2 )), 100,30);
    }
}
