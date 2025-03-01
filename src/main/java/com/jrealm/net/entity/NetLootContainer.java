package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.jrealm.game.contants.LootTier;
import com.jrealm.game.entity.item.Chest;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableBoolean;
import com.jrealm.net.core.nettypes.SerializableByte;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableString;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Streamable
@AllArgsConstructor
@NoArgsConstructor
public class NetLootContainer extends SerializableFieldType<NetLootContainer>{
	@SerializableField(order = 0, type = SerializableLong.class)
    private long lootContainerId;
	@SerializableField(order = 1, type = SerializableString.class)
    private String uid;
	@SerializableField(order = 2, type = SerializableBoolean.class)
    private boolean isChest;
	@SerializableField(order = 3, type = SerializableByte.class)
    private byte tier;
	@SerializableField(order = 4, type = NetGameItem.class, isCollection = true)
    private NetGameItem[] items;
	@SerializableField(order = 5, type = Vector2f.class)
    private Vector2f pos;
	@SerializableField(order = 6, type = SerializableLong.class)
    private long spawnedTime;
	@SerializableField(order = 7, type = SerializableBoolean.class)
    private boolean contentsChanged;
	
	@Override
	public NetLootContainer read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}
	@Override
	public int write(NetLootContainer value, DataOutputStream stream) throws Exception {
		return IOService.writeStream(value, stream);
	}
	
	public LootContainer asLootContainer() {
		LootContainer container = new LootContainer();
		container.setLootContainerId(this.lootContainerId);
		container.setUid(this.uid);
		container.setTier(LootTier.valueOf(this.tier));
		GameItem[] itemsMapped = new GameItem[items.length];	
		for(int i = 0 ; i < items.length ; i++) {
			
			itemsMapped[i] = this.items[i] == null ? null : this.items[i].asGameItem();
		}
		container.setItems(itemsMapped);
		container.setPos(this.pos);
		container.setSpawnedTime(this.spawnedTime);
		container.setContentsChanged(this.contentsChanged);
		
		if(this.isChest) {
			container = (Chest) container;
		}
		return container;
	}
	
    public static NetGameItem[] getCondensedItems(NetLootContainer container) {
        List<NetGameItem> items = new ArrayList<>();
        for (NetGameItem item : container.getItems()) {
            if (item != null) {
                items.add(item);
            }
        }
        return items.toArray(new NetGameItem[0]);
    }
    
    public boolean equals(NetLootContainer other) {
    	NetGameItem[] thisLoot = NetLootContainer.getCondensedItems(this);
    	NetGameItem[] otherLoot = NetLootContainer.getCondensedItems(other);
        boolean basic = (this.lootContainerId == other.getLootContainerId()) && this.pos.equals(other.getPos());
        boolean loot = thisLoot.length == otherLoot.length;
        boolean tier = this.getTier()==other.getTier();
        if (loot) {
            for (int i = 0; i < thisLoot.length; i++) {
                if (!thisLoot[i].equals(otherLoot[i])) {
                    loot = false;
                    break;
                }
            }
        }
        return basic && loot && tier;
    }
   
    
//	@Override
//	public LootContainer read(DataInputStream stream) throws Exception {
//        final long lootContainerId = stream.readLong();
//        final String uid = stream.readUTF();
//        final boolean isChest = stream.readBoolean();
//        final byte tier = stream.readByte();
//        final int itemsSize = stream.readInt();
//        final GameItem[] items = new GameItem[8];
//        for (int i = 0; i < itemsSize; i++) {
//            items[i] = new GameItem().read(stream);
//        }
//        final float posX = stream.readFloat();
//        final float posY = stream.readFloat();
//
//        final long spawnedTime = stream.readLong();
//        final boolean contentsChanged = stream.readBoolean();
//        final LootContainer container = LootContainer.builder().lootContainerId(lootContainerId).tier(LootTier.valueOf(tier))
//                .uid(uid).items(items).pos(new Vector2f(posX, posY)).spawnedTime(spawnedTime)
//                .contentsChanged(contentsChanged).sprite(null).build();
//        if (isChest) {
//        	final Chest chest = new Chest(container);
//            chest.setPos(new Vector2f(posX, posY));
//            return chest;
//        }
//        return container;
//	}
//
//	@Override
//	public void write(LootContainer value, DataOutputStream stream) throws Exception {
//        stream.writeLong(value.getLootContainerId());
//        stream.writeUTF(value.getUid());
//        stream.writeBoolean(value instanceof Chest);
//        stream.writeByte(value.getTier().tierId);
//        final GameItem[] toWrite = LootContainer.getCondensedItems(value);
//        stream.writeInt(toWrite.length);
//        for (int i = 0; i < toWrite.length; i++) {
//            toWrite[i].write(stream);
//        }
//
//        stream.writeFloat(value.getPos().x);
//        stream.writeFloat(value.getPos().y);
//        stream.writeLong(value.getSpawnedTime());
//        stream.writeBoolean(value.getContentsChanged());
//		
//	}

}
