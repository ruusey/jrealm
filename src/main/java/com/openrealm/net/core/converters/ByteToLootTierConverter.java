package com.openrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.openrealm.game.contants.LootTier;
public class ByteToLootTierConverter extends AbstractConverter<LootTier, Byte> {

    @Override
    protected Byte convert(LootTier source) {
        return source.tierId;
    }
}