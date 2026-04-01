package com.jrealm.game.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnimationModel {
    private int objectId;
    private String objectType;
    private String className;
    private String spriteKey;
    private Map<String, AnimationSetModel> animations;
}
