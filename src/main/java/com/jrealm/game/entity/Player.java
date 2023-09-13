package com.jrealm.game.entity;


import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import com.jrealm.game.GamePanel;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.states.PlayState;
import com.jrealm.game.tiles.TileManager;
import com.jrealm.game.tiles.blocks.NormBlock;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.Cardinality;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;

import lombok.Data;

@Data
public class Player extends Entity {

	private Camera cam;
	private ArrayList<GameObject> go;
	private TileManager tm;

	private Cardinality cardinality = Cardinality.EAST;
	private int weaponId;

	private GameItem[] inventory;

	private Stats stats;
	private long lastStatsTime = 0l;
	public Player(int id, Camera cam, SpriteSheet sprite, Vector2f origin, int size, TileManager tm) {
		super(id, sprite, origin, size);
		this.cam = cam;
		this.tm = tm;

		this.bounds.setWidth(64);
		this.bounds.setHeight(64);
		// this.bounds.setXOffset(16);
		// this.bounds.setYOffset(40);

		this.hitBounds.setWidth(64);
		this.hitBounds.setHeight(64);

		this.ani.setNumFrames(2, this.UP);
		this.ani.setNumFrames(2, this.DOWN);
		this.ani.setNumFrames(2, this.RIGHT);
		this.ani.setNumFrames(2, this.LEFT);
		this.ani.setNumFrames(2, this.ATTACK + this.RIGHT);
		this.ani.setNumFrames(2, this.ATTACK + this.LEFT);
		this.ani.setNumFrames(2, this.ATTACK + this.UP);
		this.ani.setNumFrames(2, this.ATTACK + this.DOWN);

		this.go = new ArrayList<GameObject>();

		for(int i = 0; i < sprite.getSpriteArray2().length; i++) {
			for(int j = 0; j < sprite.getSpriteArray2()[i].length; j++) {
				sprite.getSpriteArray2()[i][j].setEffect(Sprite.effect.NEGATIVE);
				sprite.getSpriteArray2()[i][j].saveColors();
			}
		}

		this.hasIdle = false;
		this.health = this.maxHealth = this.defaultMaxHealth = 500;
		this.mana = this.maxMana = this.defaultMaxMana = 100;
		this.name = "player";

		this.resetInventory();

		this.stats = new Stats();
		this.stats.setVit((short) 5);
		this.stats.setDex((short) 5);
		this.stats.setSpd((short) 5);
		this.stats.setAtt((short) 5);
		this.stats.setWis((short) 5);
	}

	private void resetInventory() {
		this.inventory = new GameItem[20];
	}

	public boolean equipSlot(int slot, GameItem item) {
		//		if (item.isConsumable() || (item.getTargetSlot() != slot))
		//			return false;
		this.inventory[slot] = item;

		return true;
	}


	public GameItem getSlot(int slot) {
		return this.inventory[slot];
	}

	public GameItem[] getSlots(int start, int end) {
		int size = end-start;
		int idx = 0;
		GameItem[] items = new GameItem[size];
		for(int i = start; i< end; i++) {
			items[idx++] = this.inventory[i];
		}

		return items;
	}

	public int getWeaponId() {
		GameItem weapon = this.getSlot(0);
		return weapon == null ? -1 : weapon.getDamage().getProjectileGroupId();
	}

	public Cardinality getCardinality() {
		return this.cardinality;
	}

	public void setTargetGameObject(GameObject go) {
		if(!this.go.contains(go)) {
			this.go.add(go);
		}
	}

	private void resetPosition() {
		System.out.println("Reseting Player... ");
		this.pos.x = (GamePanel.width / 2) - 32;
		PlayState.map.x = 0;
		this.cam.getPos().x = 0;

		this.pos.y = (GamePanel.height /2) - 32;
		PlayState.map.y = 0;
		this.cam.getPos().y = 0;
		// sprite.getSprite(spriteX, spriteY)
		this.setAnimation(this.RIGHT, this.sprite.getSpriteArray(this.RIGHT), 10);
	}

