package com.jrealm.game.entity.item;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.jrealm.game.contants.LootTier;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.realm.Realm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LootContainer {

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
        this.items[0] = GameDataManager.GAME_ITEMS.get(Realm.RANDOM.nextInt(8));
        for (int i = 1; i < (Realm.RANDOM.nextInt(7) + 1); i++) {
            this.items[i] = GameDataManager.GAME_ITEMS.get(Realm.RANDOM.nextInt(152) + 1);
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
        this.spawnedTime = Instant.now().toEpochMilli();
        if (this.hasUntieredItem()) {
            this.tier = LootTier.WHITE;
        }
    }

    public LootContainer(LootTier tier, Vector2f pos, GameItem[] loot) {
        this.tier = tier;
        this.sprite = LootTier.getLootSprite(tier.tierId);
        this.pos = pos;
        this.uid = UUID.randomUUID().toString();
        this.items = Arrays.copyOf(loot, 8);
        this.spawnedTime = Instant.now().toEpochMilli();
        if (this.hasUntieredItem()) {
            this.tier = LootTier.WHITE;
        }
    }

    public boolean isExpired() {
        return (Instant.now().toEpochMilli() - this.spawnedTime) > 45000;
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
        for (int i = 0; i < items.length; i++) {
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

    public void render(SpriteBatch batch) {
        if (this.sprite != null && this.sprite.getRegion() != null) {
            batch.draw(this.sprite.getRegion(), this.pos.getWorldVar().x, this.pos.getWorldVar().y, 32, 32);
        }
    }

    public int getNonEmptySlotCount() {
        int count = 0;
        for (GameItem s : this.getItems()) {
            if (s != null) {
                count++;
            }
        }
        return count;
    }

    public static GameItem[] getCondensedItems(LootContainer container) {
        final List<GameItem> items = new ArrayList<>();
        for (GameItem item : container.getItems()) {
            if (item != null) {
                items.add(item);
            }
        }
        return items.toArray(new GameItem[0]);
    }

    public boolean equals(LootContainer other) {
    	final GameItem[] thisLoot = LootContainer.getCondensedItems(this);
    	final GameItem[] otherLoot = LootContainer.getCondensedItems(other);
    	final boolean basic = (this.lootContainerId == other.getLootContainerId()) && this.pos.equals(other.getPos());
    	final boolean tier = this.getTier().equals(other.getTier());
    	boolean loot = (thisLoot.length == otherLoot.length);
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
}
