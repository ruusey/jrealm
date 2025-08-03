package com.jrealm.game.entity.item;

import java.util.UUID;

import com.jrealm.account.dto.GameItemRefDto;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.model.SpriteModel;
import com.jrealm.net.entity.NetGameItemRef;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@Builder
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class GameItem extends SpriteModel {
    private int itemId;
    @Builder.Default
    private String uid = UUID.randomUUID().toString();
    private String name;
    private String description;
    private Stats stats;
    private Damage damage;
    private Effect effect;
    private boolean consumable;
    private byte tier;
    private byte targetSlot;
    private byte targetClass;
    private byte fameBonus;

    public GameItem() {
        this.uid = UUID.randomUUID().toString();
    }

    @Override
    public GameItem clone() {
        GameItem.GameItemBuilder builder = GameItem.builder().itemId(this.itemId).uid(this.uid).name(this.name)
                .description(this.description).consumable(this.consumable).tier(this.tier).targetSlot(this.targetSlot)
                .targetClass(this.targetClass).fameBonus(this.fameBonus);

        if (this.damage != null) {
            builder = builder.damage(this.damage.clone());
        }

        if (this.stats != null) {
            builder = builder.stats(this.stats.clone());
        }

        GameItem itemFinal = builder.build();
        itemFinal.setAngleOffset(this.getAngleOffset());
        itemFinal.setRow(this.getRow());
        itemFinal.setCol(this.getCol());
        itemFinal.setSpriteKey(this.getSpriteKey());

        return itemFinal;
    }

    public void applySpriteModel(final SpriteModel model) {
        this.setRow(model.getRow());
        this.setCol(model.getCol());
        this.setAngleOffset(model.getAngleOffset());
        this.setSpriteKey(model.getSpriteKey());
    }
    
    public GameItemRefDto toGameItemRefDto(int idx) {
    	return new GameItemRefDto(this.itemId, idx, this.uid);
    }
    
    public NetGameItemRef asNetGameItemRef(int idx) {
    	return new NetGameItemRef(itemId, idx, this.uid);
    }

    public static GameItem fromGameItemRef(final GameItemRefDto gameItem) {
        GameItem item = GameDataManager.GAME_ITEMS.get(gameItem.getItemId());
        item.setUid(gameItem.getItemUuid());
        GameDataManager.loadSpriteModel(item);
        return item;
    }
    
    public static GameItem fromGameItemRef(final NetGameItemRef gameItem) {
        GameItem item = GameDataManager.GAME_ITEMS.get(gameItem.getItemId());
        item.setUid(gameItem.getItemUuid());
        GameDataManager.loadSpriteModel(item);
        return item;
    }
    
}
