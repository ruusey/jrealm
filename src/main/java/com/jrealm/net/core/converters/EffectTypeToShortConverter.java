package com.jrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.jrealm.game.contants.ProjectileEffectType;
public class EffectTypeToShortConverter extends AbstractConverter<Short, ProjectileEffectType> {

    @Override
    protected ProjectileEffectType convert(Short source) {
        return ProjectileEffectType.valueOf(source);
    }
}