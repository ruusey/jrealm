package com.jrealm.game.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Slots {
    private GameItem item;
    private Button button;
    private boolean selected;
    private Vector2f dragPos;

    public Slots(Button button, GameItem item) {
        this.item = item;
        this.button = button;
    }

    public void update(double time) {
        if (this.button != null) {
            this.button.update(time);
        }
    }

    public void input(MouseHandler mouse, KeyHandler key) {
        if (this.button != null) {
            this.button.input(mouse, key);
        }

        if (this.button.isClicked()) {
            this.dragPos = new Vector2f(mouse.getX(), mouse.getY());
        } else {
            this.dragPos = null;
        }
    }

    public void render(Graphics2D g, Vector2f pos) {
        if (this.getItem() == null)
            return;
        if (this.getItem().getSpriteKey() == null) {
            GameDataManager.loadSpriteModel(this.getItem());
        }
        if(this.isSelected()) {
            g.setColor(Color.yellow);
            g.fillRect((int) pos.x, (int) pos.y, 64, 64);
        }else {
        	 g.setColor(Color.gray);
             g.fillRect((int) pos.x, (int) pos.y, 64, 64);
        }

        BufferedImage itemImage = GameSpriteManager.ITEM_SPRITES.get(this.item.getItemId());
        if (itemImage == null)
            return;
        if (this.button != null) {
            this.button.render(g);
        } else {
            g.drawImage(itemImage, (int) pos.x, (int) pos.y, 64, 64, null);
        }
        g.drawImage(itemImage, (int) pos.x, (int) pos.y, 64, 64, null);
    }
}