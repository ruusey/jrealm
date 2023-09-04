package com.jrealm.game.util;

import com.jrealm.game.entity.GameObject;
import com.jrealm.game.math.AABB;

public class GameObjectKey {

    public float value;
    public GameObject go;

    public GameObjectKey(float value, GameObject go) {
        this.value = value;
        this.go = go;
    }

    public AABB getBounds() { return go.getBounds(); }
}