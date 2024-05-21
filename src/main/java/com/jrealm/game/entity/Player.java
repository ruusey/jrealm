package com.jrealm.game.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.jrealm.account.dto.CharacterStatsDto;
import com.jrealm.account.dto.GameItemRefDto;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.contants.EffectType;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.LootContainer;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.CharacterClassModel;
import com.jrealm.game.state.PlayState;
import com.jrealm.game.util.Cardinality;
import com.jrealm.game.util.KeyHandler;
import com.jrealm.game.util.MouseHandler;
import com.jrealm.game.util.Tuple;
import com.jrealm.net.Streamable;
import com.jrealm.net.client.packet.UpdatePacket;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Player extends Entity implements Streamable<Player>{
	private Cardinality cardinality = Cardinality.EAST;
	private GameItem[] inventory;
	private long lastStatsTime = 0l;
	private LootContainer currentLootContainer;
	private int classId;
	private String accountUuid;
	private String characterUuid;
	private long experience;
	private Stats stats;

	private boolean headless = false;

	public Player(long id, Vector2f origin, int size, CharacterClass characterClass) {
		super(id, origin, size);
		this.resetEffects();
		this.resetInventory();
		this.classId = characterClass.classId;
		this.size = size;
		this.experience = 0;
		this.bounds.setWidth(this.size);
		this.bounds.setHeight(this.size);

		this.hitBounds.setWidth(this.size);
		this.hitBounds.setHeight(this.size);
		CharacterClassModel classModel = GameDataManager.CHARACTER_CLASSES.get(this.classId);
		this.health = classModel.getBaseStats().getHp();
		this.mana = classModel.getBaseStats().getMp();

		this.stats = classModel.getBaseStats();
	}

	public void applyStats(CharacterStatsDto stats) {
		this.setExperience(stats.getXp());
		this.health = stats.getHp();
		this.mana = stats.getMp();
		this.stats.setHp(stats.getHp().shortValue());
		this.stats.setMp(stats.getMp().shortValue());
		this.stats.setDef(stats.getDef().shortValue());
		this.stats.setAtt(stats.getAtt().shortValue());
		this.stats.setSpd(stats.getSpd().shortValue());
		this.stats.setDex(stats.getDex().shortValue());
		this.stats.setVit(stats.getVit().shortValue());
		this.stats.setWis(stats.getWis().shortValue());
	}

	public Set<GameItemRefDto> serializeItems(){
		final Set<GameItemRefDto> res = new HashSet<>();
		for(int i=0;i<this.inventory.length; i++) {
			GameItem item = this.inventory[i];
			if(item!=null) {
				res.add(GameItemRefDto.builder().itemId(item.getItemId()).itemUuid(item.getUid()).slotIdx(i).build());
			}
		}
		return res;
	}

	public CharacterStatsDto serializeStats() {
		return CharacterStatsDto.builder()
				.xp(this.getExperience())
				.hp(Integer.valueOf((int)this.stats.getHp()))
				.mp(Integer.valueOf((int) this.stats.getMp()))
				.def(Integer.valueOf((int)this.stats.getDef()))
				.att(Integer.valueOf((int)this.stats.getAtt()))
				.spd(Integer.valueOf((int)this.stats.getSpd()))
				.dex(Integer.valueOf((int)this.stats.getDex()))
				.vit(Integer.valueOf((int)this.stats.getVit()))
				.wis(Integer.valueOf((int)this.stats.getWis()))
				.build();
	}

	private void resetInventory() {
		this.inventory = new GameItem[20];
	}

	public int firstEmptyInvSlot() {
		for(int i = 4; i<this.inventory.length; i++) {
			if(this.inventory[i]==null)
				return i;
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

	@Override
	public void update(double time) {
		super.update(time);
		Stats stats = this.getComputedStats();
		float currentHealthPercent = (float)this.getHealth()/(float)this.getComputedStats().getHp();
		float currentManaPercent = (float)this.getMana()/(float)this.getComputedStats().getMp();

		this.setHealthpercent(currentHealthPercent);
		this.setManapercent(currentManaPercent);

		if (((Instant.now().toEpochMilli() - this.lastStatsTime) >= 1000)) {
			this.lastStatsTime = System.currentTimeMillis();
			float mult = 1.0f;
			if (this.hasEffect(EffectType.HEALING)) {
				mult = 1.5f;
			}
			final int vit = (int) ((0.24f * (stats.getVit() + 4.2f)) * mult);
			if (this.getHealth() < stats.getHp()) {
				int targetHealth = this.getHealth() + vit;
				if (targetHealth > stats.getHp()) {
					targetHealth = stats.getHp();
				}
				this.setHealth(targetHealth);
			} else if (this.getHealth() > stats.getHp()) {
				int targetHealth = this.getHealth() - stats.getHp();
				this.setHealth(this.getHealth() - targetHealth);
			}
			final int wis = (int) ((0.12f * (stats.getWis() + 4.2f)));
			if (this.getMana() < stats.getMp()) {
				int targetMana = this.getMana() + wis;
				if (targetMana > stats.getMp()) {
					targetMana = stats.getMp();
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
		this.stats.setHp((short) (this.stats.getHp() + 5));
	}

	public void drinkMp() {
		this.stats.setMp((short) (this.stats.getMp() + 5));
	}
	
	@Override
	public float getHealthpercent() {
		return this.healthpercent;
	}

	@Override
	public int getHealth() {
		return this.health;
	}

	@Override
	public int getMana() {
		return this.mana;
	}

	@Override
	public void render(Graphics2D g) {
		Color c = new Color(0f, 0f, 0f, 0.35f);
		g.setColor(c);
		java.awt.Font currentFont = g.getFont();
		java.awt.Font newFont = currentFont.deriveFont(currentFont.getSize() * 0.50F);
		g.setFont(newFont);
		g.fillOval((int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y) + 24, this.size, this.size / 2);
		g.setColor(Color.BLACK);
		if(this.getName()!=null) {
			g.drawString(this.getName(), (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y) + 64);
		}
		if (this.hasEffect(EffectType.INVISIBLE)) {
			if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.SEPIA)) {
				this.getSpriteSheet().setEffect(Sprite.EffectEnum.SEPIA);
			}
		}

		if (this.hasEffect(EffectType.HEALING)) {
			if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.REDISH)) {
				this.getSpriteSheet().setEffect(Sprite.EffectEnum.REDISH);
			}
		}

		if (this.hasEffect(EffectType.SPEEDY)) {
			if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.DECAY)) {
				this.getSpriteSheet().setEffect(Sprite.EffectEnum.DECAY);
			}
		}

		if (this.hasNoEffects()) {
			if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.NORMAL)) {
				this.getSpriteSheet().setEffect(Sprite.EffectEnum.NORMAL);
			}
		}

		if (this.left) {
			g.drawImage(this.getSpriteSheet().getCurrentFrame(), (int) (this.pos.getWorldVar().x) + this.size,
					(int) (this.pos.getWorldVar().y), -this.size, this.size, null);
		} else {
			g.drawImage(this.getSpriteSheet().getCurrentFrame(), (int) (this.pos.getWorldVar().x),
					(int) (this.pos.getWorldVar().y),
					this.size, this.size, null);
		}
		g.setFont(currentFont);
	}

	public void input(MouseHandler mouse, KeyHandler key) {
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

	public int getUpperExperienceBound() {
		if (this.experience > GameDataManager.EXPERIENCE_LVLS.maxExperience())
			return GameDataManager.EXPERIENCE_LVLS.maxExperience();

		final Tuple<Integer, Integer> expRange = GameDataManager.EXPERIENCE_LVLS.getParsedMap()
				.get(GameDataManager.EXPERIENCE_LVLS.getLevel(this.experience));

		return expRange.getY();
	}

	public float getExperiencePercent() {
		if(this.experience>GameDataManager.EXPERIENCE_LVLS.maxExperience())
			return 1.0f;

		final Tuple<Integer, Integer> expRange = GameDataManager.EXPERIENCE_LVLS
				.getParsedMap().get(GameDataManager.EXPERIENCE_LVLS.getLevel(this.experience));


		return ((float) this.experience / (float) expRange.getY());
	}

	public float getHealthPercent() {
		return this.healthpercent;
	}

	public float getManaPercent() {
		return this.manapercent;
	}

	public void incrementExperience(long experience) {
		long newExperience = this.getExperience() + experience;
		int currentLevel = GameDataManager.EXPERIENCE_LVLS.getLevel(this.experience);
		int newLevel = GameDataManager.EXPERIENCE_LVLS.getLevel(newExperience);
		final CharacterClassModel classModel = GameDataManager.CHARACTER_CLASSES.get(this.getClassId());
		if (newLevel > currentLevel) {
			this.setStats(this.getStats().concat(classModel.getRandomLevelUpStats()));
		}
		this.setExperience(newExperience);
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
		this.mana = packet.getMana();
		this.setEffectIds(packet.getEffectIds());
		this.setEffectTimes(packet.getEffectTimes());
		if(packet.getPlayerId()==state.getPlayerId()) {
			state.getPui().setEquipment(this.inventory);
		}
		this.experience = packet.getExperience();
	}

	public boolean isStatMaxed(int statIdx) {
		CharacterClassModel characterClass = GameDataManager.CHARACTER_CLASSES.get(this.classId);
		Stats maxStats = characterClass.getMaxStats();
		boolean maxed = false;
		switch(statIdx) {
		case 0:
			maxed = this.stats.getHp() >= maxStats.getHp();
			break;
		case 1:
			maxed = this.stats.getMp() >= maxStats.getMp();
			break;
		case 2:
			maxed = this.stats.getDef() >= maxStats.getDef();
			break;
		case 3:
			maxed = this.stats.getAtt() >= maxStats.getAtt();
			break;
		case 4:
			maxed = this.stats.getSpd() >= maxStats.getSpd();
			break;
		case 5:
			maxed = this.stats.getDex() >= maxStats.getDex();
			break;
		case 6:
			maxed = this.stats.getVit() >= maxStats.getVit();
			break;
		case 7:
			maxed = this.stats.getWis() >= maxStats.getWis();
			break;
		}
		return maxed;
	}

	public boolean canConsume(final GameItem item) {
		boolean canConsume = true;
		if(((item.getStats().getHp()>0) && this.isStatMaxed(0)) || ((item.getStats().getMp()>0) && this.isStatMaxed(1))) {
			canConsume = false;
		} else if(((item.getStats().getMp()>0) && this.isStatMaxed(1)) || ((item.getStats().getDef()>0) && this.isStatMaxed(2))) {
			canConsume = false;
		} else if(((item.getStats().getAtt()>0) && this.isStatMaxed(3)) || ((item.getStats().getSpd()>0) && this.isStatMaxed(4))) {
			canConsume = false;
		} else if(((item.getStats().getDex()>0) && this.isStatMaxed(5)) || ((item.getStats().getVit()>0) && this.isStatMaxed(6))) {
			canConsume = false;
		} else if((item.getStats().getWis()>0) && this.isStatMaxed(7)) {
			canConsume = false;
		}
		return canConsume;
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
		stream.writeUTF(this.accountUuid);
		stream.writeUTF(this.characterUuid);
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
		String accountUuid = stream.readUTF();
		String characterUuid = stream.readUTF();
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
		player.setAccountUuid(accountUuid);
		player.setCharacterUuid(characterUuid);
		return player;
	}

	public static Player fromData(long id, String name, Vector2f origin, int size, CharacterClass characterClass) {
		Player player = new Player(id, origin, size, characterClass);
		player.setName(name);
		return player;
	}

	public static Player fromStream(DataInputStream stream) throws Exception {
		long id = stream.readLong();
		String name = stream.readUTF();
		String accountUuid = stream.readUTF();
		String characterUuid = stream.readUTF();
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
		player.setAccountUuid(accountUuid);
		player.setCharacterUuid(characterUuid);
		return player;
	}

	@Override
	public String toString() {
		return this.getId()+" , Pos: "+this.pos.toString()+", Class: "+this.getClassId()+", Headless: "+this.isHeadless();
	}
}
