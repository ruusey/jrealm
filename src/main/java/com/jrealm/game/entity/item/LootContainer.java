package com.jrealm.game.entity.item;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.time.Instant;
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
        this.tier = this.determineTier();
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
        this.tier = this.determineTier();
    }

    public LootContainer(LootTier tier, Vector2f pos, GameItem[] loot) {
        this.tier = tier;
        this.sprite = LootTier.getLootSprite(tier.tierId);
        this.pos = pos;
        this.uid = UUID.randomUUID().toString();
        // Pack items contiguously from slot 0 with no gaps.
        // Arrays.copyOf(loot, 8) would leave nulls between items if the
        // source had gaps; instead, filter nulls and pack to the front.
        this.items = new GameItem[8];
        int slot = 0;
        for (GameItem item : loot) {
            if (item != null && slot < 8) {
                this.items[slot++] = item;
            }
        }
        this.spawnedTime = Instant.now().toEpochMilli();
        this.tier = this.determineTier();
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
        if (this.tier.equals(LootTier.CHEST) || this.tier.equals(LootTier.GRAVE))
            return false;
        for (GameItem item : this.items) {
            if ((item != null) && (item.getTier() == (byte) -1))
                return true;
        }
        return false;
    }

    /**
     * Determine the appropriate loot tier based on the items inside.
     * WHITE(4): any untiered item (tier -1)
     * BLUE(3): only potions (consumable items)
     * CYAN(2): any item tier 8+
     * PURPLE(1): tiered items 0-7
     * BROWN(0): fallback / empty
     * CHEST and GRAVE are never reclassified.
     */
    public LootTier determineTier() {
        if (this.tier.equals(LootTier.CHEST) || this.tier.equals(LootTier.GRAVE))
            return this.tier;

        boolean hasUntiered = false;
        boolean hasHighTier = false; // tier 8+
        boolean hasLowTier = false;  // tier 0-7, non-consumable
        boolean hasPotion = false;
        boolean hasAnyItem = false;

        for (GameItem item : this.items) {
            if (item == null) continue;
            hasAnyItem = true;
            byte t = item.getTier();
            if (t == (byte) -1) {
                hasUntiered = true;
            } else if (item.isConsumable()) {
                hasPotion = true;
            } else if (t >= 8) {
                hasHighTier = true;
            } else {
                hasLowTier = true;
            }
        }

        if (!hasAnyItem) return LootTier.BROWN;
        if (hasUntiered) return LootTier.WHITE;
        if (hasHighTier) return LootTier.CYAN;
        if (hasLowTier) return LootTier.PURPLE;
        if (hasPotion) return LootTier.BLUE;
        return LootTier.BROWN;
    }

    public void setItems(GameItem[] items) {
        this.items = items;
        this.contentsChanged = true;
    }

    /**
     * Re-pack items to fill gaps (nulls) left by removed items.
     * After this call, all non-null items are contiguous from slot 0.
     */
    public void repackItems() {
        GameItem[] packed = new GameItem[8];
        int slot = 0;
        for (GameItem item : this.items) {
            if (item != null && slot < 8) {
                packed[slot++] = item;
            }
        }
        this.items = packed;
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

    public boolean equals(LootContainer other) {
    	final boolean basic = (this.lootContainerId == other.getLootContainerId()) && this.pos.equals(other.getPos());
    	final boolean tierMatch = this.getTier().equals(other.getTier());
    	boolean loot = true;
        for (int i = 0; i < 8; i++) {
            final GameItem a = this.items[i];
            final GameItem b = other.getItems()[i];
            if (a == null && b == null) continue;
            if (a == null || b == null || !a.equals(b)) {
                loot = false;
                break;
            }
        }
        return basic && loot && tierMatch;
    }
}
