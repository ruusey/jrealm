package com.jrealm.net.core.nettypes.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.item.Damage;
import com.jrealm.game.entity.item.Effect;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.net.core.SerializableFieldType;

public class SerializableGameItem extends SerializableFieldType<GameItem> {

	@Override
	public GameItem read(DataInputStream stream) throws Exception {
		int itemId = stream.readInt();
		if (itemId == -1) {
			return null;
		}
		String uid = stream.readUTF();
		String name = stream.readUTF();
		String description = stream.readUTF();
		boolean hasStats = stream.readBoolean();
		Stats stats = null;

		if (hasStats) {
			try {
				stats = new Stats().read(stream);
			} catch (Exception e) {
				// log.error("Failed to get stats, no stats present");
			}
		}

		boolean hasDamage = stream.readBoolean();

		Damage damage = null;
		if (hasDamage) {
			try {
				damage = new Damage().read(stream);
			} catch (Exception e) {
				// log.error("Failed to get damage, no damage present");
			}
		}

		boolean hasEffect = stream.readBoolean();
		Effect effect = null;

		if (hasEffect) {
			try {
				effect = new Effect().read(stream);
			} catch (Exception e) {
				// log.error("Failed to get effect, no effect present");
			}
		}

		boolean consumable = stream.readBoolean();
		byte tier = stream.readByte();
		byte targetSlot = stream.readByte();
		byte targetClass = stream.readByte();
		byte fameBonus = stream.readByte();

		return new GameItem(itemId, uid, name, description, stats, damage, effect, consumable, tier, targetSlot,
				targetClass, fameBonus);
	}

	@Override
	public void write(GameItem value, DataOutputStream stream) throws Exception {
	    stream.writeInt(value.getItemId());
        stream.writeUTF(value.getUid());
        stream.writeUTF(value.getName());
        stream.writeUTF(value.getDescription());
        if (value.getStats() != null) {
            stream.writeBoolean(true);
            value.getStats().write(stream);
        } else {
            stream.writeBoolean(false);
        }

        if (value.getDamage() != null) {
            stream.writeBoolean(true);
            value.getDamage().write(stream);
        } else {
            stream.writeBoolean(false);
        }

        if (value.getEffect() != null) {
            stream.writeBoolean(true);
            value.getEffect().write(stream);
        } else {
            stream.writeBoolean(false);
        }

        stream.writeBoolean(value.isConsumable());
        stream.writeByte(value.getTier());
        stream.writeByte(value.getTargetSlot());
        stream.writeByte(value.getTargetClass());
        stream.writeByte(value.getFameBonus());

	}

}
