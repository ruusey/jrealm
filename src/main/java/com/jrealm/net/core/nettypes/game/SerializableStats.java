package com.jrealm.net.core.nettypes.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.item.Stats;
import com.jrealm.net.core.SerializableFieldType;

public class SerializableStats extends SerializableFieldType<Stats> {

	@Override
	public Stats read(DataInputStream stream) throws Exception {
        short att = stream.readShort();
        short def = stream.readShort();

        short dex = stream.readShort();
        short spd = stream.readShort();

        short vit = stream.readShort();
        short wis = stream.readShort();

        short mp = stream.readShort();
        short hp = stream.readShort();

        return new Stats(hp, mp, def, att, spd, dex, vit, wis);
	}

	@Override
	public void write(Stats value, DataOutputStream stream) throws Exception {
        stream.writeShort(value.getAtt());
        stream.writeShort(value.getDef());

        stream.writeShort(value.getSpd());
        stream.writeShort(value.getDex());

        stream.writeShort(value.getVit());
        stream.writeShort(value.getWis());

        stream.writeShort(value.getMp());
        stream.writeShort(value.getHp());
	}
}
