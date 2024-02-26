package com.jrealm.game.math;

import java.util.ArrayList;

import com.jrealm.game.entity.GameObject;

import lombok.Data;

@Data
public class Rectangle {
	private Vector2f pos;
	private float xOffset = 0;
	private float yOffset = 0;
	private float w;
	private float h;
	private float r;
	private int size;

	public Rectangle(Vector2f pos, int w, int h) {
		this.pos = pos;
		this.w = w;
		this.h = h;
		this.size = Math.max(w, h);
	}

	public Rectangle(Vector2f pos, int r) {
		this.pos = pos;
		this.r = r;
		this.size = r;
	}

	public Vector2f getPos() { return this.pos; }

	public float getRadius() { return this.r; }
	public float getSize() { return this.size; }
	public float getWidth() { return this.w; }
	public float getHeight() { return this.h; }

	public void setBox(Vector2f pos, int w, int h) {
		this.pos = pos;
		this.w = w;
		this.h = h;
		this.size = Math.max(w, h);
	}

	public void setCircle(Vector2f pos, int r) {
		this.pos = pos;
		this.r = r;
		this.size = r;
	}

	public void setWidth(float f) { this.w = f; }
	public void setHeight(float f) { this.h = f; }

	public void setXOffset(float f) { this.xOffset = f; }
	public void setYOffset(float f) { this.yOffset = f; }
	public float getXOffset() { return this.xOffset; }
	public float getYOffset() { return this.yOffset; }

	public boolean collides(Rectangle bBox) {
		return this.collides(0, 0, bBox);
	}

	public boolean collides(float dx, float dy, ArrayList<GameObject> go) {
		boolean collides = false;

		for(int i = 0; i < go.size(); i++) {
			collides = this.collides(dx, dy, go.get(i).getBounds());
			if(collides) {
				go.get(i).getImage().restoreDefault();
				go.remove(i);
				return collides;
			}
		}
		return collides;
	}

	public boolean collides(float dx, float dy, Rectangle bBox) {
		float ax = ((this.pos.x + (this.xOffset)) + (this.w / 2)) + dx;
		float ay = ((this.pos.y + (this.yOffset)) + (this.h / 2)) + dy;
		float bx = ((bBox.getPos().x + (bBox.getXOffset())) + (bBox.getWidth() / 2));
		float by = ((bBox.getPos().y + (bBox.getYOffset())) + (bBox.getHeight() / 2));

		if (Math.abs(ax - bx) < ((this.w / 2) + (bBox.getWidth() / 2))) {
			if (Math.abs(ay - by) < ((this.h / 2) + (bBox.getHeight() / 2)))
				return true;
		}
		return false;
	}

	public boolean inside(int xp, int yp) {
		if((xp == -1) || (yp == - 1)) return false;

		int wTemp = (int) this.w;
		int hTemp = (int) this.h;
		int x = (int) this.pos.x;
		int y = (int) this.pos.y;

		if((xp < x) || (yp < y))
			return false;

		wTemp += x;
		hTemp += y;
		return (((wTemp < x) || (wTemp > xp)) && ((hTemp < y) || (hTemp > yp)));
	}

	public boolean intersect(Rectangle aBox) {

		if(((this.pos.x + this.xOffset) > (aBox.getPos().x + aBox.getXOffset() + aBox.getSize()))
				|| ((aBox.getPos().x + this.xOffset) > (this.pos.x + aBox.getXOffset() + aBox.getSize())))
			return false;

		if(((this.pos.y + this.yOffset) > (aBox.getPos().y + aBox.getYOffset() + aBox.getSize()))
				|| ((aBox.getPos().y + this.yOffset) > (this.pos.y + aBox.getYOffset() + aBox.getSize())))
			return false;

		return true;
	}

	public boolean colCircle(Rectangle circle) {
		float totalRadius = this.r + circle.getRadius();
		totalRadius *= totalRadius;

		float dx = (this.pos.x + circle.getPos().x);
		float dy = (this.pos.y + circle.getPos().y);

		return totalRadius < ((dx * dx) + (dy * dy));
	}

	public boolean colCircleBox(Rectangle aBox) {
		float dx = Math.max(aBox.getPos().x + aBox.getXOffset(), Math.min(this.pos.x + (this.r / 2), aBox.getPos().x + aBox.getXOffset() + aBox.getWidth()));
		float dy = Math.max(aBox.getPos().y + aBox.getYOffset(), Math.min(this.pos.y + (this.r / 2), aBox.getPos().y + aBox.getYOffset() + aBox.getHeight()));

		dx = (this.pos.x + (this.r / 2)) - dx;
		dy = (this.pos.y + (this.r / 2)) - dy;

		if(Math.sqrt((dx * dx) + (dy * dy)) < (this.r / 2))
			return true;

		return false;
	}

	public float distance(Vector2f other) {
		float dx = this.pos.x - other.x;
		float dy = this.pos.y - other.y;
		return (float) Math.sqrt((dx * dx) + (dy * dy));
	}

	public Rectangle merge(Rectangle other) {
		float minX = Math.min(this.pos.x, other.getPos().x);
		float minY = Math.min(this.pos.y, other.getPos().y);

		int maxW = (int) Math.max(this.w, other.getWidth());
		int maxH = (int) Math.max(this.h, other.getHeight());

		Vector2f pos = new Vector2f(minX, minY);
		return new Rectangle(pos, maxW, maxH);
	}

	@Override
	public String toString() {
		String x = Float.toString(this.pos.x);
		String y = Float.toString(this.pos.y);
		String w = Float.toString(this.w);
		String h = Float.toString(this.h);

		return "{" + x + ", " + y + " : " + w + ", " + h + "}";
	}
}
