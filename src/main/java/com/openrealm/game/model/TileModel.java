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

}
