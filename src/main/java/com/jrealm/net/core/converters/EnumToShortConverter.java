package com.jrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.jrealm.game.contants.EffectType;
public class EnumToShortConverter extends AbstractConverter<Short, EffectType> {

    @Override
    protected EffectType convert(Short source) {
        return EffectType.valueOf(source);
    }
}