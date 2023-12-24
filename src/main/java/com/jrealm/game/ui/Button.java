package com.jrealm.game.ui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.state.GameStateManager;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

import lombok.Data;

@Data
public class Button {

	private String label;
	private int lbWidth;
	private int lbHeight;

	private BufferedImage image;
	private BufferedImage hoverImage;
	private BufferedImage pressedImage;

	private Vector2f pos;

	private AABB bounds;
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

		this.bounds = new AABB(this.pos, size, size);
		this.drawString = false;
		this.rightClickEvents = new ArrayList<RightClickEvent>();
		this.hoverInEvents = new ArrayList<HoverInEvent>();
		this.hoverOutEvents = new ArrayList<HoverOutEvent>();
		this.mouseDownEvents = new ArrayList<MouseDownEvent>();
		this.mouseUpEvents = new ArrayList<MouseUpEvent>();
	}

	public Button(BufferedImage icon, BufferedImage image, Vector2f pos, int width, int height, int iconsize) {
		this.pos = pos;
		this.image = this.createIconButton(icon, image, width + iconsize, height + iconsize, iconsize);
		this.bounds = new AABB(this.pos, this.image.getWidth(), this.image.getHeight());
		this.rightClickEvents = new ArrayList<RightClickEvent>();

		this.hoverInEvents = new ArrayList<HoverInEvent>();
		this.hoverOutEvents = new ArrayList<HoverOutEvent>();

		this.mouseDownEvents = new ArrayList<MouseDownEvent>();
		this.mouseUpEvents = new ArrayList<MouseUpEvent>();
		this.drawString = false;
	}

	private BufferedImage createIconButton(BufferedImage icon, BufferedImage image, int width, int height, int iconsize) {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		if((image.getWidth() != width) || (image.getHeight() != height)) {
			image = this.resizeImage(image, width, height);
		}

		if((icon.getWidth() != (width - iconsize)) || (icon.getHeight() != (height - iconsize))) {
			icon = this.resizeImage(icon, width - iconsize, height - iconsize);
		}

		Graphics g = result.getGraphics();
		g.drawImage(image, 0, 0, width, height, null);

		g.drawImage(icon,
				(image.getWidth() / 2) - (icon.getWidth() / 2),
				(image.getHeight() / 2) - (icon.getHeight() / 2),
				icon.getWidth(), icon.getHeight(), null);

		g.dispose();

		return result;
	}

	public Button(String label, BufferedImage image, Font font, Vector2f pos, int buttonSize) {
		this(label, image, font, pos, buttonSize, -1);
	}

	public Button(String label, BufferedImage image, Font font, Vector2f pos, int buttonWidth, int buttonHeight) {
		GameStateManager.g.setFont(font);
		FontMetrics met = GameStateManager.g.getFontMetrics(font);
		int height = met.getHeight();
		int width = met.stringWidth(label);

		if(buttonWidth == -1) {
			buttonWidth = buttonHeight;
		}

		this.label = label;

		this.image = this.createButton(label, image, font, width + buttonWidth, height + buttonHeight, buttonWidth, buttonHeight);
		this.pos = pos;

		this.bounds = new AABB(this.pos, this.image.getWidth(), this.image.getHeight());
		this.rightClickEvents = new ArrayList<RightClickEvent>();

		this.hoverInEvents = new ArrayList<HoverInEvent>();
		this.hoverOutEvents = new ArrayList<HoverOutEvent>();

		this.mouseDownEvents = new ArrayList<MouseDownEvent>();
		this.mouseUpEvents = new ArrayList<MouseUpEvent>();

		this.drawString = false;
	}

	public BufferedImage createButton(String label, BufferedImage image, Font font, int width, int height, int buttonWidth, int buttonHeight) {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		if((image.getWidth() != width) || (image.getHeight() != height)) {
			image = this.resizeImage(image, width, height);
		}

		Graphics g = result.getGraphics();
		g.drawImage(image, 0, 0, width, height, null);

		g.setFont(font);
		g.drawString(label, buttonWidth / 2, (height - buttonHeight));

		g.dispose();

		return result;
	}

	private BufferedImage resizeImage(BufferedImage image, int width, int height) {
		Image temp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = result.createGraphics();

		g.drawImage(temp, 0, 0, null);
		g.dispose();

		return result;
	}

	public void addHoverImage(BufferedImage image) {
		this.hoverImage = image;
	}

	public void addPressedImage(BufferedImage image) {
		this.pressedImage = image;
	}

	public boolean getHovering() { return this.hovering; }

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

	public int getWidth() { return (int) this.bounds.getWidth(); }
	public int getHeight() { return (int) this.bounds.getHeight(); }
	public Vector2f getPos() { return this.bounds.getPos(); }

	public void update(double time) {
		if((this.pressedImage != null) && this.pressed && ((this.pressedtime + 300) < (time / 1000000))) {
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
			if ((mouse.isPressed(MouseEvent.BUTTON1)) && !this.clicked) {
				this.clicked = true;
				this.pressed = true;

				this.pressedtime = System.nanoTime() / 1000000;
				for (int i = 0; i < this.mouseDownEvents.size(); i++) {
					this.mouseDownEvents.get(i).action(1);
				}
			} else if(mouse.isPressed(MouseEvent.BUTTON3) && !this.clicked) {
				this.clicked = true;
				this.pressed = true;
				this.pressedtime = System.nanoTime() / 1000000;
				for (int i = 0; i < this.rightClickEvents.size(); i++) {
					this.rightClickEvents.get(i).action(new Vector2f(mouse.getX(), mouse.getY()));
				}
			} else if ((!mouse.isPressed(MouseEvent.BUTTON1)) && this.clicked) {
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
		} else if ((!mouse.isPressed(MouseEvent.BUTTON1)) && this.clicked) {
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

	public void render(Graphics2D g) {
		if(this.drawString) {
			SpriteSheet.drawArray(g, this.label, this.pos, this.lbWidth, this.lbHeight);
		}

		if ((this.hoverImage != null) && this.hovering) {
			g.drawImage(this.hoverImage, (int) this.pos.x, (int) this.pos.y, (int) this.bounds.getWidth(),
					(int) this.bounds.getHeight(), null);
		} else if((this.pressedImage != null) && this.pressed) {
			g.drawImage(this.pressedImage, (int) this.pos.x, (int) this.pos.y, (int) this.bounds.getWidth(),
					(int) this.bounds.getHeight(), null);
		} else if (this.image != null) {
			g.drawImage(this.image, (int) this.pos.x, (int) this.pos.y, (int) this.bounds.getWidth(),
					(int) this.bounds.getHeight(), null);
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