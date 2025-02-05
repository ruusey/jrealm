package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableInt;
import com.jrealm.net.core.nettypes.SerializableString;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Streamable
public class NetGameItem extends SerializableFieldType<NetGameItem> {
	@SerializableField(order = 0, type = SerializableInt.class)
    private Integer itemId;
	@SerializableField(order = 1, type = SerializableString.class)
    private String uid;
	@SerializableField(order = 2, type = SerializableString.class)
    private String name;
	@SerializableField(order = 3, type = SerializableString.class)
    private String description;
	@SerializableField(order = 4, type = NetStats.class)
    private NetStats stats;
	@SerializableField(order = 5, type = NetDamage.class)
    private NetDamage damage;
	@SerializableField(order = 6, type = NetEffect.class)
    private NetEffect effect;
	@SerializableField(order = 7, type = SerializableBoolean.class)
    private Boolean consumable;
	@SerializableField(order = 8, type = SerializableByte.class)
    private Byte tier;
	@SerializableField(order = 9, type = SerializableByte.class)
    private Byte targetSlot;
	@SerializableField(order = 10, type = SerializableByte.class)
    private Byte targetClass;
	@SerializableField(order = 11, type = SerializableByte.class)
    private Byte fameBonus;
	
	
	
	@Override
	public NetGameItem read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public void write(NetGameItem value, DataOutputStream stream) throws Exception {
		if(value==null) {
			 IOService.writeStream(new NetGameItem(), stream);
		}else {
		    IOService.writeStream(value, stream);
		}
	}

	public NetGameItem() {
		this.itemId = -1;
		this.uid = "";
		
		this.name = "";
		this.description = "";
		this.stats = new NetStats();
		this.damage = new NetDamage();
		this.effect = new NetEffect();
		this.consumable = false;
		this.tier = -2;
		this.targetSlot = -1;
		this.targetClass = -1;
		this.fameBonus = -1;
	}
}
