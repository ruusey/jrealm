package com.jrealm.game.entity.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.UUID;

import com.jrealm.account.dto.GameItemRefDto;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.model.SpriteModel;
import com.jrealm.net.Streamable;

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
public class GameItem extends SpriteModel implements Streamable<GameItem> {
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
                log.error("Failed to get stats, no stats present");
            }
        }

        boolean hasDamage = stream.readBoolean();

        Damage damage = null;
        if (hasDamage) {
            try {
                damage = new Damage().read(stream);
            } catch (Exception e) {
                log.error("Failed to get damage, no damage present");
            }
        }

        boolean hasEffect = stream.readBoolean();
        Effect effect = null;

        if (hasEffect) {
            try {
                effect = new Effect().read(stream);
            } catch (Exception e) {
                log.error("Failed to get effect, no effect present");
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
    public void write(DataOutputStream stream) throws Exception {
        stream.writeInt(this.itemId);
        stream.writeUTF(this.uid);
        stream.writeUTF(this.name);
        stream.writeUTF(this.description);
        if (this.stats != null) {
            stream.writeBoolean(true);
            this.stats.write(stream);
        } else {
            stream.writeBoolean(false);
        }

        if (this.damage != null) {
            stream.writeBoolean(true);
            this.damage.write(stream);
        } else {
            stream.writeBoolean(false);
        }

        if (this.effect != null) {
            stream.writeBoolean(true);
            this.effect.write(stream);
        } else {
            stream.writeBoolean(false);
        }

        stream.writeBoolean(this.consumable);
        stream.writeByte(this.tier);
        stream.writeByte(this.targetSlot);
        stream.writeByte(this.targetClass);
        stream.writeByte(this.fameBonus);
    }

    public void applySpriteModel(final SpriteModel model) {
        this.setRow(model.getRow());
        this.setCol(model.getCol());
        this.setAngleOffset(model.getAngleOffset());
        this.setSpriteKey(model.getSpriteKey());
    }

    public static GameItem fromGameItemRef(final GameItemRefDto gameItem) {
        GameItem item = GameDataManager.GAME_ITEMS.get(gameItem.getItemId());
        item.setUid(gameItem.getItemUuid());
        GameDataManager.loadSpriteModel(item);
        return item;
    }
}