	@Override
	public void update(double time) {
		super.update(time);
		Stats stats = this.getComputedStats();

		this.attacking = this.isAttacking(time);

		if ((stats.getHp() > 0) && (this.getMaxHealth() == this.getDefaultMaxHealth())) {
			this.setMaxHealth(this.getMaxHealth() + stats.getHp());
		} else if (stats.getHp() == 0) {
			this.setMaxHealth(this.getDefaultMaxHealth());
		}

		if ((stats.getMp() > 0) && (this.getMaxMana() == this.getDefaultMaxMana())) {
			this.setMaxMana(this.getMaxMana() + stats.getMp());
		} else if (stats.getHp() == 0) {
			this.setMaxMana(this.getDefaultMaxMana());
		}

		if(!this.fallen) {
			this.move();
			if(!this.tc.collisionTile(this.dx, 0) && !this.bounds.collides(this.dx, 0, this.go)) {
				//PlayState.map.x += dx;
				this.pos.x += this.dx;
				this.xCol = false;
			} else {
				this.xCol = true;
			}
			if(!this.tc.collisionTile(0, this.dy) && !this.bounds.collides(0, this.dy, this.go)) {
				//PlayState.map.y += dy;
				this.pos.y += this.dy;
				this.yCol = false;
			} else {
				this.yCol = true;
			}

			this.tc.normalTile(this.dx, 0);
			this.tc.normalTile(0, this.dy);

		} else {
			this.xCol = true;
			this.yCol = true;
			if(this.ani.hasPlayedOnce()) {
				this.resetPosition();
				this.dx = 0;
				this.dy = 0;
				this.fallen = false;
			}
		}

		if (((System.currentTimeMillis() - this.lastStatsTime) >= 1000)) {
			this.lastStatsTime = System.currentTimeMillis();

			if (this.getHealth() < this.getMaxHealth()) {
				int targetHealth = this.getHealth() + stats.getVit();
				if (targetHealth > this.getMaxHealth()) {
					targetHealth = this.getMaxHealth();
				}
				this.setHealth(targetHealth, 0f, false);
			}

			if (this.getMana() < this.getMaxMana()) {
				int targetMana = this.getMana() + stats.getWis();
				if (targetMana > this.getMaxMana()) {
					targetMana = this.getMaxMana();
				}
				this.setMana(targetMana);
			}
		}

		NormBlock[] block = this.tm.getNormalTile(this.tc.getTile());
		for(int i = 0; i < block.length; i++) {
			if(block[i] != null) {
				block[i].getImage().restoreDefault();
			}
		}
	}

	public Stats getComputedStats() {
		Stats stats = this.stats.clone();
		GameItem[] equipment = this.getSlots(0, 4);
		for (GameItem item : equipment) {
			if (item != null) {
				stats = stats.concat(item.getStats());
			}
		}
		return stats;
	}

	public void drinkHp() {
		this.maxHealth += 5;
	}

	public void drinkMp() {
		this.maxMana += 5;
	}

	@Override
	public void render(Graphics2D g) {
		g.setColor(Color.green);
		g.drawRect((int) (this.pos.getWorldVar().x + this.bounds.getXOffset()), (int) (this.pos.getWorldVar().y + this.bounds.getYOffset()), (int) this.bounds.getWidth(), (int) this.bounds.getHeight());

		if(this.attack) {
			g.setColor(Color.red);
			g.drawRect((int) (this.hitBounds.getPos().getWorldVar().x + this.hitBounds.getXOffset()), (int) (this.hitBounds.getPos().getWorldVar().y + this.hitBounds.getYOffset()), (int) this.hitBounds.getWidth(), (int) this.hitBounds.getHeight());
		}

		if(this.isInvincible && ((GamePanel.tickCount % 30) >= 15)) {
			this.ani.getImage().setEffect(Sprite.effect.REDISH);
		} else {
			this.ani.getImage().restoreColors();
		}

		if(this.useRight && this.left) {
			g.drawImage(this.ani.getImage().image, (int) (this.pos.getWorldVar().x) + this.size, (int) (this.pos.getWorldVar().y), -this.size, this.size, null);
		} else {
			g.drawImage(this.ani.getImage().image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y), this.size, this.size, null);
		}
	}

	public void input(MouseHandler mouse, KeyHandler key) {
		Stats stats = this.getComputedStats();

		if(!this.fallen) {
			if(key.up.down) {
				this.up = true;
			} else {
				this.up = false;
			}
			if(key.down.down) {
				this.down = true;
			} else {
				this.down = false;
			}
			if(key.left.down) {
				this.left = true;
			} else {
				this.left = false;
			}
			if(key.right.down) {
				this.right = true;
			} else {
				this.right = false;
			}

			if(key.attack.down && this.canAttack) {
				this.attack = true;
				this.attacktime = System.nanoTime();
			} else if(!this.attacking) {
				this.attack = false;
			}

			if(key.shift.down) {
				this.maxSpeed = 8;
				this.cam.setMaxSpeed(7);
			} else {
				float maxSpeed = 3.0f + (stats.getSpd() * 0.05f);
				this.maxSpeed = maxSpeed;
				this.cam.setMaxSpeed(maxSpeed);
			}

			if(this.up && this.down) {
				this.up = false;
				this.down = false;
			}

			if(this.right && this.left) {
				this.right = false;
				this.left = false;
			}
		} else {
			this.up = false;
			this.down = false;
			this.right = false;
			this.left = false;
		}
	}
}
