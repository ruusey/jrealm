package com.jrealm.game.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PortalModel extends SpriteModel {
    private int portalId;
    private int targetRealmDepth;
    private String portalName;
    private int mapId;
    // Graph-based dungeon system: the target node this portal leads to
    private String targetNodeId;

}
