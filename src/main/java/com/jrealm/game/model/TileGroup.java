package com.jrealm.game.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TileGroup {
    private String name;
    private Integer ordinal;
    private List<Integer> tileIds;
    private Map<String, Float> rarities;
}
