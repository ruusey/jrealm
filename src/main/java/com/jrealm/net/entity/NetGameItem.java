package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

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

	private static final NetStats STATS_SERIALIZER = new NetStats();
	private static final NetDamage DAMAGE_SERIALIZER = new NetDamage();
	private static final NetEffect EFFECT_SERIALIZER = new NetEffect();

	private static int writeString(String s, DataOutputStream stream) throws Exception {
		if (s == null) s = "";
		final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		stream.writeInt(bytes.length);
		stream.write(bytes);
		return 4 + bytes.length;
	}

	private static String readString(DataInputStream stream) throws Exception {
		final int len = stream.readInt();
		if (len <= 0) return "";
		final byte[] bytes = new byte[len];
		stream.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	/** Hand-coded write bypassing IOService reflection */
	@Override
	public int write(NetGameItem value, DataOutputStream stream) throws Exception {
		final NetGameItem v = (value == null ? new NetGameItem() : value);
		int written = 0;
		stream.writeInt(v.itemId); written += 4;
		written += writeString(v.uid, stream);
		written += writeString(v.name, stream);
		written += writeString(v.description, stream);
		written += STATS_SERIALIZER.write(v.stats, stream);
		written += DAMAGE_SERIALIZER.write(v.damage, stream);
		written += EFFECT_SERIALIZER.write(v.effect, stream);
		stream.writeBoolean(v.consumable); written += 1;
		stream.writeByte(v.tier); written += 1;
		stream.writeByte(v.targetSlot); written += 1;
		stream.writeByte(v.targetClass); written += 1;
		stream.writeByte(v.fameBonus); written += 1;
		return written;
	}

	/** Hand-coded read bypassing IOService reflection */
	@Override
	public NetGameItem read(DataInputStream stream) throws Exception {
		final NetGameItem item = new NetGameItem();
		item.itemId = stream.readInt();
		item.uid = readString(stream);
		item.name = readString(stream);
		item.description = readString(stream);
		item.stats = STATS_SERIALIZER.read(stream);
		item.damage = DAMAGE_SERIALIZER.read(stream);
		item.effect = EFFECT_SERIALIZER.read(stream);
		item.consumable = stream.readBoolean();
		item.tier = stream.readByte();
		item.targetSlot = stream.readByte();
		item.targetClass = stream.readByte();
		item.fameBonus = stream.readByte();
		return item;
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
