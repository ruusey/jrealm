package com.jrealm.game.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnimationSetModel {
    private List<AnimationFrameModel> frames;
    private List<Integer> durations;
}
