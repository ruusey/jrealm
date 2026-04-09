package com.jrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.jrealm.game.contants.StatusEffectType;
public class ShortToEffectTypeConverter extends AbstractConverter<StatusEffectType, Short> {

    @Override
    protected Short convert(StatusEffectType source) {
        return source.effectId;
    }
}