package com.jrealm.game.util;

import com.jrealm.game.entity.GameObject;
import com.jrealm.game.math.Rectangle;

import lombok.Data;

@Data
public class GameObjectKey {

	public float value;
	public GameObject go;

	public GameObjectKey(float value, GameObject go) {
		this.value = value;
		this.go = go;
	}

	public Rectangle getBounds() { return this.go.getBounds(); }
}