package com.jrealm.game.entity;

import java.awt.Color;
import java.awt.Graphics2D;

import com.jrealm.game.contants.EffectType;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.model.ProjectilePositionMode;
import com.jrealm.game.states.PlayState;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class Enemy extends Entity {

	private long enemyId;
	protected AABB sense;
	protected int r_sense;

	protected AABB attackrange;
	protected int r_attackrange;

	protected int xOffset;
	protected int yOffset;

	public long lastShotTick = 0;

	private int weaponId = 1;

	public Enemy(int id, SpriteSheet sprite, Vector2f origin, int size) {
		super(id, sprite, origin, size);
		this.weaponId = id;
		this.sense = new AABB(new Vector2f((origin.x + (size / 2)) - (this.r_sense / 2),
				(origin.y + (size / 2)) - (this.r_sense / 2)), this.r_sense);
		this.attackrange = new AABB(new Vector2f(
				(origin.x + this.bounds.getXOffset() + (this.bounds.getWidth() / 2)) - (this.r_attackrange / 2),
				(origin.y + this.bounds.getYOffset() + (this.bounds.getHeight() / 2)) - (this.r_attackrange / 2)),
				this.r_attackrange);
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

	public void update(PlayState playState, double time) {
		super.update(time);
		Player player = playState.getRealm().getPlayer(playState.getPlayerId());
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
				&& !playState.getPlayer().hasEffect(EffectType.INVISIBLE)) {
			this.attack = true;

			boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > 500);

			if (canShoot) {
				this.lastShotTick = System.currentTimeMillis();
				Player target = player;
				Vector2f dest = target.getBounds().getPos().clone(target.getSize() / 2, target.getSize() / 2);

				Vector2f source = this.getPos().clone(this.getSize() / 2, this.getSize() / 2);
				float angle = Bullet.getAngle(source, dest);
				ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(this.weaponId);

				for (Projectile p : group.getProjectiles()) {
					if (p.getPositionMode().equals(ProjectilePositionMode.TARGET_PLAYER)) {
						playState.addProjectile(this.getWeaponId(), source.clone(),
								angle + Float.parseFloat(p.getAngle()), p.getSize(), p.getMagnitude(), p.getRange(),
								p.getDamage(), true, p.getFlags(), p.getAmplitude(), p.getFrequency());
					} else if (p.getPositionMode().equals(ProjectilePositionMode.ABSOLUTE)) {
						playState.addProjectile(this.getWeaponId(), source.clone(), Float.parseFloat(p.getAngle()),
								p.getSize(), p.getMagnitude(), p.getRange(), p.getDamage(), true, p.getFlags(),
								p.getAmplitude(), p.getFrequency());
					}

				}
			}
		} else {
			this.attack = false;
		}

		if (!this.isFallen()) {
			if (!this.tc.collisionTile(playState.getRealm().getTileManager().getTm().get(1).getBlocks(),
					this.dx,
					0)) {
				this.sense.getPos().x += this.dx;
				this.attackrange.getPos().x += this.dx;
				this.pos.x += this.dx;
			}
			if (!this.tc.collisionTile(playState.getRealm().getTileManager().getTm().get(1).getBlocks(), 0,
					this.dy)) {
				this.sense.getPos().y += this.dy;
				this.attackrange.getPos().y += this.dy;
				this.pos.y += this.dy;
			}
		} else if (this.ani.hasPlayedOnce()) {
			this.die = true;
		}
	}

	@Override
	public void render(Graphics2D g) {

		Color c = new Color(0f, 0f, 0f, .4f);
		g.setColor(c);
		g.fillOval((int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y) + 45, this.size, this.size / 2);
		if (this.useRight && this.left) {
			g.drawImage(this.ani.getImage().image, (int) (this.pos.getWorldVar().x) + this.size,
					(int) (this.pos.getWorldVar().y), -this.size, this.size, null);
		} else {
			g.drawImage(this.ani.getImage().image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y),
					this.size, this.size, null);
		}

		// Health Bar UI
		g.setColor(Color.red);
		g.fillRect((int) (this.pos.getWorldVar().x + this.bounds.getXOffset()), (int) (this.pos.getWorldVar().y - 5),
				24, 5);

		g.setColor(Color.green);
		g.fillRect((int) (this.pos.getWorldVar().x + this.bounds.getXOffset()), (int) (this.pos.getWorldVar().y - 5),
				(int) (24 * this.healthpercent), 5);

	}

}