package com.jrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.jrealm.game.contants.LootTier;
public class LootTierToByteConverter extends AbstractConverter<Byte, LootTier> {

    @Override
    protected LootTier convert(Byte source) {
        return LootTier.valueOf(source);
    }
}