package com.openrealm.game.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class SpriteModel {
    private String spriteKey;
    private int row;
    private int col;
    private int spriteSize;
    private int spriteHeight;
    private String angleOffset;

    public int getEffectiveSpriteHeight() {
        return this.spriteHeight > 0 ? this.spriteHeight : this.spriteSize;
    }
}
