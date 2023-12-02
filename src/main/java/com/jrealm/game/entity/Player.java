package com.jrealm.game.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Map;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.states.PlayState;
import com.jrealm.game.util.Camera;
import com.jrealm.game.util.Cardinality;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;
import com.jrealm.net.Streamable;
import com.jrealm.net.client.packet.UpdatePacket;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Player extends Entity implements Streamable<Player>{
	private Camera cam;
	private Cardinality cardinality = Cardinality.EAST;
	private GameItem[] inventory;
	private Stats stats;
	private long lastStatsTime = 0l;
	private LootContainer currentLootContainer;
	private int classId;

	private boolean headless = false;
	public Player(long id, Camera cam, SpriteSheet sprite, Vector2f origin, int size, CharacterClass characterClass) {
		super(id, sprite, origin, size);
		this.classId = characterClass.classId;
		this.resetEffects();
		this.cam = cam;
		this.size = size;
		this.bounds.setWidth(this.size);
		this.bounds.setHeight(this.size);

		this.hitBounds.setWidth(this.size);
		this.hitBounds.setHeight(this.size);

		this.ani.setNumFrames(2, this.UP);
		this.ani.setNumFrames(2, this.DOWN);
		this.ani.setNumFrames(2, this.RIGHT);
		this.ani.setNumFrames(2, this.LEFT);
		this.ani.setNumFrames(2, this.ATTACK + this.RIGHT);
		this.ani.setNumFrames(2, this.ATTACK + this.LEFT);
		this.ani.setNumFrames(2, this.ATTACK + this.UP);
		this.ani.setNumFrames(2, this.ATTACK + this.DOWN);

		this.hasIdle = false;
		this.health = this.maxHealth = this.defaultMaxHealth = 500;
		this.mana = this.maxMana = this.defaultMaxMana = 100;

		this.resetInventory();

		this.stats = new Stats();
		this.stats.setVit((short) 5);
		this.stats.setDex((short) 5);
		this.stats.setSpd((short) 5);
		this.stats.setAtt((short) 5);
		this.stats.setWis((short) 5);
	}

	public Player(long id, Vector2f origin, int size, CharacterClass characterClass) {
		super(id, origin, size);
		this.classId = characterClass.classId;
		this.resetEffects();
		this.size = size;
		this.bounds.setWidth(this.size);
		this.bounds.setHeight(this.size);

		this.hitBounds.setWidth(this.size);
		this.hitBounds.setHeight(this.size);
		this.hasIdle = false;
		this.health = this.maxHealth = this.defaultMaxHealth = 500;
		this.mana = this.maxMana = this.defaultMaxMana = 100;

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
	
	public int firstEmptyInvSlot() {
		for(int i = 4; i<this.inventory.length; i++) {
			if(this.inventory[i]==null) {
				return i;
			}
		}
		return -1;
	}

	public boolean equipSlot(int slot, GameItem item) {
		this.inventory[slot] = item;
		return true;
	}

	public void equipSlots(Map<Integer, GameItem> items) {
		for (Map.Entry<Integer, GameItem> entry : items.entrySet()) {
			this.equipSlot(entry.getKey(), entry.getValue());
		}
	}

	public GameItem getSlot(int slot) {
		return this.inventory[slot];
	}

	public GameItem[] getSlots(int start, int end) {
		int size = end - start;
		int idx = 0;
		GameItem[] items = new GameItem[size];
		for (int i = start; i < end; i++) {
			items[idx++] = this.inventory[i];
		}

		return items;
	}

	public int getWeaponId() {
		GameItem weapon = this.getSlot(0);
		return weapon == null ? -1 : weapon.getDamage().getProjectileGroupId();
	}

	public GameItem getAbility() {
		GameItem weapon = this.getSlot(1);
		return weapon;
	}

	public Cardinality getCardinality() {
		return this.cardinality;
	}

	public void resetPosition() {
		this.pos.x = (GamePanel.width / 2) - (this.size / 2);
		PlayState.map.x = 0;
		this.cam.getPos().x = 0;

		this.pos.y = (GamePanel.height / 2) - (this.size / 2);
		PlayState.map.y = 0;
		this.cam.getPos().y = 0;
		// sprite.getSprite(spriteX, spriteY)
		this.setAnimation(this.RIGHT, this.sprite.getSpriteArray(this.RIGHT), 10);
	}

	@Override
	public void update(double time) {
		super.update(time);
		Stats stats = this.getComputedStats();

		this.cam.update();
		if ((stats.getHp() > 0) && (this.getMaxHealth() == this.getDefaultMaxHealth())) {
			//	this.setMaxHealth(this.getMaxHealth() + stats.getHp());
		} else if (stats.getHp() == 0) {
			this.setMaxHealth(this.getDefaultMaxHealth());
			if (this.getHealth() > this.getMaxHealth()) {
				this.setHealth(this.getMaxHealth(), 0, false);
			}
		}

		if ((stats.getMp() > 0) && (this.getMaxMana() == this.getDefaultMaxMana())) {
			this.setMaxMana(this.getMaxMana() + stats.getMp());
		} else if (stats.getHp() == 0) {
			this.setMaxMana(this.getDefaultMaxMana());
			if (this.getMana() > this.getMaxMana()) {
				this.setMana(this.getMaxMana());
			}
		}

		if (((System.currentTimeMillis() - this.lastStatsTime) >= 1000)) {
			this.lastStatsTime = System.currentTimeMillis();

			if (this.hasEffect(EffectType.HEALING)) {
				stats.setVit((short) (stats.getVit() * 2));
			}
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
		this.defaultMaxHealth += 5;
		this.maxHealth += 5;
	}

	public void drinkMp() {
		this.defaultMaxMana += 5;
		this.maxMana += 5;
	}

	@Override
	public void render(Graphics2D g) {
		// g.setColor(Color.green);
		// g.drawRect((int) (this.pos.getWorldVar().x + this.bounds.getXOffset()), (int)
		// (this.pos.getWorldVar().y + this.bounds.getYOffset()), (int)
		// this.bounds.getWidth(), (int) this.bounds.getHeight());
		Color c = new Color(0f, 0f, 0f, 1f);
		g.setColor(c);
		java.awt.Font currentFont = g.getFont();
		java.awt.Font newFont = currentFont.deriveFont(currentFont.getSize() * 0.50F);
		g.setFont(newFont);
		g.fillOval((int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y) + 24, this.size, this.size / 2);
		if(this.getName()!=null) {
			g.drawString(this.getName(), (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y) + 64);
		}
		if (this.hasEffect(EffectType.INVISIBLE)) {
			if (!this.getSprite().hasEffect(Sprite.EffectEnum.SEPIA)) {
				this.getSprite().setEffect(Sprite.EffectEnum.SEPIA);
			}
		}

		if (this.hasEffect(EffectType.HEALING)) {
			if (!this.getSprite().hasEffect(Sprite.EffectEnum.REDISH)) {
				this.getSprite().setEffect(Sprite.EffectEnum.REDISH);
			}
		}

		if (this.hasEffect(EffectType.SPEEDY)) {
			if (!this.getSprite().hasEffect(Sprite.EffectEnum.DECAY)) {
				this.getSprite().setEffect(Sprite.EffectEnum.DECAY);
			}
		}

		if (this.hasNoEffects()) {
			if (!this.getSprite().hasEffect(Sprite.EffectEnum.NORMAL)) {
				this.getSprite().setEffect(Sprite.EffectEnum.NORMAL);
			}
		}

		if (this.useRight && this.left) {
			g.drawImage(this.ani.getImage().image, (int) (this.pos.getWorldVar().x) + this.size,
					(int) (this.pos.getWorldVar().y), -this.size, this.size, null);
		} else {
			g.drawImage(this.ani.getImage().image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y),
					this.size, this.size, null);
		}
		g.setFont(currentFont);
	}

	public void input(MouseHandler mouse, KeyHandler key) {
		Stats stats = this.getComputedStats();

		if (!this.isFallen()) {
			if (key.up.down) {
				this.up = true;
			} else {
				this.up = false;
			}
			if (key.down.down) {
				this.down = true;
			} else {
				this.down = false;
			}
			if (key.left.down) {
				this.left = true;
			} else {
				this.left = false;
			}
			if (key.right.down) {
				this.right = true;
			} else {
				this.right = false;
			}

			float maxSpeed = 2.2f + (stats.getSpd() * 0.05f);
			if (this.hasEffect(EffectType.SPEEDY)) {
				maxSpeed *= 1.5;
			}
			this.maxSpeed = maxSpeed;
			this.cam.setMaxSpeed(maxSpeed);

			if (this.up && this.down) {
				this.up = false;
				this.down = false;
			}

			if (this.right && this.left) {
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

	public void applyUpdate(UpdatePacket packet, PlayState state) {
		this.name = packet.getPlayerName();
		this.stats = packet.getStats();
		this.inventory = packet.getInventory();
		for(GameItem item: this.inventory) {
			if(item!=null) {
				GameDataManager.loadSpriteModel(item);
			}
		}
		this.health = packet.getHealth();
		this.maxHealth = packet.getMaxHealth();
		this.mana = packet.getMana();
		this.maxMana = packet.getMaxMana();
		this.setEffectIds(packet.getEffectIds());
		this.setEffectTimes(packet.getEffectTimes());
		if(packet.getPlayerId()==state.getPlayerId()) {
			state.getPui().setEquipment(this.inventory);
		}
	}

	public boolean getIsUp() {
		return this.up;
	}

	public boolean getIsDown() {
		return this.down;
	}

	public boolean getIsLeft() {
		return this.left;
	}

	public boolean getIsRight() {
		return this.right;
	}

	@Override
	public void write(DataOutputStream stream) throws Exception {
		stream.writeLong(this.getId());
		stream.writeUTF(this.getName());
		stream.writeInt(this.getClassId());
		stream.writeShort(this.getSize());
		stream.writeFloat(this.getPos().x);
		stream.writeFloat(this.getPos().y);
		stream.writeFloat(this.dx);
		stream.writeFloat(this.dy);
	}

	@Override
	public Player read(DataInputStream stream) throws Exception {
		long id = stream.readLong();
		String name = stream.readUTF();
		int classId = stream.readInt();
		short size = stream.readShort();
		float posX = stream.readFloat();
		float posY = stream.readFloat();
		float dX = stream.readFloat();
		float dY = stream.readFloat();
		Player player = Player.fromData(id, name, new Vector2f(posX, posY), size, CharacterClass.valueOf(classId));
		player.setDx(dX);
		player.setDy(dY);
		player.setName(name);
		return player;
	}

	public static Player fromData(long id, String name, Vector2f origin, int size, CharacterClass characterClass) {
		Camera c = new Camera(new AABB(new Vector2f(0, 0), GamePanel.width + 64, GamePanel.height + 64));
		SpriteSheet sheet = GameDataManager.loadClassSprites(characterClass);
		Player player =  new Player(id, c, sheet, origin, size, characterClass);
		player.setName(name);
		return player;
	}

	public static Player fromStream(DataInputStream stream) throws Exception {
		long id = stream.readLong();
		String name = stream.readUTF();
		int classId = stream.readInt();
		short size = stream.readShort();
		float posX = stream.readFloat();
		float posY = stream.readFloat();
		float dX = stream.readFloat();
		float dY = stream.readFloat();
		Player player = new Player(id, new Vector2f(posX, posY), size, CharacterClass.valueOf(classId));
		player.setDx(dX);
		player.setDy(dY);
		player.setName(name);
		return player;
	}

	@Override
	public String toString() {
		return this.getId()+" , Pos: "+this.pos.toString()+", Class: "+this.getClassId()+", Headless: "+this.isHeadless();
	}
}
