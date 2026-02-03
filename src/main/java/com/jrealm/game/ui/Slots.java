package com.jrealm.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.math.Vector2f;
import com.jrealm.util.KeyHandler;
import com.jrealm.util.MouseHandler;

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

    public void render(SpriteBatch batch, ShapeRenderer shapes, Vector2f pos) {
        if (this.getItem() == null)
            return;
        if (this.getItem().getSpriteKey() == null) {
            GameDataManager.loadSpriteModel(this.getItem());
        }

        // Draw slot background via ShapeRenderer
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (this.isSelected()) {
            shapes.setColor(Color.YELLOW);
        } else {
            shapes.setColor(Color.GRAY);
        }
        shapes.rect(pos.x, pos.y, 64, 64);
        shapes.end();
        batch.begin();

        TextureRegion itemRegion = GameSpriteManager.ITEM_SPRITES.get(this.item.getItemId());
        if (itemRegion == null)
            return;
        if (this.button != null) {
            this.button.render(batch);
        }
        batch.draw(itemRegion, pos.x, pos.y, 64, 64);
    }
}
