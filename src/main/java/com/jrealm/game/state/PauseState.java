package com.jrealm.game.state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;

import com.jrealm.account.dto.CharacterDto;
import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.ui.Button;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

public class PauseState extends GameState {
    
    private Button btnResume;
    private Button btnExit;
    private Font font;
    private PlayerAccountDto account;
    public PauseState(GameStateManager gsm, PlayerAccountDto account) {
        super(gsm);
        this.account = account;
        BufferedImage imgButton = GameStateManager.button.cropImage(0, 0, 121, 26);
        BufferedImage imgHover = GameStateManager.button.cropImage(0, 29, 122, 28);

        font = new Font("MeatMadness", Font.PLAIN, 48);
        btnResume = new Button("RESUME", imgButton, font, new Vector2f(GamePanel.width / 2, GamePanel.height / 2 - 48), 32, 16);
        btnExit = new Button("EXIT", imgButton, font, new Vector2f(GamePanel.width / 2, GamePanel.height / 2 + 48), 32, 16);

        btnResume.addHoverImage(btnResume.createButton("RESUME", imgHover, font, btnResume.getWidth(), btnResume.getHeight(), 32, 20));
        btnExit.addHoverImage(btnExit.createButton("EXIT", imgHover, font, btnExit.getWidth(), btnExit.getHeight(), 32, 20));
        
        btnResume.onMouseDown(e -> {
            gsm.pop(GameStateManager.PAUSE);
        });

        btnExit.onMouseDown(e -> {
            System.exit(0);
        });
    }

    @Override
    public void update(double time) {

    }

    @Override
    public void input(MouseHandler mouse, KeyHandler key) {
        btnResume.input(mouse, key);
        btnExit.input(mouse, key);

    }

    @Override
    public void render(Graphics2D g) {
    	int i = 0;

        btnResume.render(g);
        btnExit.render(g);
        int rowWidth = 500;
        int rowHeight = 100;
        if(this.account!=null) {
        	for(CharacterDto cls : this.account.getCharacters()) {
        		final CharacterClass characterClass = CharacterClass.valueOf(cls.getCharacterClass());
        		int lvl = 0;
        		if(GameDataManager.EXPERIENCE_LVLS.isMaxLvl(cls.getStats().getXp())) {
        			lvl = 20;
        		}else {
        			lvl = GameDataManager.EXPERIENCE_LVLS.getLevel(cls.getStats().getXp());
        		}
        		
        		final SpriteSheet classImg = GameSpriteManager.loadClassSprites(characterClass);
        		String characterStr = "{0}, lv {1} {2} {3}/8";
        		characterStr = MessageFormat.format(characterStr, this.account.getAccountName(), lvl, characterClass, 0);
        		g.setColor(Color.GRAY);
        		g.fillRect(0, (i*rowHeight), rowWidth, rowHeight);
        		
        		g.setColor(Color.WHITE);
        		g.drawString(characterStr, 100, 32+(rowHeight*i));
        		g.drawImage(classImg.getCurrentFrame(),0, i*rowHeight, 64,64, null);
        		i++;
        	}
        }
    }    
}
