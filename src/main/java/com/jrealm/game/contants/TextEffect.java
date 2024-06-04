package com.jrealm.game.contants;

public enum TextEffect {

    DAMAGE, HEAL, ARMOR_BREAK, ENVIRONMENT, PLAYER_INFO;

    public static TextEffect from(final byte ordinal) {
        TextEffect result = null;
        for (final TextEffect effect : TextEffect.values()) {
            if (effect.ordinal() == (int) ordinal) {
                result = effect;
            }
        }
        return result;
    }
}
