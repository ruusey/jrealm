package com.jrealm.game.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.states.PlayState;
import com.jrealm.game.util.Camera;

public abstract class Enemy extends Entity {

	protected AABB sense;
	protected int r_sense;

	protected AABB attackrange;
	protected int r_attackrange;

	private Camera cam;

	protected int xOffset;
	protected int yOffset;

	protected ArrayList<GameObject> collisions;

	public long lastShotTick = 0;

	public Enemy(int id, Camera cam, SpriteSheet sprite, Vector2f origin, int size) {
		super(id, sprite, origin, size);
		this.cam = cam;


		this.sense = new AABB(new Vector2f((origin.x + (size / 2)) - (this.r_sense / 2), (origin.y + (size / 2)) - (this.r_sense / 2)), this.r_sense);
		this.attackrange = new AABB(new Vector2f((origin.x + this.bounds.getXOffset() + (this.bounds.getWidth() / 2)) - (this.r_attackrange / 2) , (origin.y + this.bounds.getYOffset() + (this.bounds.getHeight() / 2)) - (this.r_attackrange / 2) ), this.r_attackrange);
	}


	public void chase(Player player) {
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
		if(this.cam.getBounds().collides(this.bounds)) {
			super.update(time);
			this.chase(playState.getPlayer());
			this.move();

			if(this.teleported) {
				this.teleported = false;

				//				this.bounds.setWidth(this.size / 2);
				//				this.bounds.setHeight((this.size / 2) - this.yOffset);
				//				this.bounds.setXOffset((this.size / 2) - this.xOffset);
				//				this.bounds.setYOffset((this.size / 2) + this.yOffset);

				this.hitBounds = new AABB(this.pos, this.size, this.size);
				// this.hitBounds.setXOffset(this.size / 2);
				// this.hitBounds.setYOffset(128);
				// this.hitBounds.setYOffset((this.size / 2) + this.yOffset);

				this.sense = new AABB(new Vector2f((this.pos.x + (this.size / 2)) - (this.r_sense / 2), (this.pos.y + (this.size / 2)) - (this.r_sense / 2)), this.r_sense);
				this.attackrange = new AABB(new Vector2f((this.pos.x + this.bounds.getXOffset() + (this.bounds.getWidth() / 2)) - (this.r_attackrange / 2) , (this.pos.y + this.bounds.getYOffset() + (this.bounds.getHeight() / 2)) - (this.r_attackrange / 2) ), this.r_attackrange);
			}

			if (this.attackrange.colCircleBox(playState.getPlayer().getBounds()) && !this.isInvincible) {
				this.attack = true;
				//				playState.getPlayer().setHealth(playState.getPlayer().getHealth() - this.damage,
				//						5f * this.getDirection(),
				//						(this.currentDirection == this.UP) || (this.currentDirection == this.DOWN));

				boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > 500);

				if (canShoot) {
					this.lastShotTick = System.currentTimeMillis();
					Player target = playState.getPlayer();
					Vector2f dest = target.getBounds().getPos().clone(target.getSize() / 2, target.getSize() / 2);

					Vector2f source = this.getPos().clone(this.getSize() / 2, this.getSize() / 2);
					float angle = Bullet.getAngle(source, dest);

					playState.addProjectile(source.clone(), angle - 0.2f, (short) 8, 3.0f, 356.0f, (short) 10, true);
					playState.addProjectile(source.clone(), angle - 0.1f, (short) 8, 3.0f, 356.0f, (short) 10, true);
					playState.addProjectile(source.clone(), angle, (short) 8, 3.0f, 356.0f, (short) 10, true);
					playState.addProjectile(source.clone(), angle + 0.1f, (short) 8, 3.0f, 356.0f, (short) 10, true);
					playState.addProjectile(source.clone(), angle + 0.2f, (short) 8, 3.0f, 356.0f, (short) 10, true);

				}
			} else {
				this.attack = false;
			}

			if (!this.fallen) {
				if (!this.tc.collisionTile(this.dx, 0)) {
					this.sense.getPos().x += this.dx;
					this.attackrange.getPos().x += this.dx;
					this.pos.x += this.dx;
				}
				if (!this.tc.collisionTile(0, this.dy)) {
					this.sense.getPos().y += this.dy;
					this.attackrange.getPos().y += this.dy;
					this.pos.y += this.dy;
				}
			} else if(this.ani.hasPlayedOnce()) {
				this.die = true;
			}
		}
	}

	@Override
	public void render(Graphics2D g) {
		if(this.cam.getBounds().collides(this.bounds)) {

			//if(isInvincible)
			if(this.useRight && this.left) {
				g.drawImage(this.ani.getImage().image, (int) (this.pos.getWorldVar().x) + this.size, (int) (this.pos.getWorldVar().y), -this.size, this.size, null);
			} else {
				g.drawImage(this.ani.getImage().image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y), this.size, this.size, null);
			}


			// Health Bar UI
			g.setColor(Color.red);
			g.fillRect((int) (this.pos.getWorldVar().x + this.bounds.getXOffset()), (int) (this.pos.getWorldVar().y - 5), 24, 5);

			g.setColor(Color.green);
			g.fillRect((int) (this.pos.getWorldVar().x + this.bounds.getXOffset()), (int) (this.pos.getWorldVar().y - 5), (int) (24 * this.healthpercent), 5);

		}
	}
}