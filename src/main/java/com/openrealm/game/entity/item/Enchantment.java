package com.openrealm.game.entity.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Enchantment {
    public static final byte STAT_VIT = 0;
    public static final byte STAT_WIS = 1;
    public static final byte STAT_HP = 2;
    public static final byte STAT_MP = 3;
    public static final byte STAT_ATT = 4;
    public static final byte STAT_DEF = 5;
    public static final byte STAT_SPD = 6;
    public static final byte STAT_DEX = 7;

    private byte statId;
    private byte deltaValue;
    private byte pixelX;
    private byte pixelY;
    private int pixelColor;

    public Enchantment clone() {
        return new Enchantment(this.statId, this.deltaValue, this.pixelX, this.pixelY, this.pixelColor);
    }
}
