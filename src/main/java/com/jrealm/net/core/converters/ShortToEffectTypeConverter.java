package com.jrealm.net.core.converters;

import org.modelmapper.AbstractConverter;
import com.jrealm.game.contants.ProjectileEffectType;
public class ShortToEffectTypeConverter extends AbstractConverter<ProjectileEffectType, Short> {

    @Override
    protected Short convert(ProjectileEffectType source) {
        return source.effectId;
    }
}