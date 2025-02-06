package com.jrealm.game.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.jrealm.game.GamePanel;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.ItemTooltip;
import com.jrealm.game.state.PlayState;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;
import com.jrealm.net.server.packet.TextPacket;

import lombok.Data;

@Data
public class PlayerUI {
    private FillBars hp;
    private FillBars mp;
    private FillBars xp;

    private Slots[] inventory;
    private Slots[] groundLoot;

    private PlayState playState;
    private PlayerChat playerChat;
    private Minimap minimap;
    private Map<String, ItemTooltip> tooltips;
    private Graphics2D tempGraphics;
    private long lastAction = Instant.now().toEpochMilli();
    private Button menuButton = null;
    public PlayerUI(PlayState p) {
        SpriteSheet bars = new SpriteSheet("fillbars.png", 12, 12);
        BufferedImage[] barSpritesHp = { bars.cropImage(12, 2, 7, 16), bars.cropImage(39, 0, 7, 14),
                bars.cropImage(0, 0, 12, 20) };
        BufferedImage[] barSpritesMp = { bars.cropImage(12, 2, 7, 16), bars.cropImage(39, 16, 7, 14),
                bars.cropImage(0, 0, 12, 20) };

        BufferedImage[] barSpritesXp = { bars.cropImage(12, 2, 7, 16), bars.cropImage(59, 0, 7, 14),
                bars.cropImage(0, 0, 12, 20) };

        Vector2f posHp = new Vector2f(GamePanel.width - 356, 128);
        Vector2f posMp = posHp.clone(0, 32);
        Vector2f posXp = posHp.clone(0, -32);

        this.playState = p;
        this.hp = new FillBars(p.getPlayer(), barSpritesHp, posHp, 16, 16, "getHealthPercent");
        this.mp = new FillBars(p.getPlayer(), barSpritesMp, posMp, 16, 16, "getManaPercent");
        this.xp = new FillBars(p.getPlayer(), barSpritesXp, posXp, 16, 16, "getExperiencePercent");
        this.groundLoot = new Slots[8];
        this.inventory = new Slots[20];
        this.tooltips = new HashMap<>();
        this.playerChat = new PlayerChat(p);
        this.minimap = new Minimap(p);
    }

    public Slots getSlot(int slot) {
        return this.inventory[slot];
    }

    public Slots[] getSlots(int start, int end) {
        int size = end - start;
        int idx = 0;
        Slots[] items = new Slots[size];
        for (int i = start; i < end; i++) {
            items[idx++] = this.inventory[i];
        }
        return items;
    }

    public int firstNullIdx(GameItem[] objs) {
        for (int i = 0; i < objs.length; i++) {
            if (objs[i] == null || objs[i].getItemId()==-1)
                return i;
        }
        return -1;
    }

    public void enqueueChat(final TextPacket packet) {
        this.playerChat.addChatMessage(packet);
    }

    public void setEquipment(GameItem[] loot) {
        this.inventory = new Slots[20];

        GameItem[] equipmentArr = Arrays.copyOfRange(loot, 0, 4);
        GameItem[] inventoryArr = Arrays.copyOfRange(loot, 4, 12);

        this.buildEquipmentSlots(equipmentArr);
        this.buildInventorySlots(inventoryArr);
    }

    public void setGroundLoot(GameItem[] loot, Graphics2D g) {
        this.groundLoot = new Slots[8];
        //GamePanel.ui2.createItemIcons(loot);
        for (int i = 0; i < loot.length; i++) {
            GameItem item = loot[i];
            if(item ==null || item.getItemId()==-1) continue;
            this.buildGroundLootSlotButton(i, item, g);
        }
    }

    public int getNonEmptySlotCount() {
        int count = 0;
        for (Slots s : this.getGroundLoot()) {
            if (s != null && s.getItem() != null) {
                count++;
            }
        }
        return count;
    }

