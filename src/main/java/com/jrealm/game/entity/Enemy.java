package com.jrealm.game.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.ProjectilePositionMode;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerClient;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.game.script.ScriptBase;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.Streamable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public abstract class Enemy extends Entity implements Streamable<Enemy>{
	private static final int IDLE_FRAMES = 12;
	protected EnemyModel model;
	protected int chaseRange;
	protected int attackRange;
	protected int xOffset;
	protected int yOffset;

	private long lastShotTick = 0;
	private int enemyId;
	private int weaponId = -1;
	private int idleTime = 0;

	public Enemy(long id, int enemyId, Vector2f origin, int size, int weaponId) {
		super(id, origin, size);
		this.model = GameDataManager.ENEMIES.get(enemyId);
		this.enemyId = enemyId;
		this.weaponId = weaponId;
	}

	public void chase(Player player) {

		if ((player == null) || player.hasEffect(EffectType.INVISIBLE)) {
			this.up = false;
			this.down = false;
			this.right = false;
			this.left = false;
			return;
		}

		if ((this.getPos().distanceTo(player.getPos()) < this.chaseRange)
				&& (this.getPos().distanceTo(player.getPos()) >= this.attackRange)) {
			if (this.pos.y > (player.pos.y + 1)) {
				this.up = true;
			} else {
				this.up = false;
			}
			if (this.pos.y < (player.pos.y - 1)) {
				this.down = true;
			} else {
				this.down = false;
			}

			if (this.pos.x > (player.pos.x + 1)) {
				this.left = true;
			} else {
				this.left = false;
			}
			if (this.pos.x < (player.pos.x - 1)) {
				this.right = true;
			} else {
				this.right = false;
			}
		}
	}

	public void update(RealmManagerClient mgr, double time) {
		Player player = mgr.getClosestPlayer(this.getPos(), this.chaseRange);
		super.update(time);
		this.move();
		if (player == null)
			return;
		this.chase(player);

		if (this.hasEffect(EffectType.PARALYZED)) {
			this.up = false;
			this.down = false;
			this.right = false;
			this.left = false;
			return;
		}
		if (!this.isFallen()) {
			this.pos.x += this.dx;
			this.pos.y += this.dy;
		}
	}

	public void update(long realmId, RealmManagerServer mgr, double time) {
		final Realm targetRealm = mgr.getRealms().get(realmId);
		Player player = mgr.getClosestPlayer(targetRealm.getRealmId(), this.getPos(), this.chaseRange);
		super.update(time);
		this.move();
		if (player == null)
			return;
		this.chase(player);
		final boolean notInvisible = !player.hasEffect(EffectType.INVISIBLE);
		if ((this.getPos().distanceTo(player.getPos()) < this.attackRange)
				&& notInvisible) {
			this.attack = true;

			int dex = (int) ((6.5 * (this.model.getStats().getDex() + 17.3)) / 75);
			boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > (1000 / dex));

			if (canShoot) {
				this.lastShotTick = System.currentTimeMillis();
				Player target = player;
				ScriptBase script = mgr.getEnemyScript(this.enemyId);
				if(script==null) {
					Vector2f dest = target.getBounds().getPos().clone(target.getSize() / 2, target.getSize() / 2);

					Vector2f source = this.getPos().clone(this.getSize() / 2, this.getSize() / 2);
					float angle = Bullet.getAngle(source, dest);
					ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(this.weaponId);

					for (Projectile p : group.getProjectiles()) {
						if (p.getPositionMode().equals(ProjectilePositionMode.TARGET_PLAYER)) {
							mgr.addProjectile(targetRealm.getRealmId(), 0l, player.getId(), this.getWeaponId(),
									p.getProjectileId(),
									source.clone(),
									angle + Float.parseFloat(p.getAngle()), p.getSize(), p.getMagnitude(), p.getRange(),
									p.getDamage(), true, p.getFlags(), p.getAmplitude(), p.getFrequency());
						} else if (p.getPositionMode().equals(ProjectilePositionMode.ABSOLUTE)) {
							mgr.addProjectile(targetRealm.getRealmId(), 0l, player.getId(), this.getWeaponId(),
									p.getProjectileId(),
									source.clone(), Float.parseFloat(p.getAngle()),
									p.getSize(), p.getMagnitude(), p.getRange(), p.getDamage(), true, p.getFlags(),
									p.getAmplitude(), p.getFrequency());
						}
					}
				}else {
					final Runnable enemyAttack = () -> {
						try {
							script.attack(targetRealm, this, target);
						} catch (Exception e) {
							Enemy.log.error("Failed to invoke enemy attack script. Reason: {}", e);
						}
					};
					WorkerThread.doAsync(enemyAttack);
				}

			}
		} else {
			this.attack = false;
		}
		if (this.hasEffect(EffectType.PARALYZED)) {
			this.up = false;
			this.down = false;
			this.right = false;
			this.left = false;
			return;
		}

		this.pos.x += this.dx;
		this.pos.y += this.dy;

		if (this.idleTime >= Enemy.IDLE_FRAMES) {
			this.up = Realm.RANDOM.nextBoolean();
			this.down = Realm.RANDOM.nextBoolean();
			this.left = Realm.RANDOM.nextBoolean();
			this.right = Realm.RANDOM.nextBoolean();
			this.idleTime = 0;
		} else {
			this.idleTime++;
		}
	}

	@Override
	public void render(Graphics2D g) {

		Color c = new Color(0f, 0f, 0f, .4f);
		g.setColor(c);
		g.fillOval((int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y) + (this.size / 2), this.size,
				this.size / 2);
		if (this.left) {
			g.drawImage(this.getSpriteSheet().getCurrentFrame(), (int) (this.pos.getWorldVar().x) + this.size,
					(int) (this.pos.getWorldVar().y), -this.size, this.size, null);
		} else {
			g.drawImage(this.getSpriteSheet().getCurrentFrame(), (int) (this.pos.getWorldVar().x),
					(int) (this.pos.getWorldVar().y),
					this.size, this.size, null);
		}

		if (this.hasEffect(EffectType.PARALYZED)) {
			if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.GRAYSCALE)) {
				this.getSpriteSheet().setEffect(Sprite.EffectEnum.GRAYSCALE);
			}
		}

		if (this.hasEffect(EffectType.STUNNED)) {
			if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.DECAY)) {
				this.getSpriteSheet().setEffect(Sprite.EffectEnum.DECAY);
			}
		}

		if (this.hasNoEffects()) {
			if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.NORMAL)) {
				this.getSpriteSheet().setEffect(Sprite.EffectEnum.NORMAL);
			}
		}

		// Health Bar UI
		g.setColor(Color.red);
		g.fillRect((int) (this.pos.getWorldVar().x + this.bounds.getXOffset()), (int) (this.pos.getWorldVar().y - 5),
				24, 5);

		g.setColor(Color.green);
		g.fillRect((int) (this.pos.getWorldVar().x + this.bounds.getXOffset()), (int) (this.pos.getWorldVar().y - 5),
				(int) (24 * this.healthpercent), 5);

	}

	// TODO: Add enemy type identifier
	@Override
	public void write(DataOutputStream stream) throws Exception{
		stream.writeLong(this.getId());
		stream.writeInt(this.getEnemyId());
		stream.writeInt(this.getWeaponId());
		stream.writeShort(this.getSize());
		stream.writeFloat(this.getPos().x);
		stream.writeFloat(this.getPos().y);
		stream.writeFloat(this.dx);
		stream.writeFloat(this.dy);
	}

	@Override
	public Enemy read(DataInputStream stream) throws Exception {
		final long id = stream.readLong();
		final int enemyId = stream.readInt();
		final int weaponId = stream.readInt();
		final short size = stream.readShort();
		final float posX = stream.readFloat();
		final float posY = stream.readFloat();
		final float dx = stream.readFloat();
		final float dy = stream.readFloat();

		final Enemy newEnemy = new Monster(id, enemyId, new Vector2f(posX, posY), size, weaponId);
		newEnemy.setDy(dy);
		newEnemy.setDx(dx);
		return newEnemy;
	}

	public static Enemy fromStream(DataInputStream stream) throws Exception{
		final long id = stream.readLong();
		final int enemyId = stream.readInt();
		final int weaponId = stream.readInt();
		final short size = stream.readShort();
		final float posX = stream.readFloat();
		final float posY = stream.readFloat();
		final float dx = stream.readFloat();
		final float dy = stream.readFloat();

		final Enemy newEnemy = new Monster(id, enemyId, new Vector2f(posX, posY), size, weaponId);
		newEnemy.setDy(dy);
		newEnemy.setDx(dx);
		return newEnemy;
	}

}