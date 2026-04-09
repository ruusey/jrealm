package com.jrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.jrealm.game.contants.StatusEffectType;
public class EffectTypeToShortConverter extends AbstractConverter<Short, StatusEffectType> {

    @Override
    protected StatusEffectType convert(Short source) {
        return StatusEffectType.valueOf(source);
    }
}