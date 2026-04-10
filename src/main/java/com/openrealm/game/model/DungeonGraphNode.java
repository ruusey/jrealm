package com.openrealm.game.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class DungeonGraphNode {
    private String nodeId;
    private String displayName;
    private int mapId;
    private float difficulty;
    private boolean entryPoint;
    private boolean bossNode;
    private boolean shared;
    private List<String> childNodes;
    // Maps child nodeId -> portalId used to reach it
    private Map<String, Integer> portalDropNodeMap;
}
