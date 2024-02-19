package com.jrealm.game.entity.item;

import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.jrealm.game.contants.LootTier;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Streamable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LootContainer implements Streamable<LootContainer> {

	private long lootContainerId;
	private LootTier tier;
	private Sprite sprite;
	private String uid;
	private GameItem[] items;
	private Vector2f pos;

	private long spawnedTime;

	private boolean contentsChanged;

	public LootContainer(LootTier tier, Vector2f pos) {
		this.tier = tier;
		this.sprite = LootTier.getLootSprite(tier.tierId);
		this.uid = UUID.randomUUID().toString();
		this.items = new GameItem[8];
		this.pos = pos;
		Random r = new Random(System.nanoTime());
		this.items[0] = GameDataManager.GAME_ITEMS.get(r.nextInt(8));
		for (int i = 1; i < (r.nextInt(7) + 1); i++) {
			this.items[i] = GameDataManager.GAME_ITEMS.get(r.nextInt(152) + 1);
		}
		this.spawnedTime = System.currentTimeMillis();
		if (this.hasUntieredItem()) {
			this.tier = LootTier.WHITE;
		}
	}

	public boolean getContentsChanged() {
		return this.contentsChanged;
	}

	public LootContainer(LootTier tier, Vector2f pos, GameItem loot) {
		this.tier = tier;
		this.sprite = LootTier.getLootSprite(tier.tierId);
		this.pos = pos;
		this.uid = UUID.randomUUID().toString();
		this.items = new GameItem[8];
		this.items[0] = loot;
		this.spawnedTime = System.currentTimeMillis();
		if (this.hasUntieredItem()) {
			this.tier = LootTier.WHITE;
		}
	}

	public LootContainer(LootTier tier, Vector2f pos, GameItem[] loot) {
		this.tier = tier;
		this.sprite = LootTier.getLootSprite(tier.tierId);
		this.pos = pos;
		this.uid = UUID.randomUUID().toString();
		this.items = loot;
		this.spawnedTime = System.currentTimeMillis();
		if (this.hasUntieredItem()) {
			this.tier = LootTier.WHITE;
		}
	}

	public boolean isExpired() {
		return (System.currentTimeMillis() - this.spawnedTime) > 60000;
	}

	public boolean isEmpty() {
		for (GameItem item : this.items) {
			if (item != null)
				return false;
		}
		return true;
	}

	public boolean hasUntieredItem() {
		if (this.tier.equals(LootTier.CHEST))
			return false;
		for (GameItem item : this.items) {
			if ((item != null) && (item.getTier() == (byte) -1))
				return true;
		}
		return false;
	}

	public void setItems(GameItem[] items) {
		this.items = items;
		this.contentsChanged = true;
	}

	public void setItemsUncondensed(GameItem[] items) {
		this.items = new GameItem[8];
		for(int i = 0 ; i<items.length; i++) {
			this.items[i] = items[i];
		}
		this.contentsChanged = true;
	}

	public void setItem(int idx, GameItem replacement) {
		this.items[idx] = replacement;
		this.contentsChanged = true;
	}

	public int getFirstNullIdx() {
		int idx = -1;
		for (int i = 0; i < this.items.length; i++) {
			if (this.items[i] == null) {
				idx = i;
				return idx;
			}
		}
		return idx;
	}

	public void render(Graphics2D g) {
		g.drawImage(this.sprite.image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y), 32, 32,
				null);
	}

	@Override
	public void write(DataOutputStream stream) throws Exception {
		stream.writeLong(this.getLootContainerId());
		stream.writeUTF(this.getUid());
		stream.writeBoolean(this instanceof Chest);
		stream.writeByte(this.getTier().tierId);
		GameItem[] toWrite = LootContainer.getCondensedItems(this);
		stream.writeInt(toWrite.length);
		for (int i = 0; i < toWrite.length; i++) {
			toWrite[i].write(stream);
		}

		stream.writeFloat(this.pos.x);
		stream.writeFloat(this.pos.y);
		stream.writeLong(this.spawnedTime);
		stream.writeBoolean(this.contentsChanged);
	}

	@Override
	public LootContainer read(DataInputStream stream) throws Exception {

		long lootContainerId = stream.readLong();
		String uid = stream.readUTF();
		boolean isChest = stream.readBoolean();
		byte tier = stream.readByte();
		int itemsSize = stream.readInt();
		GameItem[] items = new GameItem[8];
		for (int i = 0; i < itemsSize; i++) {
			items[i] = new GameItem().read(stream);
		}
		float posX = stream.readFloat();
		float posY = stream.readFloat();

		long spawnedTime = stream.readLong();
		boolean contentsChanged = stream.readBoolean();
		LootContainer container = LootContainer.builder().lootContainerId(lootContainerId).tier(LootTier.valueOf(tier)).uid(uid).items(items)
				.pos(new Vector2f(posX, posY)).spawnedTime(spawnedTime).contentsChanged(contentsChanged)
				.sprite(null).build();
		if(isChest) {
			Chest chest = new Chest(container);
			chest.setPos(new Vector2f(posX, posY));
			return chest;
		}
		return container;

	}

	public static GameItem[] getCondensedItems(LootContainer container) {
		List<GameItem> items = new ArrayList<>();
		for(GameItem item : container.getItems()) {
			if(item!=null) {
				items.add(item);
			}
		}
		return items.toArray(new GameItem[0]);
	}

	public boolean equals(LootContainer other) {
		GameItem[] thisLoot = LootContainer.getCondensedItems(this);
		GameItem[] otherLoot = LootContainer.getCondensedItems(other);
		boolean basic = (this.lootContainerId == other.getLootContainerId())  && this.pos.equals(other.getPos());
		boolean loot = thisLoot.length == otherLoot.length;
		boolean tier = this.getTier().equals(other.getTier());
		if(loot) {
			for(int i = 0; i < thisLoot.length; i++) {
				if(!thisLoot[i].equals(otherLoot[i])) {
					loot = false;
					break;
				}
			}
		}
		return basic && loot && tier;
	}
}
