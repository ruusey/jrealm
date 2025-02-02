package com.jrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.jrealm.game.contants.EffectType;
public class ShortToEnumConverter extends AbstractConverter<EffectType, Short> {

    @Override
    protected Short convert(EffectType source) {
        return source.effectId;
    }
}