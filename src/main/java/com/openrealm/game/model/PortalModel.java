package com.openrealm.game.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PortalModel extends SpriteModel {
    private int portalId;
    private String portalName;
    private int mapId;
    // Graph-based dungeon system: the target node this portal leads to
    private String targetNodeId;
    // Static portal placement: world position (pixels) when placed on a map
    private float x;
    private float y;
    private String label;
}
