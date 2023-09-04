package com.jrealm.game.math;


public class Vector2f {

	public float x;
	public float y;

	public static float worldX;
	public static float worldY;

	public Vector2f() {
		this.x = 0;
		this.y = 0;
	}

	public Vector2f(Vector2f vec) {
		new Vector2f(vec.x, vec.y);
	}

	public Vector2f(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void addX(float f) { this.x += f; }
	public void addY(float f) { this.y += f; }

	public void setX(float f) { this.x = f; }
	public void setY(float f) { this.y = f; }

	public void setVector(Vector2f vec) {
		this.x = vec.x;
		this.y = vec.y;
	}

	public void setVector(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public static void setWorldVar(float x, float y) {
		Vector2f.worldX = x;
		Vector2f.worldY = y;
	}

	public static float getWorldVarX(float x) {
		return x - Vector2f.worldX;
	}

	public static float getWorldVarY(float y) {
		return y - Vector2f.worldY;
	}

	@Override
	public Vector2f clone() {
		return new Vector2f(this.x, this.y);
	}

	public Vector2f clone(float xOffset, float yOffset) {
		return new Vector2f(this.x + xOffset, this.y + yOffset);
	}

	public Vector2f getWorldVar() {
		return new Vector2f(this.x - Vector2f.worldX, this.y - Vector2f.worldY);
	}

	@Override
	public String toString() {
		return this.x + ", " + this.y;
	}

}
