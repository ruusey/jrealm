package com.jrealm.game.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.EffectType;
import com.jrealm.game.contants.ProjectilePositionMode;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.enemy.Monster;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.realm.Realm;
import com.jrealm.game.realm.RealmManagerServer;
import com.jrealm.net.Streamable;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class Enemy extends Entity implements Streamable<Enemy>{

	protected AABB sense;
	protected int r_sense;

	protected AABB attackrange;
	protected int r_attackrange;

	protected int xOffset;
	protected int yOffset;

	public long lastShotTick = 0;


	private int enemyId;
	private int weaponId = -1;

	public Enemy(long id, int enemyId, SpriteSheet sprite, Vector2f origin, int size, int weaponId) {
		super(id, sprite, origin, size);

		this.sense = new AABB(new Vector2f((origin.x + (size / 2)) - (this.r_sense / 2),
				(origin.y + (size / 2)) - (this.r_sense / 2)), this.r_sense);
		this.attackrange = new AABB(new Vector2f(
				(origin.x + this.bounds.getXOffset() + (this.bounds.getWidth() / 2)) - (this.r_attackrange / 2),
				(origin.y + this.bounds.getYOffset() + (this.bounds.getHeight() / 2)) - (this.r_attackrange / 2)),
				this.r_attackrange);
		this.enemyId = enemyId;
		this.weaponId = weaponId;
	}

	public Enemy(long id, int enemyId, Vector2f origin, int size, int weaponId) {
		super(id, origin, size);
		this.sense = new AABB(new Vector2f((origin.x + (size / 2)) - (this.r_sense / 2),
				(origin.y + (size / 2)) - (this.r_sense / 2)), this.r_sense);
		this.attackrange = new AABB(new Vector2f(
				(origin.x + this.bounds.getXOffset() + (this.bounds.getWidth() / 2)) - (this.r_attackrange / 2),
				(origin.y + this.bounds.getYOffset() + (this.bounds.getHeight() / 2)) - (this.r_attackrange / 2)),
				this.r_attackrange);
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

		AABB playerBounds = player.getBounds();
		if (this.sense.colCircleBox(playerBounds) && !this.attackrange.colCircleBox(playerBounds)) {
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
		} else {
			this.up = false;
			this.down = false;
			this.left = false;
			this.right = false;
		}
	}

	public void update(long realmId, RealmManagerServer mgr, double time) {
		final Realm targetRealm = mgr.getRealms().get(realmId);
		Player player = mgr.getClosestPlayer(targetRealm.getRealmId(), this.getPos(), this.r_sense);
		super.update(time);
		if (player == null)
			return;
		this.chase(player);
		this.move();

		if (this.teleported) {
			this.teleported = false;

			this.hitBounds = new AABB(this.pos, this.size, this.size);

			this.sense = new AABB(new Vector2f((this.pos.x + (this.size / 2)) - (this.r_sense / 2),
					(this.pos.y + (this.size / 2)) - (this.r_sense / 2)), this.r_sense);
			this.attackrange = new AABB(new Vector2f(
					(this.pos.x + this.bounds.getXOffset() + (this.bounds.getWidth() / 2)) - (this.r_attackrange / 2),
					(this.pos.y + this.bounds.getYOffset() + (this.bounds.getHeight() / 2)) - (this.r_attackrange / 2)),
					this.r_attackrange);
		}

		if (this.attackrange.colCircleBox(player.getBounds()) && !this.isInvincible
				&& !player.hasEffect(EffectType.INVISIBLE)) {
			this.attack = true;

			boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > 1500)
					&& !this.hasEffect(EffectType.STUNNED);

			if (canShoot) {
				this.lastShotTick = System.currentTimeMillis();
				Player target = player;
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
		if (!this.isFallen()) {
			//			if (!this.tc.collisionTile((TileMapObj)mgr.getRealm().getTileManager().getTm().get(1), mgr.getRealm().getTileManager().getTm().get(1).getBlocks(),
			//					this.dx,0)) {
			this.sense.getPos().x += this.dx;
			this.attackrange.getPos().x += this.dx;
			this.pos.x += this.dx;
			//			}
			//			if (!this.tc.collisionTile((TileMapObj)mgr.getRealm().getTileManager().getTm().get(1), mgr.getRealm().getTileManager().getTm().get(1).getBlocks(), 0,
			//					this.dy)) {
			this.sense.getPos().y += this.dy;
			this.attackrange.getPos().y += this.dy;
			this.pos.y += this.dy;
			//			}
		} else if (this.ani.hasPlayedOnce()) {
			this.die = true;
		}
	}

	@Override
	public void render(Graphics2D g) {

		Color c = new Color(0f, 0f, 0f, .4f);
		g.setColor(c);
		g.fillOval((int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y) + (this.size / 2), this.size,
				this.size / 2);
		if (this.useRight && this.left) {
			g.drawImage(this.ani.getImage().image, (int) (this.pos.getWorldVar().x) + this.size,
					(int) (this.pos.getWorldVar().y), -this.size, this.size, null);
		} else {
			g.drawImage(this.ani.getImage().image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y),
					this.size, this.size, null);
		}

		if (this.hasEffect(EffectType.PARALYZED)) {
			if (!this.getSprite().hasEffect(Sprite.EffectEnum.GRAYSCALE)) {
				this.getSprite().setEffect(Sprite.EffectEnum.GRAYSCALE);
			}
		}

		if (this.hasEffect(EffectType.STUNNED)) {
			if (!this.getSprite().hasEffect(Sprite.EffectEnum.DECAY)) {
				this.getSprite().setEffect(Sprite.EffectEnum.DECAY);
			}
		}

		if (this.hasNoEffects()) {
			if (!this.getSprite().hasEffect(Sprite.EffectEnum.NORMAL)) {
				this.getSprite().setEffect(Sprite.EffectEnum.NORMAL);
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