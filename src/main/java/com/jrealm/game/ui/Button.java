package com.jrealm.game.ui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.states.GameStateManager;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

public class Button {

	private String label;
	private int lbWidth;
	private int lbHeight;

	private BufferedImage image;
	private BufferedImage hoverImage;
	private BufferedImage pressedImage;

	private Vector2f pos;
	private Vector2f dragPos;

	private AABB bounds;
	private AABB dragBounds;
	private boolean hovering = false;
	private int hoverSize;
	private ArrayList<ClickedEvent> events;
	private ArrayList<SlotEvent> slotevents;
	private boolean clicked = false;
	private boolean pressed = false;
	private boolean canHover = true;
	private boolean drawString = true;

	private float pressedtime;
	private Slots slot; // temp fix

	// ******************************************** ICON CUSTOM POS *******************************************
	public Button(Vector2f pos, int size) {
		this.pos = pos;
		this.dragPos = this.pos.clone();

		this.bounds = new AABB(this.pos, size, size);
		this.dragBounds = new AABB(this.dragPos, size, size);
		this.canHover = true;
		this.drawString = false;

		this.events = new ArrayList<ClickedEvent>();
		this.slotevents = new ArrayList<SlotEvent>();

	}

	public Button(BufferedImage icon, BufferedImage image, Vector2f pos, int width, int height, int iconsize) {
		this.pos = pos;
		this.dragPos = this.pos.clone();
		this.image = this.createIconButton(icon, image, width + iconsize, height + iconsize, iconsize);
		this.bounds = new AABB(this.pos, this.image.getWidth(), this.image.getHeight());

		this.events = new ArrayList<ClickedEvent>();
		this.slotevents = new ArrayList<SlotEvent>();
		this.canHover = false;
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

	// ******************************************** LABEL TTF CUSTOM MIDDLE POS *******************************************

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
		this.dragPos = this.pos.clone();

		this.bounds = new AABB(this.pos, this.image.getWidth(), this.image.getHeight());


		this.events = new ArrayList<ClickedEvent>();
		this.canHover = false;
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
		this.canHover = true;
	}

	public void addPressedImage(BufferedImage image) {
		this.pressedImage = image;
	}

	public void setHoverSize(int size) { this.hoverSize = size; }
	public boolean getHovering() { return this.hovering; }
	public void setHover(boolean b) { this.canHover = b; }
	public void addEvent(ClickedEvent e) { this.events.add(e);}
	public void addSlotEvent(SlotEvent e) { this.slotevents.add(e); }
	public void setSlot(Slots slot) { this.slot = slot;} // temp fix

	public int getWidth() { return (int) this.bounds.getWidth(); }
	public int getHeight() { return (int) this.bounds.getHeight(); }
	public Vector2f getPos() { return this.bounds.getPos(); }

	public void update(double time) {
		if((this.pressedImage != null) && this.pressed && ((this.pressedtime + 300) < (time / 1000000))) {
			this.pressed = false;
		}
	}

	private void hover(int value) {
		if(this.hoverImage == null) {
			this.pos.x -= value / 2;
			this.pos.y -= value / 2;
			float iWidth = value + this.bounds.getWidth();
			float iHeight = value + this.bounds.getHeight();
			this.bounds = new AABB(this.pos, (int) iWidth, (int) iHeight);

			this.pos.x -= value / 2;
			this.pos.y -= value / 2;
			this.lbWidth += value / 3;
			this.lbHeight += value / 3;

		}

		this.hovering = true;
	}

	public void input(MouseHandler mouse, KeyHandler key) {
		if(this.bounds.inside(mouse.getX(), mouse.getY())) {
			if(this.canHover && !this.hovering) {
				this.hover(this.hoverSize);
			}
			if((mouse.getButton() == 1) && !this.clicked) {
				this.clicked = true;
				this.pressed = true;

				this.pressedtime = System.nanoTime() / 1000000;

				for(int i = 0; i < this.events.size(); i++) {
					this.events.get(i).action(1);
				}
				if(this.slotevents == null) return;
				for(int i = 0; i < this.slotevents.size(); i++) {
					this.slotevents.get(i).action(this.slot);
				}
			} else if(mouse.getButton() == -1) {
				this.clicked = false;
			}
		} else if(this.canHover && this.hovering) {
			this.hover(-this.hoverSize);
			this.hovering = false;
		}
	}

	public void render(Graphics2D g) {
		if(this.drawString) {
			SpriteSheet.drawArray(g, this.label, this.pos, this.lbWidth, this.lbHeight);
		}

		if(this.canHover && (this.hoverImage != null) && this.hovering) {
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

	public interface ClickedEvent {
		void action(int mouseButton);
	}

	public interface SlotEvent {
		void action(Slots slot);
	}

}