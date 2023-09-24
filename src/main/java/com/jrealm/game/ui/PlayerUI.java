package com.jrealm.game.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.jrealm.game.GamePanel;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.ItemTooltip;
import com.jrealm.game.states.PlayState;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

import lombok.Data;

@Data
public class PlayerUI {
	public static boolean DRAGGING_ITEM = false;
	private FillBars hp;
	private FillBars mp;

	private Slots[] inventory;

	private Slots[] groundLoot;

	private PlayState playState;

	private Map<String, ItemTooltip> tooltips;

	public PlayerUI(PlayState p) {
		this.playState = p;
		SpriteSheet bars = new SpriteSheet("ui/fillbars.png", 0);
		BufferedImage[] barSpritesHp = { bars.getSubimage(12, 2, 7, 16), bars.getSubimage(39, 0, 7, 14), // red health
				// bar
				bars.getSubimage(0, 0, 12, 20) };

		BufferedImage[] barSpritesMp = { bars.getSubimage(12, 2, 7, 16), bars.getSubimage(39, 16, 7, 14),
				bars.getSubimage(0, 0, 12, 20) };
		Vector2f posHp = new Vector2f(GamePanel.width - 356, 128);
		Vector2f posMp = posHp.clone(0, 64);

		this.hp = new FillBars(p.getPlayer(), barSpritesHp, posHp, 16, 16, false);
		this.mp = new FillBars(p.getPlayer(), barSpritesMp, posMp, 16, 16, true);

		this.groundLoot = new Slots[8];
		this.inventory = new Slots[20];

		this.tooltips = new HashMap<>();
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

	public int firstNullIdx(Object[] objs) {
		for (int i = 0; i < objs.length; i++) {
			if (objs[i] == null)
				return i;
		}
		return -1;
	}

	public void setEquipment(GameItem[] loot) {
		this.inventory = new Slots[20];

		GameItem[] equipmentArr = Arrays.copyOfRange(loot, 0, 4);
		GameItem[] inventoryArr = Arrays.copyOfRange(loot, 4, 12);

		this.buildEquipmentSlots(equipmentArr);
		this.buildInventorySlots(inventoryArr);
	}

	public void setGroundLoot(GameItem[] loot, Graphics2D g) {
		int panelWidth = (GamePanel.width / 5);

		int startX = GamePanel.width - panelWidth;

		this.groundLoot = new Slots[8];

		for (int i = 0; i < loot.length; i++) {
			GameItem item = loot[i];
			int yOffset = i > 3 ? 64 : 0;
			if (item != null) {
				final int actualIdx = i;

				Button b = new Button(new Vector2f(startX + (actualIdx * 64), 650 + yOffset), 64);
				b.onHoverIn(event -> {
					this.tooltips.put(item.getUid(),
							new ItemTooltip(item, new Vector2f((GamePanel.width / 2) + 75, 100), panelWidth, 400));
				});
				b.onHoverOut(event -> {
					this.tooltips.remove(item.getUid());
				});
				b.onMouseDown(event -> {
					PlayerUI.DRAGGING_ITEM = true;

				});
				b.onMouseUp(event -> {
					PlayerUI.DRAGGING_ITEM = false;
					this.tooltips.clear();
					if (this.overlapsEquipment(event)) {
						Slots currentEquip = this.inventory[item.getTargetSlot()];
						this.groundLoot[actualIdx].setItem(currentEquip.getItem());
						this.inventory[item.getTargetSlot()].setItem(item);
						this.playState.replaceLootContainerItemByUid(item.getUid(),
								this.groundLoot[actualIdx].getItem());
						this.getPlayState().getPlayer().getInventory()[item.getTargetSlot()] = item;

						this.setEquipment(this.getPlayState().getPlayer().getInventory());
						this.setGroundLoot(this.groundLoot);
					} else if (this.overlapsInventory(event)) {
						GameItem[] currentInv = this.playState.getPlayer().getSlots(4, 12);
						Slots groundLoot = this.groundLoot[actualIdx];
						int idx = this.firstNullIdx(currentInv);
						Slots currentEquip = this.inventory[idx + 4];
						if ((currentEquip == null) && (idx > -1)) {
							this.inventory[idx + 4] = groundLoot;
							this.groundLoot[actualIdx] = null;
							this.playState.replaceLootContainerItemByUid(item.getUid(), null);
							this.getPlayState().getPlayer().getInventory()[idx + 4] = item;
						}
						this.setEquipment(this.getPlayState().getPlayer().getInventory());
						this.setGroundLoot(this.groundLoot);
					}
				});
				this.groundLoot[actualIdx] = new Slots(b, item);
			}
		}
	}

	private void buildEquipmentSlots(GameItem[] equipment) {
		final int panelWidth = (GamePanel.width / 5);
		final int startX = GamePanel.width - panelWidth;

		for (int i = 0; i < equipment.length; i++) {
			GameItem item = equipment[i];
			if (item != null) {
				int actualIdx = (int) item.getTargetSlot();
				if (actualIdx == -1) {
					actualIdx = i;
				}
				Button b = new Button(new Vector2f(startX + (actualIdx * 64), 256), 64);
				b.onHoverIn(event -> {
					this.tooltips.put(item.getUid(),
							new ItemTooltip(item, new Vector2f((GamePanel.width / 2) + 75, 100), panelWidth, 400));
				});

				b.onHoverOut(event -> {
					this.tooltips.clear();
				});
				b.onMouseDown(event -> {
					PlayerUI.DRAGGING_ITEM = true;
				});
				b.onMouseUp(event -> {
					PlayerUI.DRAGGING_ITEM = false;
					this.tooltips.clear();

				});
				this.inventory[actualIdx] = new Slots(b, item);
			}
		}
	}

	private void buildInventorySlots(GameItem[] inventory) {
		final int inventoryOffset = 4;
		final int panelWidth = (GamePanel.width / 5);
		final int startX = GamePanel.width - panelWidth;

		for (int i = 0; i < (inventory.length); i++) {
			GameItem item = inventory[i];
			if (item != null) {
				final int actualIdx = i + inventoryOffset;
				Button b = null;
				if (i > 3) {
					b = new Button(new Vector2f(startX + ((i - 4) * 64), 516), 64);
				} else {
					b = new Button(new Vector2f(startX + (i * 64), 450), 64);
				}
				b.onHoverIn(event -> {
					this.tooltips.put(item.getUid(),
							new ItemTooltip(item, new Vector2f((GamePanel.width / 2) + 75, 100), panelWidth, 400));
				});

				b.onHoverOut(event -> {
					this.tooltips.clear();
				});
				b.onMouseDown(event -> {
					PlayerUI.DRAGGING_ITEM = true;
					if (item.isConsumable()) {
						Stats newStats = this.playState.getPlayer().getStats().concat(item.getStats());
						this.playState.getPlayer().setStats(newStats);

						if (item.getStats().getHp() > 0) {
							this.playState.getPlayer().drinkHp();
						} else if (item.getStats().getMp() > 0) {
							this.playState.getPlayer().drinkMp();
						}
						this.tooltips.remove(item.getUid());
						this.getPlayState().getPlayer().getInventory()[actualIdx] = null;
						this.inventory[actualIdx] = null;
						PlayerUI.DRAGGING_ITEM = false;
						this.tooltips.clear();
					}
				});
				b.onMouseUp(event -> {
					PlayerUI.DRAGGING_ITEM = false;
					if (this.overlapsEquipment(event)) {
						Slots currentEquip = this.inventory[item.getTargetSlot()];
						GameItem itemClone = currentEquip.getItem().clone();
						this.getPlayState().getPlayer().getInventory()[item.getTargetSlot()] = item;
						this.getPlayState().getPlayer().getInventory()[actualIdx] = itemClone;

						this.setEquipment(this.getPlayState().getPlayer().getInventory());
					} else if (this.overlapsInventory(event)) {
						Slots dropped = this.getOverlapping(event);
						if (dropped != null) {
							int idx = this.getOverlapIdx(event);
							GameItem swap = dropped.getItem().clone();
							this.getPlayState().getPlayer().getInventory()[actualIdx] = swap;
							this.getPlayState().getPlayer().getInventory()[idx] = item;
							this.setEquipment(this.getPlayState().getPlayer().getInventory());
						} else {
							int idx = this.getOverlapIdx(event);
							this.getPlayState().getPlayer().getInventory()[idx] = item;
							this.setEquipment(this.getPlayState().getPlayer().getInventory());
						}

					} else if (this.overlapsGround(event)) {
						GameItem toDrop = item.clone();
						this.getPlayState().getPlayer().getInventory()[actualIdx] = null;
						this.playState.getRealm().addLootContainer(
								new LootContainer(this.playState.getPlayer().getPos().clone(), toDrop));
						this.setEquipment(this.getPlayState().getPlayer().getInventory());
					}
				});
				this.inventory[actualIdx] = new Slots(b, item);
			}
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

	private boolean overlapsGround(Vector2f pos) {
		final int panelWidth = (GamePanel.width / 5);

		AABB currBounds = new AABB(new Vector2f(0, 0), GamePanel.width - panelWidth, GamePanel.height);

		return currBounds.inside((int) pos.x, (int) pos.y);
	}

	private Slots getOverlapping(Vector2f pos) {
		Slots[] equipSlots = this.getSlots(4, 12);
		for (Slots s : equipSlots) {
			if ((s == null) || (s.getButton() == null)) {
				continue;
			}
			if (s.getButton().getBounds().inside((int) pos.x, (int) pos.y))
				return s;
		}
		return null;
	}

	private boolean overlapsInventory(Vector2f pos) {
		Slots invSlots = this.getSlot(4);
		final int panelWidth = (GamePanel.width / 5);

		final int startX = GamePanel.width - panelWidth;
		final int startY = 450;
		AABB currBounds = new AABB(new Vector2f(startX, startY), panelWidth, 128);
		AABB bounds = new AABB(currBounds.getPos().clone(), (int) currBounds.getWidth() * 4,
				(int) currBounds.getHeight() * 4);

		return bounds.inside((int) pos.x, (int) pos.y);

	}

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

			Stats stats = this.playState.getPlayer().getComputedStats();

			g.setColor(Color.WHITE);

			int xOffset = 128;
			int yOffset = 42;
			int startY = 350;

			Vector2f posHp = new Vector2f(GamePanel.width - 64, 128 + 32);
			Vector2f posMp = posHp.clone(0, 64);
			g.drawString("" + this.playState.getPlayer().getHealth(), posHp.x, posHp.y);

			g.drawString("" + this.playState.getPlayer().getMana(), posMp.x, posMp.y);

			g.drawString("att :" + stats.getAtt(), startX, startY);
			g.drawString("spd :" + stats.getSpd(), startX, startY + (1 * yOffset));
			g.drawString("vit :" + stats.getVit(), startX, startY + (2 * yOffset));

			g.drawString("def :" + stats.getDef(), startX + xOffset, startY);
			g.drawString("dex :" + stats.getDex(), startX + xOffset, startY + (1 * yOffset));
			g.drawString("wis :" + stats.getWis(), startX + xOffset, startY + (2 * yOffset));

		}
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

		for (int i = 0; i < this.groundLoot.length; i++) {
			Slots curr = this.groundLoot[i];
			if (curr != null) {
				if ((curr.getDragPos() == null)) {
					this.groundLoot[i].render(g, new Vector2f(startX + (i * 64), 650));
				} else {
					this.groundLoot[i].render(g, curr.getDragPos());

				}
			}
		}

		for (ItemTooltip tip : this.tooltips.values()) {
			tip.render(g);
		}

		this.hp.render(g);
		this.mp.render(g);
		this.renderStats(g);
	}
}