package com.jrealm.net.entity;

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

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
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
	@SerializableField(order = 4, type = NetStats.class)
	private NetStats stats;
	@SerializableField(order = 5, type = NetDamage.class)
	private NetDamage damage;
	@SerializableField(order = 6, type = NetEffect.class)
	private NetEffect effect;
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
	public int write(NetGameItem value, DataOutputStream stream) throws Exception {
		final NetGameItem toWrite = (value == null ? new NetGameItem() : value);
		return IOService.writeStream(toWrite, stream);
	}

	public GameItem asGameItem() {
		GameItem item = new GameItem();
		item.setItemId(this.itemId);
		item.setUid(this.uid);
		item.setName(this.name);
		item.setDescription(this.description);
		item.setStats(IOService.mapModel(this.stats, Stats.class));
		item.setDamage(IOService.mapModel(this.damage, Damage.class));
		item.setEffect(IOService.mapModel(this.stats, Effect.class));
		item.setConsumable(this.consumable);
		item.setTier(this.tier);
		item.setTargetSlot(this.targetSlot);
		item.setTargetClass(this.targetClass);
		item.setFameBonus(this.fameBonus);
		return item;
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
