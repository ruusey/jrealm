package com.jrealm.net.core.nettypes.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.item.Damage;
import com.jrealm.net.core.SerializableFieldType;

public class SerializableDamage extends SerializableFieldType<Damage>{

	@Override
	public Damage read(DataInputStream stream) throws Exception {
        final int projectileGroupId = stream.readInt();
        final short min = stream.readShort();
        final short max = stream.readShort();
        return new Damage(projectileGroupId, min, max);
	}

	@Override
	public void write(Damage value, DataOutputStream stream) throws Exception {
        stream.writeInt(value.getProjectileGroupId());
        stream.writeShort(value.getMin());
        stream.writeShort(value.getMax());
	}
}
