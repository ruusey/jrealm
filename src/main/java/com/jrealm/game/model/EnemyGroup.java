package com.jrealm.game.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnemyGroup {
    private String name;
    private int ordinal;
    private List<Integer> enemyIds;

}
