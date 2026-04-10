package com.openrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.openrealm.game.contants.LootTier;
public class LootTierToByteConverter extends AbstractConverter<Byte, LootTier> {

    @Override
    protected LootTier convert(Byte source) {
        return LootTier.valueOf(source);
    }
}