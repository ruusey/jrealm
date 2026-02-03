package com.jrealm.game.ui;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.util.KeyHandler;
import com.jrealm.util.MouseHandler;

import lombok.Data;

@Data
public class Button {

    private String label;
    private int lbWidth;
    private int lbHeight;

    private TextureRegion image;
    private TextureRegion hoverImage;
    private TextureRegion pressedImage;

    private Vector2f pos;

    private Rectangle bounds;
    private boolean hovering = false;
    private boolean canHover = true;
    private ArrayList<RightClickEvent> rightClickEvents;

    private ArrayList<MouseDownEvent> mouseDownEvents;
    private ArrayList<MouseUpEvent> mouseUpEvents;
    private ArrayList<HoverInEvent> hoverInEvents;
    private ArrayList<HoverOutEvent> hoverOutEvents;

    private boolean clicked = false;
    private boolean pressed = false;
    private boolean drawString = true;

    private float pressedtime;

    public Button(Vector2f pos, int size) {
        this.pos = pos;
        this.bounds = new Rectangle(this.pos, size, size);
        this.drawString = false;
        this.rightClickEvents = new ArrayList<>();
        this.hoverInEvents = new ArrayList<>();
        this.hoverOutEvents = new ArrayList<>();
        this.mouseDownEvents = new ArrayList<>();
        this.mouseUpEvents = new ArrayList<>();
    }

    public Button(String label, Vector2f pos, int width, int height) {
        this.label = label;
        this.pos = pos;
        this.bounds = new Rectangle(this.pos, width, height);
        this.rightClickEvents = new ArrayList<>();
        this.hoverInEvents = new ArrayList<>();
        this.hoverOutEvents = new ArrayList<>();
        this.mouseDownEvents = new ArrayList<>();
        this.mouseUpEvents = new ArrayList<>();
        this.drawString = true;
    }

    public void addHoverImage(TextureRegion image) {
        this.hoverImage = image;
    }

    public void addPressedImage(TextureRegion image) {
        this.pressedImage = image;
    }

    public boolean getHovering() {
        return this.hovering;
    }

    public void onRightClick(RightClickEvent e) {
        this.rightClickEvents.add(e);
    }

    public void onMouseDown(MouseDownEvent e) {
        this.mouseDownEvents.add(e);
    }

    public void onMouseUp(MouseUpEvent e) {
        this.mouseUpEvents.add(e);
    }

    public void onHoverIn(HoverInEvent e) {
        this.hoverInEvents.add(e);
    }

    public void onHoverOut(HoverOutEvent e) {
        this.hoverOutEvents.add(e);
    }

    public int getWidth() {
        return (int) this.bounds.getWidth();
    }

    public int getHeight() {
        return (int) this.bounds.getHeight();
    }

    public Vector2f getPos() {
        return this.bounds.getPos();
    }

    public void update(double time) {
        if ((this.pressedImage != null) && this.pressed && ((this.pressedtime + 300) < (time / 1000000))) {
            this.pressed = false;
        }
    }

    public void input(MouseHandler mouse, KeyHandler key) {
        if (this.bounds.inside(mouse.getX(), mouse.getY())) {
            this.hovering = true;

            if (this.hovering && this.canHover) {
                for (int i = 0; i < this.hoverInEvents.size(); i++) {
                    this.hoverInEvents.get(i).action(1);
                }
            }
            this.canHover = false;
            if ((mouse.isPressed(1)) && !this.clicked) {
                this.clicked = true;
                this.pressed = true;

                this.pressedtime = System.nanoTime() / 1000000;
                for (int i = 0; i < this.mouseUpEvents.size(); i++) {
                    this.mouseUpEvents.get(i).action(new Vector2f(mouse.getX(), mouse.getY()));
                }
            } else if (mouse.isPressed(3) && !this.clicked) {
                this.clicked = true;
                this.pressed = true;
                this.pressedtime = System.nanoTime() / 1000000;
                for (int i = 0; i < this.rightClickEvents.size(); i++) {
                    this.rightClickEvents.get(i).action(new Vector2f(mouse.getX(), mouse.getY()));
                }
            } else if ((!mouse.isPressed(1)) && this.clicked) {
                this.clicked = false;
                for (int i = 0; i < this.mouseUpEvents.size(); i++) {
                    this.mouseUpEvents.get(i).action(new Vector2f(mouse.getX(), mouse.getY()));
                }
            }
        } else if (this.hovering && !this.bounds.inside(mouse.getX(), mouse.getY())) {
            this.hovering = false;
            this.canHover = true;
            for (int i = 0; i < this.hoverOutEvents.size(); i++) {
                this.hoverOutEvents.get(i).action(1);
            }
        } else if ((!mouse.isPressed(1)) && this.clicked) {
            this.clicked = false;
            for (int i = 0; i < this.mouseUpEvents.size(); i++) {
                this.mouseUpEvents.get(i).action(new Vector2f(mouse.getX(), mouse.getY()));
            }
        }

        if (!this.bounds.inside(mouse.getX(), mouse.getY())) {
            this.hovering = false;
            this.canHover = true;
        }
    }

    public void render(SpriteBatch batch) {
        if ((this.hoverImage != null) && this.hovering) {
            batch.draw(this.hoverImage, this.pos.x, this.pos.y, this.bounds.getWidth(), this.bounds.getHeight());
        } else if ((this.pressedImage != null) && this.pressed) {
            batch.draw(this.pressedImage, this.pos.x, this.pos.y, this.bounds.getWidth(), this.bounds.getHeight());
        } else if (this.image != null) {
            batch.draw(this.image, this.pos.x, this.pos.y, this.bounds.getWidth(), this.bounds.getHeight());
        }
    }

    public interface RightClickEvent {
        void action(Vector2f mouseButton);
    }

    public interface MouseDownEvent {
        void action(int mouseButton);
    }

    public interface MouseUpEvent {
        void action(Vector2f mousePos);
    }

    public interface HoverInEvent {
        void action(int mouseButton);
    }

    public interface HoverOutEvent {
        void action(int mouseButton);
    }
}
