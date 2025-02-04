package com.jrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.jrealm.game.contants.EffectType;
public class EffectTypeToShortConverter extends AbstractConverter<Short, EffectType> {

    @Override
    protected EffectType convert(Short source) {
        return EffectType.valueOf(source);
    }
}