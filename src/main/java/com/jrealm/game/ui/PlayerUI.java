package com.jrealm.game.ui;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.jrealm.game.JRealmGame;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.ItemTooltip;
import com.jrealm.game.state.PlayState;
import com.jrealm.net.client.packet.UpdatePlayerTradeSelectionPacket;
import com.jrealm.net.entity.NetInventorySelection;
import com.jrealm.net.entity.NetTradeSelection;
import com.jrealm.net.messaging.CommandType;
import com.jrealm.net.messaging.ServerCommandMessage;
import com.jrealm.net.server.packet.CommandPacket;
import com.jrealm.net.server.packet.TextPacket;
import com.jrealm.util.KeyHandler;
import com.jrealm.util.MouseHandler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PlayerUI {
    private boolean isTrading;
    private FillBars hp;
    private FillBars mp;
    private FillBars xp;

    private Slots[] inventory;
    private Slots[] groundLoot;

    private PlayState playState;
    private PlayerChat playerChat;
    private Minimap minimap;
    private Map<String, ItemTooltip> tooltips;
    private long lastAction = Instant.now().toEpochMilli();
    private Button menuButton = null;

    private NetTradeSelection currentTradeSelection = null;
    private String tradePartnerName = null;
    private Button confirmTradeButton = null;
    private Button cancelTradeButton = null;

    private Map<Integer, TextureRegion> classIconCache = new HashMap<>();
    private List<Button> nearbyPlayerButtons = new ArrayList<>();
    private List<Player> nearbyPlayerList = new ArrayList<>();
    private Player hoveredPlayer = null;
    private long lastNearbyRefresh = 0;

    private int dragSourceIndex = -1;
    private boolean isDragging = false;
    private Vector2f dragStartPos = null;
    private static final float DRAG_THRESHOLD = 8.0f;

    public PlayerUI(PlayState p) {
        int panelWidth = JRealmGame.width / 5;
        int startX = JRealmGame.width - panelWidth;
        int barHeight = 24;
        int barY = 32;

        this.isTrading = false;
        this.playState = p;
        this.hp = new FillBars(p.getPlayer(), new Vector2f(startX, barY),
                panelWidth, barHeight, "getHealthPercent", Color.DARK_GRAY, Color.RED);
        this.mp = new FillBars(p.getPlayer(), new Vector2f(startX, barY + barHeight),
                panelWidth, barHeight, "getManaPercent", Color.DARK_GRAY, Color.BLUE);
        this.xp = new FillBars(p.getPlayer(), new Vector2f(startX, barY + barHeight * 2),
                panelWidth, barHeight, "getExperiencePercent", Color.DARK_GRAY, new Color(1.0f, 0.5f, 0.0f, 1.0f));
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
            if (objs[i] == null || objs[i].getItemId() == -1)
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

    public void setGroundLoot(GameItem[] loot) {
        if (this.isTrading && this.currentTradeSelection != null) {
            loot = this.getOtherPlayerSelectedItems();
        }
        this.groundLoot = new Slots[8];
        for (int i = 0; i < loot.length; i++) {
            GameItem item = loot[i];
            if (item == null || item.getItemId() == -1) continue;
            this.buildGroundLootSlotButton(i, item);
        }
    }

    /**
     * Get the other player's NetInventorySelection (not filtered).
     */
    private NetInventorySelection getOtherPlayerSelection() {
        if (this.currentTradeSelection == null) return null;

        long myId = this.playState.getPlayerId();
        if (this.currentTradeSelection.getPlayer0Selection() != null
                && this.currentTradeSelection.getPlayer0Selection().getPlayerId() == myId) {
            return this.currentTradeSelection.getPlayer1Selection();
        } else {
            return this.currentTradeSelection.getPlayer0Selection();
        }
    }

    /**
     * Get the items that the OTHER player has selected for trade.
     * Determines which selection is "other" based on local player ID.
     */
    private GameItem[] getOtherPlayerSelectedItems() {
        NetInventorySelection otherSelection = this.getOtherPlayerSelection();
        if (otherSelection == null || otherSelection.getItemRefs() == null) {
            return new GameItem[8];
        }

        GameItem[] allItems = otherSelection.getGameItems();
        Boolean[] selection = otherSelection.getSelection();
        if (selection == null) return new GameItem[8];

        GameItem[] selectedItems = new GameItem[8];
        int idx = 0;
        for (int i = 0; i < selection.length && i < allItems.length; i++) {
            if (selection[i] != null && selection[i] && allItems[i] != null) {
                if (idx < 8) {
                    selectedItems[idx++] = allItems[i];
                }
            }
        }
        return selectedItems;
    }

    public void clearTradeSelections() {
        Slots[] invSlots = this.getSlots(4, 12);
        for (Slots slot : invSlots) {
            if (slot != null) {
                slot.setSelected(false);
            }
        }
        this.confirmTradeButton = null;
        this.cancelTradeButton = null;
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

    private void buildGroundLootSlotButton(int index, GameItem item) {
        int panelWidth = (JRealmGame.width / 5);
        int startX = JRealmGame.width - panelWidth;

        int yOffset = index > 3 ? 64 : 0;
        if (item != null) {
            final int actualIdx = index;
            Button b;
            if (index > 3) {
                b = new Button(new Vector2f(startX + ((actualIdx - 4) * 64), 650 + yOffset), 64);
            } else {
                b = new Button(new Vector2f(startX + (actualIdx * 64), 650 + yOffset), 64);
            }

            b.onHoverIn(event -> {
                this.tooltips.put(item.getUid(),
                        new ItemTooltip(item, new Vector2f((JRealmGame.width / 2) + 75, 100), panelWidth, 400));
            });

            b.onHoverOut(event -> {
                this.tooltips.clear();
            });

            b.onMouseUp(event -> {
                // Don't allow picking up items from ground loot area during trade
                if (this.isTrading) return;
                if (this.isDragging) return;
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
            if (item == null || item.getItemId() == -1) continue;
            this.buildEquipmentSlotButton(i, item);
        }
    }

    private void buildEquipmentSlotButton(int idx, GameItem item) {
        final int panelWidth = (JRealmGame.width / 5);
        final int startX = JRealmGame.width - panelWidth;
        if (item != null) {
            int actualIdx = (int) item.getTargetSlot();
            if (actualIdx == -1) {
                actualIdx = idx;
            }
            Button b = new Button(new Vector2f(startX + (actualIdx * 64), 256), 64);
            b.onHoverIn(event -> {
                this.tooltips.put(item.getUid(),
                        new ItemTooltip(item, new Vector2f((JRealmGame.width / 2) + 75, 100), panelWidth, 400));
            });

            b.onHoverOut(event -> {
                this.tooltips.clear();
            });

            b.onRightClick(event -> {
                // Don't allow equipment swaps during trade
                if (this.isTrading) return;
                Slots dropped = this.getOverlapping(event);
                if ((dropped != null) && this.canSwap()) {
                    this.setActionTime();
                    int dropIndex = this.getOverlapIdx(event);
                    this.playState.getRealmManager().moveItem(-1, dropIndex, true, false);
                }
            });

            this.inventory[actualIdx] = new Slots(b, item);
        }
    }

    private void buildInventorySlots(GameItem[] inventory) {
        for (int i = 0; i < (inventory.length); i++) {
            GameItem item = inventory[i];
            if (item == null || item.getItemId() == -1) continue;
            this.buildInventorySlotsButton(i, item);
        }
    }

    private void buildInventorySlotsButton(int index, GameItem item) {
        final int inventoryOffset = 4;
        final int panelWidth = (JRealmGame.width / 5);
        final int startX = JRealmGame.width - panelWidth;

        if (item != null) {
            final int actualIdx = index + inventoryOffset;
            Button b;
            if (index > 3) {
                b = new Button(new Vector2f(startX + ((index - 4) * 64), 516), 64);
            } else {
                b = new Button(new Vector2f(startX + (index * 64), 450), 64);
            }

            b.onHoverIn(event -> {
                this.tooltips.put(item.getUid(),
                        new ItemTooltip(item, new Vector2f((JRealmGame.width / 2) + 75, 100), panelWidth, 400));
            });

            b.onHoverOut(event -> {
                this.tooltips.clear();
            });

            b.onRightClick(event -> {
                if (this.isTrading) {
                    Slots dropped = this.getOverlapping(event);
                    if ((dropped != null)) {
                        dropped.setSelected(!dropped.isSelected());
                        final UpdatePlayerTradeSelectionPacket updatedTrade = UpdatePlayerTradeSelectionPacket
                                .fromSelection(this.getPlayState().getPlayer(), this);
                        try {
                            this.playState.getRealmManager().getClient().sendRemote(updatedTrade);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Slots dropped = this.getOverlapping(event);
                    if ((dropped != null) && this.canSwap()) {
                        this.setActionTime();
                        int idx = this.getOverlapIdx(event);
                        this.playState.getRealmManager().moveItem(-1, idx, true, false);
                    }
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
            if ((s == null) || (s.getButton() == null)) continue;
            if (s.getButton().getBounds().inside((int) pos.x, (int) pos.y)) {
                returnIdx = i;
            }
        }
        return returnIdx + 4;
    }

    private Slots getOverlapping(Vector2f pos) {
        Slots[] equipSlots = this.getSlots(0, 12);
        for (Slots s : equipSlots) {
            if ((s == null) || (s.getButton() == null)) continue;
            if (s.getButton().getBounds().inside((int) pos.x, (int) pos.y))
                return s;
        }
        return null;
    }

    public void update(double time) {
        for (int i = 0; i < this.inventory.length; i++) {
            Slots curr = this.inventory[i];
            if (curr != null) {
                curr.update(time);
            }
        }

        // Update trade buttons
        if (this.isTrading) {
            if (this.confirmTradeButton != null) {
                this.confirmTradeButton.update(time);
            }
            if (this.cancelTradeButton != null) {
                this.cancelTradeButton.update(time);
            }
        }

        // Update nearby player buttons
        for (Button btn : this.nearbyPlayerButtons) {
            btn.update(time);
        }
    }

    public void input(MouseHandler mouse, KeyHandler key) {
        this.handleDragAndDrop(mouse);

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

        // Handle trade button input
        if (this.isTrading) {
            if (this.confirmTradeButton != null) {
                this.confirmTradeButton.input(mouse, key);
            }
            if (this.cancelTradeButton != null) {
                this.cancelTradeButton.input(mouse, key);
            }
        }

        // Handle nearby player button input
        for (Button btn : this.nearbyPlayerButtons) {
            btn.input(mouse, key);
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
            if (curr == null) continue;
            if (curr.getItem() != null)
                return false;
        }
        return true;
    }

    public boolean isGroundLootEmpty() {
        for (int i = 0; i < this.groundLoot.length; i++) {
            Slots curr = this.groundLoot[i];
            if (curr == null) continue;
            if (curr.getItem() != null)
                return false;
        }
        return true;
    }

    public boolean isHoveringInventory(float posX) {
        int panelWidth = (JRealmGame.width / 5);
        int startX = JRealmGame.width - panelWidth;
        return posX >= startX;
    }

    private void sendTradeCommand(String command) {
        try {
            ServerCommandMessage serverCommand = ServerCommandMessage.parseFromInput("/" + command);
            CommandPacket packet = CommandPacket.create(this.playState.getPlayer(), CommandType.SERVER_COMMAND,
                    serverCommand);
            this.playState.getRealmManager().getClient().sendRemote(packet);
        } catch (Exception e) {
            log.error("Failed to send trade command. Reason: {}", e);
        }
    }

    private void ensureTradeButtons() {
        if (this.confirmTradeButton != null && this.cancelTradeButton != null) return;

        int panelWidth = (JRealmGame.width / 5);
        int startX = JRealmGame.width - panelWidth;
        int buttonWidth = (panelWidth / 2) - 8;
        int buttonY = 790;

        this.confirmTradeButton = new Button("CONFIRM", new Vector2f(startX + 4, buttonY), buttonWidth, 32);
        this.confirmTradeButton.onMouseUp(event -> {
            this.sendTradeCommand("confirm true");
        });

        this.cancelTradeButton = new Button("CANCEL", new Vector2f(startX + buttonWidth + 12, buttonY), buttonWidth, 32);
        this.cancelTradeButton.onMouseUp(event -> {
            this.sendTradeCommand("decline");
        });
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        int panelWidth = (JRealmGame.width / 5);
        int startX = JRealmGame.width - panelWidth;

        // Draw panel background
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.GRAY);
        shapes.rect(startX, 0, panelWidth, JRealmGame.height);
        shapes.end();
        batch.begin();

        Slots[] equips = this.getSlots(0, 4);
        Slots[] inv1 = this.getSlots(4, 8);
        Slots[] inv2 = this.getSlots(8, 12);

        for (int i = 0; i < equips.length; i++) {
            Slots curr = equips[i];
            if (curr != null) {
                Vector2f pos = (curr.getDragPos() == null) ? new Vector2f(startX + (i * 64), 256) : curr.getDragPos();
                curr.render(batch, shapes, pos);
            }
        }

        for (int i = 0; i < inv1.length; i++) {
            Slots curr = inv1[i];
            if (curr != null) {
                Vector2f pos = (curr.getDragPos() == null) ? new Vector2f(startX + (i * 64), 450) : curr.getDragPos();
                curr.render(batch, shapes, pos);
            }
        }

        for (int i = 0; i < inv2.length; i++) {
            Slots curr = inv2[i];
            if (curr != null) {
                Vector2f pos = (curr.getDragPos() == null) ? new Vector2f(startX + (i * 64), 516) : curr.getDragPos();
                curr.render(batch, shapes, pos);
            }
        }

        // Trade UI overlay
        if (this.isTrading) {
            this.renderTradeUI(batch, shapes, font, startX, panelWidth);
        } else {
            // Normal ground loot rendering
            Slots[] gl1 = Arrays.copyOfRange(this.groundLoot, 0, 4);
            Slots[] gl2 = Arrays.copyOfRange(this.groundLoot, 4, 8);

            for (int i = 0; i < gl1.length; i++) {
                Slots curr = gl1[i];
                if (curr != null) {
                    Vector2f pos = (curr.getDragPos() == null) ? new Vector2f(startX + (i * 64), 650) : curr.getDragPos();
                    curr.render(batch, shapes, pos);
                }
            }

            for (int i = 0; i < gl2.length; i++) {
                Slots curr = gl2[i];
                if (curr != null) {
                    Vector2f pos = (curr.getDragPos() == null) ? new Vector2f(startX + (i * 64), 714) : curr.getDragPos();
                    curr.render(batch, shapes, pos);
                }
            }
        }

        // Render nearby players list
        this.renderNearbyPlayers(batch, shapes, font, startX, panelWidth);

        for (ItemTooltip tip : this.tooltips.values()) {
            tip.render(batch, shapes, font);
        }

        // Render hovered player tooltip on top of everything
        this.renderPlayerTooltip(batch, shapes, font);

        this.hp.render(batch, shapes, font);
        this.mp.render(batch, shapes, font);
        this.xp.render(batch, shapes, font);
        this.renderStats(batch, font);
        this.playerChat.render(batch, shapes, font);

        if (this.minimap.isInitialized()) {
            this.minimap.update();
            this.minimap.render(batch, shapes);
        }
    }

    private void renderTradeUI(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, int startX, int panelWidth) {
        this.ensureTradeButtons();

        // Update ground loot with ALL of other player's items, marking selected ones
        if (this.currentTradeSelection != null) {
            NetInventorySelection otherSelection = this.getOtherPlayerSelection();
            this.groundLoot = new Slots[8];
            if (otherSelection != null && otherSelection.getItemRefs() != null) {
                GameItem[] allItems = otherSelection.getGameItems();
                Boolean[] selection = otherSelection.getSelection();
                for (int i = 0; i < allItems.length && i < 8; i++) {
                    GameItem item = allItems[i];
                    if (item == null || item.getItemId() == -1) continue;
                    this.buildGroundLootSlotButton(i, item);
                    // Mark selected items so they render with yellow highlight
                    if (this.groundLoot[i] != null && selection != null && i < selection.length
                            && selection[i] != null && selection[i]) {
                        this.groundLoot[i].setSelected(true);
                    }
                }
            }
        }

        // Draw trade header
        String header = "Trading with " + (this.tradePartnerName != null ? this.tradePartnerName : "...");
        font.setColor(Color.GREEN);
        font.draw(batch, header, startX + 4, 230);

        // Draw "YOUR OFFER" label
        font.setColor(Color.YELLOW);
        font.draw(batch, "YOUR OFFER (right-click to select)", startX + 4, 440);

        // Draw "THEIR OFFER" label - show partner's full inventory
        String theirLabel = this.tradePartnerName != null
                ? this.tradePartnerName + "'s ITEMS (selected = offered)"
                : "THEIR ITEMS";
        font.setColor(Color.CYAN);
        font.draw(batch, theirLabel, startX + 4, 640);

        // Render the other player's items in the ground loot area
        Slots[] gl1 = Arrays.copyOfRange(this.groundLoot, 0, 4);
        Slots[] gl2 = Arrays.copyOfRange(this.groundLoot, 4, 8);

        for (int i = 0; i < gl1.length; i++) {
            Slots curr = gl1[i];
            if (curr != null) {
                curr.render(batch, shapes, new Vector2f(startX + (i * 64), 650));
            }
        }

        for (int i = 0; i < gl2.length; i++) {
            Slots curr = gl2[i];
            if (curr != null) {
                curr.render(batch, shapes, new Vector2f(startX + (i * 64), 714));
            }
        }

        // Draw Confirm and Cancel buttons
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.FOREST);
        shapes.rect(this.confirmTradeButton.getPos().x, this.confirmTradeButton.getPos().y,
                this.confirmTradeButton.getWidth(), this.confirmTradeButton.getHeight());
        shapes.setColor(Color.FIREBRICK);
        shapes.rect(this.cancelTradeButton.getPos().x, this.cancelTradeButton.getPos().y,
                this.cancelTradeButton.getWidth(), this.cancelTradeButton.getHeight());
        shapes.end();
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "CONFIRM", this.confirmTradeButton.getPos().x + 8, this.confirmTradeButton.getPos().y + 22);
        font.draw(batch, "CANCEL", this.cancelTradeButton.getPos().x + 12, this.cancelTradeButton.getPos().y + 22);
    }

    private TextureRegion getClassIcon(int classId) {
        TextureRegion cached = this.classIconCache.get(classId);
        if (cached != null) return cached;
        CharacterClass cls = CharacterClass.valueOf(classId);
        if (cls == null) return null;
        try {
            TextureRegion icon = GameSpriteManager.loadClassSprites(cls).getSubSprite(0, 0).getRegion();
            this.classIconCache.put(classId, icon);
            return icon;
        } catch (Exception e) {
            return null;
        }
    }

    private void refreshNearbyPlayerButtons(int startX, int panelWidth) {
        long now = Instant.now().toEpochMilli();
        if ((now - this.lastNearbyRefresh) < 500 && !this.nearbyPlayerButtons.isEmpty()) return;
        this.lastNearbyRefresh = now;

        Set<Player> nearby = null;
        try {
            nearby = this.playState.getRealmManager().getRealm().getPlayersExcept(this.playState.getPlayerId());
        } catch (Exception e) {
            return;
        }
        if (nearby == null || nearby.isEmpty()) {
            this.nearbyPlayerButtons.clear();
            this.nearbyPlayerList.clear();
            this.hoveredPlayer = null;
            return;
        }

        int headerY = this.isTrading ? 840 : 830;
        int iconSize = 20;
        int entryHeight = 26;
        int colWidth = (panelWidth - 12) / 2;
        int startY = headerY + 16;

        List<Player> playerList = new ArrayList<>(nearby);
        List<Button> newButtons = new ArrayList<>();

        for (int i = 0; i < playerList.size() && i < 16; i++) {
            Player p = playerList.get(i);
            int col = i % 2;
            int row = i / 2;

            int x = startX + 4 + (col * (colWidth + 4));
            int y = startY + (row * entryHeight);

            Button btn = new Button(new Vector2f(x, y), iconSize);
            btn.getBounds().setWidth(colWidth);
            btn.getBounds().setHeight(entryHeight);
            final Player hoverTarget = p;
            btn.onHoverIn(event -> {
                this.hoveredPlayer = hoverTarget;
            });
            btn.onHoverOut(event -> {
                if (this.hoveredPlayer == hoverTarget) {
                    this.hoveredPlayer = null;
                }
            });
            newButtons.add(btn);
        }

        this.nearbyPlayerList = playerList;
        this.nearbyPlayerButtons = newButtons;
    }

    private void renderNearbyPlayers(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, int startX, int panelWidth) {
        this.refreshNearbyPlayerButtons(startX, panelWidth);

        if (this.nearbyPlayerList.isEmpty()) return;

        int headerY = this.isTrading ? 840 : 830;
        font.setColor(Color.WHITE);
        font.draw(batch, "Nearby Players", startX + 4, headerY);

        int iconSize = 20;
        int entryHeight = 26;
        int colWidth = (panelWidth - 12) / 2;
        int startY = headerY + 16;
        int maxNameChars = 10;

        for (int i = 0; i < this.nearbyPlayerList.size() && i < 16; i++) {
            Player p = this.nearbyPlayerList.get(i);
            int col = i % 2;
            int row = i / 2;

            int x = startX + 4 + (col * (colWidth + 4));
            int y = startY + (row * entryHeight);

            // Draw class icon
            TextureRegion icon = this.getClassIcon(p.getClassId());
            if (icon != null) {
                batch.draw(icon, x, y, iconSize, iconSize);
            }

            // Draw clipped player name
            String displayName = p.getName();
            if (displayName.length() > maxNameChars) {
                displayName = displayName.substring(0, maxNameChars) + "..";
            }

            // Highlight hovered player
            if (this.hoveredPlayer == p) {
                font.setColor(Color.YELLOW);
            } else {
                font.setColor(Color.WHITE);
            }
            font.draw(batch, displayName, x + iconSize + 4, y + 14);
        }
    }

    private void renderPlayerTooltip(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        if (this.hoveredPlayer == null) return;

        Player p = this.hoveredPlayer;
        int tooltipX = (JRealmGame.width / 2) + 75;
        int tooltipY = 100;
        int tooltipWidth = JRealmGame.width / 5;
        int tooltipHeight = 120;
        int spacing = 16;

        // Background
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.2f, 0.2f, 0.2f, 0.9f));
        shapes.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        shapes.end();
        batch.begin();

        // Player name and class
        CharacterClass cls = CharacterClass.valueOf(p.getClassId());
        String className = cls != null ? cls.name() : "Unknown";
        font.setColor(Color.YELLOW);
        font.draw(batch, p.getName() + " (" + className + ")", tooltipX + 4, tooltipY + spacing);

        // HP and MP
        font.setColor(Color.RED);
        font.draw(batch, "HP: " + p.getHealth(), tooltipX + 4, tooltipY + (2 * spacing));
        font.setColor(Color.BLUE);
        font.draw(batch, "MP: " + p.getMana(), tooltipX + 100, tooltipY + (2 * spacing));

        // Equipped items label
        font.setColor(Color.WHITE);
        font.draw(batch, "Equipment:", tooltipX + 4, tooltipY + (3 * spacing));

        // Draw equipped item sprites (slots 0-3)
        GameItem[] equips = p.getSlots(0, 4);
        int itemX = tooltipX + 4;
        int itemY = tooltipY + (3 * spacing) + 4;
        int itemSize = 32;
        for (int i = 0; i < equips.length; i++) {
            if (equips[i] != null && equips[i].getItemId() != -1) {
                TextureRegion itemRegion = GameSpriteManager.ITEM_SPRITES.get(equips[i].getItemId());
                if (itemRegion != null) {
                    batch.draw(itemRegion, itemX + (i * (itemSize + 4)), itemY, itemSize, itemSize);
                }
            } else {
                // Draw empty slot placeholder
                batch.end();
                shapes.begin(ShapeRenderer.ShapeType.Line);
                shapes.setColor(Color.DARK_GRAY);
                shapes.rect(itemX + (i * (itemSize + 4)), itemY, itemSize, itemSize);
                shapes.end();
                batch.begin();
            }
        }
    }

    public void handleDragAndDrop(MouseHandler mouse) {
        if (this.isTrading) {
            for (Slots slot : this.inventory) {
                if (slot != null) slot.setDragPos(null);
            }
            for (Slots slot : this.groundLoot) {
                if (slot != null) slot.setDragPos(null);
            }
            this.isDragging = false;
            this.dragSourceIndex = -1;
            this.dragStartPos = null;
            return;
        }

        int mouseX = (int) mouse.getX();
        int mouseY = (int) mouse.getY();

        if (mouse.isPressed(1) && !this.isDragging && this.dragSourceIndex == -1) {
            // Detect drag start: find slot with non-null dragPos
            for (int i = 0; i < this.inventory.length; i++) {
                Slots slot = this.inventory[i];
                if (slot != null && slot.getDragPos() != null && slot.getItem() != null) {
                    this.dragSourceIndex = i;
                    this.dragStartPos = new Vector2f(mouseX, mouseY);
                    break;
                }
            }
            if (this.dragSourceIndex == -1) {
                for (int i = 0; i < this.groundLoot.length; i++) {
                    Slots slot = this.groundLoot[i];
                    if (slot != null && slot.getDragPos() != null && slot.getItem() != null) {
                        this.dragSourceIndex = i + 20;
                        this.dragStartPos = new Vector2f(mouseX, mouseY);
                        break;
                    }
                }
            }
        }

        // Only set isDragging after mouse moves past threshold
        if (this.dragSourceIndex != -1 && !this.isDragging && this.dragStartPos != null) {
            float dist = new Vector2f(mouseX, mouseY).distanceTo(this.dragStartPos);
            if (dist > DRAG_THRESHOLD) {
                this.isDragging = true;
            }
        }

        // On mouse release while dragging
        if (!mouse.isPressed(1) && this.isDragging) {
            int targetIndex = this.findSlotAtPositionByLayout(mouseX, mouseY);
            this.executeDrop(this.dragSourceIndex, targetIndex);
            this.isDragging = false;
            this.dragSourceIndex = -1;
            this.dragStartPos = null;
        } else if (!mouse.isPressed(1)) {
            // Reset if released without dragging
            this.dragSourceIndex = -1;
            this.dragStartPos = null;
        }
    }

    private int findSlotAtPositionByLayout(int mouseX, int mouseY) {
        int panelWidth = (JRealmGame.width / 5);
        int startX = JRealmGame.width - panelWidth;

        // Must be within the panel
        if (mouseX < startX || mouseX > JRealmGame.width) return -1;

        int col = (mouseX - startX) / 64;
        if (col < 0 || col > 3) return -1;

        // Equipment row: Y=256, height=64
        if (mouseY >= 256 && mouseY < 320) {
            return col; // 0-3
        }
        // Inventory row 1: Y=450, height=64
        if (mouseY >= 450 && mouseY < 514) {
            return 4 + col; // 4-7
        }
        // Inventory row 2: Y=516, height=64
        if (mouseY >= 516 && mouseY < 580) {
            return 8 + col; // 8-11
        }
        // Ground loot row 1: Y=650, height=64
        if (mouseY >= 650 && mouseY < 714) {
            return 20 + col; // 20-23
        }
        // Ground loot row 2: Y=714, height=64
        if (mouseY >= 714 && mouseY < 778) {
            return 24 + col; // 24-27
        }
        return -1;
    }

    private void executeDrop(int fromIndex, int targetIndex) {
        if (!this.canSwap()) return;
        if (fromIndex == targetIndex) return;

        this.setActionTime();

        boolean fromIsGround = fromIndex >= 20 && fromIndex <= 27;
        boolean targetIsGround = targetIndex >= 20 && targetIndex <= 27;
        boolean fromIsEquip = fromIndex >= 0 && fromIndex <= 3;
        boolean targetIsEquip = targetIndex >= 0 && targetIndex <= 3;

        if (targetIndex == -1) {
            // Dropped outside any slot: drop item
            this.playState.getRealmManager().moveItem(-1, fromIndex, true, false);
        } else if (fromIsGround && !targetIsGround) {
            // Ground -> inventory/equip: pickup
            this.playState.getRealmManager().moveItem(targetIndex, fromIndex, false, false);
        } else if (!fromIsGround && targetIsGround) {
            // Inventory/equip -> ground area: drop
            this.playState.getRealmManager().moveItem(-1, fromIndex, true, false);
        } else {
            // inv->equip, equip->inv, inv->inv: swap/equip/unequip
            this.playState.getRealmManager().moveItem(targetIndex, fromIndex, false, false);
        }
    }

    private void renderStats(SpriteBatch batch, BitmapFont font) {
        if (this.playState.getPlayer() != null) {
            int panelWidth = (JRealmGame.width / 5);
            int startX = (JRealmGame.width - panelWidth) + 8;
            int xOffset = 128;
            int yOffset = 42;
            int startY = 350;

            Stats stats = this.playState.getPlayer().getComputedStats();
            int nameLvlX = (JRealmGame.width - panelWidth) + 8;
            int nameLvlY = 20;

            font.setColor(Color.WHITE);
            long fame = GameDataManager.EXPERIENCE_LVLS.getBaseFame(this.playState.getPlayer().getExperience());
            if (fame == 0l) {
                font.draw(batch,
                        this.playState.getPlayer().getName() + "   Lv. "
                                + GameDataManager.EXPERIENCE_LVLS.getLevel(this.playState.getPlayer().getExperience()),
                        nameLvlX, nameLvlY);
            } else {
                font.draw(batch, this.playState.getPlayer().getName() + "   Lv. 20", nameLvlX, nameLvlY);
            }

            font.setColor(this.playState.getPlayer().isStatMaxed(3) ? Color.YELLOW : Color.WHITE);
            font.draw(batch, "att :" + stats.getAtt(), startX, startY);
            font.setColor(this.playState.getPlayer().isStatMaxed(4) ? Color.YELLOW : Color.WHITE);
            font.draw(batch, "spd :" + stats.getSpd(), startX, startY + (1 * yOffset));
            font.setColor(this.playState.getPlayer().isStatMaxed(6) ? Color.YELLOW : Color.WHITE);
            font.draw(batch, "vit :" + stats.getVit(), startX, startY + (2 * yOffset));
            font.setColor(this.playState.getPlayer().isStatMaxed(2) ? Color.YELLOW : Color.WHITE);
            font.draw(batch, "def :" + stats.getDef(), startX + xOffset, startY);
            font.setColor(this.playState.getPlayer().isStatMaxed(5) ? Color.YELLOW : Color.WHITE);
            font.draw(batch, "dex :" + stats.getDex(), startX + xOffset, startY + (1 * yOffset));
            font.setColor(this.playState.getPlayer().isStatMaxed(7) ? Color.YELLOW : Color.WHITE);
            font.draw(batch, "wis :" + stats.getWis(), startX + xOffset, startY + (2 * yOffset));
        }
    }
}
