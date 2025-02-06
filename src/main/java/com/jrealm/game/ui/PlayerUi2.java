package com.jrealm.game.ui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.jrealm.game.GamePanel;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.graphics.Sprite;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlayerUi2 extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1432183643127528001L;
	private Map<String, GameItem> gameItemUidMap;

	public PlayerUi2() {
		super(new GridLayout());
		this.setSize(new Dimension((GamePanel.width / 5), GamePanel.height));
	}
	
	
	public void createItemIcons(GameItem[] inventory) {
		
		gameItemUidMap =  new HashMap<>();
        for(GameItem item : inventory) {
        	if(item==null || item.getItemId()==-1) continue;
        	GameDataManager.loadSpriteModel(item);
        	Sprite sprite = GameSpriteManager.loadSprite(item);
            ImageIcon icon = new ImageIcon(sprite.getImage());
            JButton button = new JButton(item.getUid(), icon);
            button.addActionListener(this.clickItem());
            this.add(item.getUid(), button);	
            this.gameItemUidMap.put(item.getUid(), item);
        }

	}
	
	public ActionListener clickItem() {
		return e ->{
			final JButton source = (JButton)e.getSource();
			final GameItem buttonItem = this.gameItemUidMap.get(source.getName());
			log.info("Button clicked for item {}", buttonItem);
		};
	}
}