    private void buildGroundLootSlotButton(int index, GameItem item, Graphics2D g) {
        int panelWidth = (GamePanel.width / 5);
        int startX = GamePanel.width - panelWidth;

        int yOffset = index > 3 ? 64 : 0;
        if (item != null) {
            final int actualIdx = index;
            Button b = null;
            if (index > 3) {
                b = new Button(new Vector2f(startX + ((actualIdx - 4) * 64), 650 + yOffset), 64);
            } else {
                b = new Button(new Vector2f(startX + (actualIdx * 64), 650 + yOffset), 64);
            }

            b.onHoverIn(event -> {
                this.tooltips.put(item.getUid(),
                        new ItemTooltip(item, new Vector2f((GamePanel.width / 2) + 75, 100), panelWidth, 400));
            });

            b.onHoverOut(event -> {
                this.tooltips.clear();
            });

            b.onMouseUp(event -> {
            	System.out.println();
                this.tooltips.clear();
                if (this.canSwap()) {
                    this.setActionTime();
                    GameItem[] currentInv = this.playState.getPlayer().getSlots(4, 12);
                    int idx = this.firstNullIdx(currentInv);
                    Slots currentEquip = this.inventory[idx + 4];

                    if ((currentEquip == null) && (idx > -1)) {
                        try {
                            this.playState.getRealmManager().moveItem(idx + 4, actualIdx + 20, false, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            this.groundLoot[actualIdx] = new Slots(b, item);
        }
    }

    private void buildEquipmentSlots(GameItem[] equipment) {
        for (int i = 0; i < equipment.length; i++) {
            GameItem item = equipment[i];
            if(item.getItemId()==-1) continue;
            this.buildEquipmentSlotButton(i, item);
        }
    }

    private void buildEquipmentSlotButton(int idx, GameItem item) {
        final int panelWidth = (GamePanel.width / 5);
        final int startX = GamePanel.width - panelWidth;
        if (item != null) {
            int actualIdx = (int) item.getTargetSlot();
            if (actualIdx == -1) {
                actualIdx = idx;
            }
            Button b = new Button(new Vector2f(startX + (actualIdx * 64), 256), 64);
            b.onHoverIn(event -> {
                this.tooltips.put(item.getUid(),
                        new ItemTooltip(item, new Vector2f((GamePanel.width / 2) + 75, 100), panelWidth, 400));
            });

            b.onHoverOut(event -> {
                this.tooltips.clear();
            });

            b.onRightClick(event -> {

                Slots dropped = this.getOverlapping(event);
                if ((dropped != null) && this.canSwap()) {
                    this.setActionTime();
                    int dropIndex = this.getOverlapIdx(event);
                    this.playState.getRealmManager().moveItem(-1, dropIndex, true, false);
                }
            });
            b.onHoverOut(event -> {
                this.tooltips.clear();
            });

            this.inventory[actualIdx] = new Slots(b, item);
        }
    }

    private void buildInventorySlots(GameItem[] inventory) {
        for (int i = 0; i < (inventory.length); i++) {
            GameItem item = inventory[i];
            if(item.getItemId()==-1) continue;
            this.buildInventorySlotsButton(i, item);
        }
    }

    private void buildInventorySlotsButton(int index, GameItem item) {
        final int inventoryOffset = 4;
        final int panelWidth = (GamePanel.width / 5);
        final int startX = GamePanel.width - panelWidth;

        if (item != null) {
            final int actualIdx = index + inventoryOffset;
            Button b = null;
            if (index > 3) {
                b = new Button(new Vector2f(startX + ((index - 4) * 64), 516), 64);
            } else {
                b = new Button(new Vector2f(startX + (index * 64), 450), 64);
            }

            b.onHoverIn(event -> {
                this.tooltips.put(item.getUid(),
                        new ItemTooltip(item, new Vector2f((GamePanel.width / 2) + 75, 100), panelWidth, 400));
            });

            b.onHoverOut(event -> {
                this.tooltips.clear();
            });

            b.onRightClick(event -> {
                Slots dropped = this.getOverlapping(event);
                if ((dropped != null) && this.canSwap()) {
                    this.setActionTime();
                    int idx = this.getOverlapIdx(event);
                    this.playState.getRealmManager().moveItem(-1, idx, true, false);
                }
            });

            this.inventory[actualIdx] = new Slots(b, item);
        }
    }

    private int getOverlapIdx(Vector2f pos) {
        Slots[] equipSlots = this.getSlots(4, 12);
        int returnIdx = -1;
        for (int i = 0; i < equipSlots.length; i++) {
            Slots s = equipSlots[i];
            if ((s == null) || (s.getButton() == null)) {
                continue;
            }
            if (s.getButton().getBounds().inside((int) pos.x, (int) pos.y)) {
                returnIdx = i;
            }
        }
        return returnIdx + 4;

    }

    @SuppressWarnings("unused")
    private boolean overlapsEquipment(Vector2f pos) {
        Slots[] equipSlots = this.getSlots(0, 4);
        for (Slots s : equipSlots) {
            if ((s == null) || (s.getButton() == null)) {
                continue;
            }
            if (s.getButton().getBounds().inside((int) pos.x, (int) pos.y))
                return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    private boolean overlapsGround(Vector2f pos) {
        final int panelWidth = (GamePanel.width / 5);
        Rectangle currBounds = new Rectangle(new Vector2f(0, 0), GamePanel.width - panelWidth, GamePanel.height);
        return currBounds.inside((int) pos.x, (int) pos.y);
    }

    private Slots getOverlapping(Vector2f pos) {
        Slots[] equipSlots = this.getSlots(0, 12);
        for (Slots s : equipSlots) {
            if ((s == null) || (s.getButton() == null)) {
                continue;
            }
            if (s.getButton().getBounds().inside((int) pos.x, (int) pos.y))
                return s;
        }
        return null;
    }

    @SuppressWarnings("unused")
    private boolean overlapsInventory(Vector2f pos) {
        final int panelWidth = (GamePanel.width / 5);
        final int startX = GamePanel.width - panelWidth;
        final int startY = 450;

        Rectangle currBounds = new Rectangle(new Vector2f(startX, startY), panelWidth, 128);
        Rectangle bounds = new Rectangle(currBounds.getPos().clone(), (int) currBounds.getWidth() * 4,
                (int) currBounds.getHeight() * 4);

        return bounds.inside((int) pos.x, (int) pos.y);
    }

    @SuppressWarnings("unused")
    private void removeGroundLootItemByUid(String uid) {
        int foundIdx = -1;
        for (int i = 0; i < this.groundLoot.length; i++) {
            Slots curr = this.groundLoot[i];
            if ((curr != null) && (curr.getItem() != null) && curr.getItem().getUid().equals(uid)) {
                foundIdx = i;
            }
        }
        if (foundIdx > -1) {
            this.groundLoot[foundIdx] = null;
        }
    }

    public void update(double time) {
        for (int i = 0; i < this.inventory.length; i++) {
            Slots curr = this.inventory[i];
            if (curr != null) {
                curr.update(time);
            }
        }
    }

    public void input(MouseHandler mouse, KeyHandler key) {
        for (int i = 0; i < this.inventory.length; i++) {
            Slots curr = this.inventory[i];
            if (curr != null) {
                curr.input(mouse, key);
            }
        }

        for (int i = 0; i < this.groundLoot.length; i++) {
            Slots curr = this.groundLoot[i];
            if (curr != null) {
                curr.input(mouse, key);
            }
        }
        try {
            this.playerChat.input(mouse, key, this.playState.getRealmManager().getClient());
        } catch (Exception e) {

        }
    }

    public boolean canSwap() {
        return (Instant.now().toEpochMilli() - this.lastAction) > 1000;
    }

    public void setActionTime() {
        this.lastAction = Instant.now().toEpochMilli();
    }

    public boolean isEquipmentEmpty() {
        for (int i = 0; i < this.inventory.length; i++) {
            Slots curr = this.inventory[i];
            if (curr == null) {
                continue;
            }
            if (curr.getItem() != null)
                return false;
        }
        return true;
    }

    public boolean isGroundLootEmpty() {
        for (int i = 0; i < this.groundLoot.length; i++) {
            Slots curr = this.groundLoot[i];
            if (curr == null) {
                continue;
            }
            if (curr.getItem() != null)
                return false;
        }
        return true;
    }

    private void renderStats(Graphics2D g) {
        if (this.playState.getPlayer() != null) {
            int panelWidth = (GamePanel.width / 5);
            int startX = (GamePanel.width - panelWidth) + 8;
            int xOffset = 128;
            int yOffset = 42;
            int startY = 350;
            
            Stats stats = this.playState.getPlayer().getComputedStats();
            Vector2f posHp = new Vector2f(GamePanel.width - 64, 128 + 32);
            Vector2f posMp = posHp.clone(0, 32);
            Vector2f posXp = posHp.clone(-128, -32);
            Vector2f nameLvlPos = posHp.clone(-256, -128);

            g.setColor(Color.WHITE);

            long fame = GameDataManager.EXPERIENCE_LVLS.getBaseFame(this.playState.getPlayer().getExperience());
            if (fame == 0l) {
                g.drawString(this.playState.getPlayer().getExperience() + "/"
                        + this.playState.getPlayer().getUpperExperienceBound(), posXp.x, posXp.y);
                g.drawString(
                        this.playState.getPlayer().getName() + "   Lv. "
                                + GameDataManager.EXPERIENCE_LVLS.getLevel(this.playState.getPlayer().getExperience()),
                        nameLvlPos.x, nameLvlPos.y);
            } else {
                g.drawString("Fame: " + fame, posXp.x, posXp.y);
                g.drawString(this.playState.getPlayer().getName() + "   Lv. 20", nameLvlPos.x, nameLvlPos.y);
            }
            if (this.playState.getPlayer().isStatMaxed(0)) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString("" + this.playState.getPlayer().getHealth(), posHp.x, posHp.y);
            if (this.playState.getPlayer().isStatMaxed(1)) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString("" + this.playState.getPlayer().getMana(), posMp.x, posMp.y);
            if (this.playState.getPlayer().isStatMaxed(3)) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString("att :" + stats.getAtt(), startX, startY);
            if (this.playState.getPlayer().isStatMaxed(4)) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString("spd :" + stats.getSpd(), startX, startY + (1 * yOffset));
            if (this.playState.getPlayer().isStatMaxed(6)) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString("vit :" + stats.getVit(), startX, startY + (2 * yOffset));
            if (this.playState.getPlayer().isStatMaxed(2)) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString("def :" + stats.getDef(), startX + xOffset, startY);
            if (this.playState.getPlayer().isStatMaxed(5)) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString("dex :" + stats.getDex(), startX + xOffset, startY + (1 * yOffset));
            if (this.playState.getPlayer().isStatMaxed(7)) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString("wis :" + stats.getWis(), startX + xOffset, startY + (2 * yOffset));
        }
    }
    
    public boolean isHoveringInventory(float posX) {
        int panelWidth = (GamePanel.width / 5);
        int startX = GamePanel.width - panelWidth;
        
        return posX >=startX;
    }

    public void render(Graphics2D g) {
        int panelWidth = (GamePanel.width / 5);
        int startX = GamePanel.width - panelWidth;
        g.setColor(Color.GRAY);

        g.fillRect(startX, 0, panelWidth, GamePanel.height);

        Slots[] equips = this.getSlots(0, 4);
        Slots[] inv1 = this.getSlots(4, 8);
        Slots[] inv2 = this.getSlots(8, 12);

        for (int i = 0; i < equips.length; i++) {
            Slots curr = equips[i];
            if (curr != null) {
                if ((curr.getDragPos() == null)) {
                    curr.render(g, new Vector2f(startX + (i * 64), 256));
                } else {
                    curr.render(g, curr.getDragPos());
                }
            }
        }

        for (int i = 0; i < inv1.length; i++) {
            Slots curr = inv1[i];
            if (curr != null) {
                if ((curr.getDragPos() == null)) {
                    curr.render(g, new Vector2f(startX + (i * 64), 450));
                } else {
                    curr.render(g, curr.getDragPos());

                }
            }
        }

        for (int i = 0; i < inv2.length; i++) {
            Slots curr = inv2[i];
            if (curr != null) {
                if ((curr.getDragPos() == null)) {
                    curr.render(g, new Vector2f(startX + (i * 64), 516));
                } else {
                    curr.render(g, curr.getDragPos());
                }
            }
        }

        Slots[] gl1 = Arrays.copyOfRange(this.groundLoot, 0, 4);
        Slots[] gl2 = Arrays.copyOfRange(this.groundLoot, 4, 8);

        for (int i = 0; i < gl1.length; i++) {
            Slots curr = gl1[i];
            if (curr != null) {
                if ((curr.getDragPos() == null)) {
                    gl1[i].render(g, new Vector2f(startX + (i * 64), 650));
                } else {
                    gl1[i].render(g, curr.getDragPos());

                }
            }
        }

        for (int i = 0; i < gl2.length; i++) {
            Slots curr = gl2[i];
            if (curr != null) {
                if ((curr.getDragPos() == null)) {
                    gl2[i].render(g, new Vector2f(startX + (i * 64), 714));
                } else {
                    gl2[i].render(g, curr.getDragPos());

                }
            }
        }

        for (ItemTooltip tip : this.tooltips.values()) {
            tip.render(g);
        }

        this.hp.render(g);
        this.mp.render(g);
        this.xp.render(g);
        this.renderStats(g);
        this.playerChat.render(g);
//        if (this.minimap.isInitialized()) {
//            this.minimap.update();
//            this.minimap.render(g);
//        }
    }
}