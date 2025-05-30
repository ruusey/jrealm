package com.jrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.jrealm.game.contants.EffectType;
public class ShortToEffectTypeConverter extends AbstractConverter<EffectType, Short> {

    @Override
    protected Short convert(EffectType source) {
        return source.effectId;
    }
}