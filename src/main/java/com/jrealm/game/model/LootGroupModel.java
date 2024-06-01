package com.jrealm.game.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LootGroupModel {
    private Integer lootGroupId;
    private String lootGroupName;
    private List<Integer> potentialDrops;
}
