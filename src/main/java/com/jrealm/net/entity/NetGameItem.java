package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.item.Damage;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableString;
import com.jrealm.net.core.nettypes.game.SerializableDamage;
import com.jrealm.net.core.nettypes.game.SerializableStats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Streamable
public class NetGameItem extends SerializableFieldType<NetGameItem> {
	@SerializableField(order = 0, type = SerializableInt.class)
    private int itemId;
	@SerializableField(order = 1, type = SerializableString.class)
    private String uid;
	@SerializableField(order = 2, type = SerializableString.class)
    private String name;
	@SerializableField(order = 3, type = SerializableString.class)
    private String description;
	@SerializableField(order = 4, type = SerializableStats.class)
    private Stats stats;
	@SerializableField(order = 5, type = SerializableDamage.class)
    private Damage damage;
	@SerializableField(order = 6, type = SerializableString.class)
    private Effect effect;
	@SerializableField(order = 7, type = SerializableBoolean.class)
    private boolean consumable;
	@SerializableField(order = 8, type = SerializableByte.class)
    private byte tier;
	@SerializableField(order = 9, type = SerializableByte.class)
    private byte targetSlot;
	@SerializableField(order = 10, type = SerializableByte.class)
    private byte targetClass;
	@SerializableField(order = 11, type = SerializableByte.class)
    private byte fameBonus;
	
	
	@Override
	public NetGameItem read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetGameItem value, DataOutputStream stream) throws Exception {
	    IOService.writeStream(value, stream);
	}
}
