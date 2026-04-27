package com.openrealm.game.model;

import com.openrealm.game.tile.TileData;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class TileModel extends SpriteModel {
    private int tileId;
    private String name;
    private short size;
    private TileData data;
    /**
     * Optional tag that marks this tile as interactive. When non-null, the
     * client may send InteractTilePacket while standing adjacent to it; the
     * server validates and dispatches based on this string. Examples: "forge".
     */
    private String interactionType;

}
