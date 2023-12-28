package com.jrealm.game.ui;

public enum TextEffect {

	DAMAGE, HEAL, ARMOR_BREAK, ENVIRONMENT;

	public static TextEffect fromOrdinal(final byte ordinal) {
		TextEffect result = null;
		for (final TextEffect effect : TextEffect.values()) {
			if (effect.ordinal() == (int) ordinal) {
				result = effect;
			}
		}
		return result;
	}
}
