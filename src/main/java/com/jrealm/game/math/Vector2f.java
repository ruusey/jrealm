package com.jrealm.game.math;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.NetConstants;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableFloat;
import com.jrealm.net.realm.Realm;

import lombok.Data;

@Streamable
@Data
public class Vector2f extends SerializableFieldType<Vector2f>{
	@SerializableField(order = 0, type = SerializableFloat.class)
    public float x;
	@SerializableField(order = 1, type = SerializableFloat.class)
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

    public void addX(float f) {
        this.x += f;
    }

    public void addY(float f) {
        this.y += f;
    }

    public void setX(float f) {
        this.x = f;
    }

    public void setY(float f) {
        this.y = f;
    }

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

    public Vector2f rotateRad(float radians) {
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        float newX = (this.x * cos) - (this.y * sin);
        float newY = (this.x * sin) + (this.y * cos);

        this.x = newX;
        this.y = newY;

        return this;
    }

    public float distanceTo(Vector2f other) {
        return (float) Math.hypot(this.x - other.x, this.y - other.y);
    }

    public Vector2f withNoise(int xVariance, int yVariance) {
        int xRandom = Realm.RANDOM.nextInt(xVariance / 2);
        int yRandom = Realm.RANDOM.nextInt(yVariance / 2);

        if (Realm.RANDOM.nextBoolean()) {
            xRandom = -xRandom;
        }

        if (Realm.RANDOM.nextBoolean()) {
            yRandom = -yRandom;
        }

        return new Vector2f(this.x + (float) xRandom, this.y + (float) yRandom);
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

    public boolean equals(Vector2f other) {
        return (this.x == other.x) && (this.y == other.y);
    }

	@Override
	public Vector2f read(DataInputStream stream) throws Exception {
		final float x = stream.readFloat();
		final float y = stream.readFloat();
		return new Vector2f(x, y);
	}

	@Override
	public int write(Vector2f value, DataOutputStream stream) throws Exception {
		stream.writeFloat(value.x);
		stream.writeFloat(value.y);
		
		return NetConstants.FLOAT_LENGTH * 2;
	}

}
