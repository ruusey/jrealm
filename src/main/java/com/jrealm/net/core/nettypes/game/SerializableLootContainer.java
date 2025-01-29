package com.jrealm.net.core.nettypes.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.LootTier;
import com.jrealm.game.entity.item.Chest;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.core.SerializableFieldType;

public class SerializableLootContainer extends SerializableFieldType<LootContainer>{

	@Override
	public LootContainer read(DataInputStream stream) throws Exception {
        final long lootContainerId = stream.readLong();
        final String uid = stream.readUTF();
        final boolean isChest = stream.readBoolean();
        final byte tier = stream.readByte();
        final int itemsSize = stream.readInt();
        final GameItem[] items = new GameItem[8];
        for (int i = 0; i < itemsSize; i++) {
            items[i] = new GameItem().read(stream);
        }
        final float posX = stream.readFloat();
        final float posY = stream.readFloat();

        final long spawnedTime = stream.readLong();
        final boolean contentsChanged = stream.readBoolean();
        final LootContainer container = LootContainer.builder().lootContainerId(lootContainerId).tier(LootTier.valueOf(tier))
                .uid(uid).items(items).pos(new Vector2f(posX, posY)).spawnedTime(spawnedTime)
                .contentsChanged(contentsChanged).sprite(null).build();
        if (isChest) {
        	final Chest chest = new Chest(container);
            chest.setPos(new Vector2f(posX, posY));
            return chest;
        }
        return container;
	}

	@Override
	public void write(LootContainer value, DataOutputStream stream) throws Exception {
        stream.writeLong(value.getLootContainerId());
        stream.writeUTF(value.getUid());
        stream.writeBoolean(value instanceof Chest);
        stream.writeByte(value.getTier().tierId);
        final GameItem[] toWrite = LootContainer.getCondensedItems(value);
        stream.writeInt(toWrite.length);
        for (int i = 0; i < toWrite.length; i++) {
            toWrite[i].write(stream);
        }

        stream.writeFloat(value.getPos().x);
        stream.writeFloat(value.getPos().y);
        stream.writeLong(value.getSpawnedTime());
        stream.writeBoolean(value.getContentsChanged());
		
	}

}
