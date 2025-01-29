package com.jrealm.net.core.nettypes.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.EffectType;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.net.core.SerializableFieldType;

public class SerializableEffect extends SerializableFieldType<Effect>{

	@Override
	public Effect read(DataInputStream stream) throws Exception {
        final boolean self = stream.readBoolean();
        final short effectId = stream.readShort();
        final long duration = stream.readLong();
        final long cooldownDuration = stream.readLong();
        final short mpCost = stream.readShort();
        return new Effect(self, EffectType.valueOf(effectId), duration, cooldownDuration, mpCost);
	}

	@Override
	public void write(Effect value, DataOutputStream stream) throws Exception {
        stream.writeBoolean(value.isSelf());
        stream.writeShort(value.getEffectId().effectId);
        stream.writeLong(value.getDuration());
        stream.writeLong(value.getCooldownDuration());
        stream.writeShort(value.getMpCost());
	}
}
