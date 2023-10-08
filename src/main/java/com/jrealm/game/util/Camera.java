package com.jrealm.game.util;

import java.awt.Graphics;

import com.jrealm.game.GamePanel;
import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.entity.Entity;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.states.PlayState;

import lombok.Data;

@Data
public class Camera {

	private AABB collisionCam;

	private boolean up;
	private boolean down;
	private boolean left;
	private boolean right;

	private float dx;
	private float dy;
	private float maxSpeed = 4f;
	private float acc = 3f;
	private float deacc = 0.3f;

	private int widthLimit;
	private int heightLimit;

	private int tileSize = GlobalConstants.BASE_TILE_SIZE;

	private Entity e;

	public Camera(AABB collisionCam) {
		this.collisionCam = collisionCam;
	}

	public void setLimit(int widthLimit, int heightLimit) {
		this.widthLimit = widthLimit;
		this.heightLimit = heightLimit;
	}

	public void setTileSize(int tileSize) {
		this.tileSize = tileSize;
	}

	public Entity getTarget() { return this.e; }

	public Vector2f getPos() {
		return this.collisionCam.getPos();
	}
	public AABB getBounds() {
		return this.collisionCam;
	}

	public void update() {
		this.move();
		if(this.e != null) {
			if (!this.e.xCol) {
				if (((this.e.getPos().getWorldVar().x
						+ this.dy) < (Vector2f.getWorldVarX(this.widthLimit - (this.collisionCam.getWidth() / 2))
								+ this.tileSize))
						&& ((this.e.getPos().getWorldVar().x + this.dy) > Vector2f
								.getWorldVarX((GamePanel.width / 2) - (this.tileSize * 2)))) {
					PlayState.map.x += this.dx;
					this.collisionCam.getPos().x += this.dx;
					//bounds.getPos().x += dx;
				}
			}
			if (!this.e.yCol) {
				if (((this.e.getPos().getWorldVar().y
						+ this.dy) < (Vector2f.getWorldVarY(this.heightLimit - (this.collisionCam.getHeight() / 2))
								+ this.tileSize))
						&& ((this.e.getPos().getWorldVar().y + this.dy) > Vector2f
								.getWorldVarY((GamePanel.height / 2) - (this.tileSize * 2)))) {
					PlayState.map.y += this.dy;
					this.collisionCam.getPos().y += this.dy;
					//bounds.getPos().y += dy;
				}
			}
		} else {
			if(((this.collisionCam.getPos().x + this.dx) > 0)
					&& ((this.collisionCam.getPos().getWorldVar().x
							+ this.dx) < (Vector2f.getWorldVarX(this.widthLimit - this.collisionCam.getWidth())
									- this.tileSize))) {
				PlayState.map.x += this.dx;
				this.collisionCam.getPos().x += this.dx;
			}

			if(((this.collisionCam.getPos().y + this.dy) > 0)
					&& ((this.collisionCam.getPos().getWorldVar().y
							+ this.dy) < (Vector2f.getWorldVarY(this.heightLimit - this.collisionCam.getHeight())
									- this.tileSize))) {
				PlayState.map.y += this.dy;
				this.collisionCam.getPos().y += this.dy;
			}
		}
	}

	private void move() {
		if (this.up) {
			this.dy -= this.acc;
			if (this.dy < -this.maxSpeed) {
				this.dy = -this.maxSpeed;
			}
		} else if (this.dy < 0) {
			this.dy += this.deacc;
			if (this.dy > 0) {
				this.dy = 0;
			}
		}
		if (this.down) {
			this.dy += this.acc;
			if (this.dy > this.maxSpeed) {
				this.dy = this.maxSpeed;
			}
		} else if (this.dy > 0) {
			this.dy -= this.deacc;
			if (this.dy < 0) {
				this.dy = 0;
			}
		}
		if (this.left) {
			this.dx -= this.acc;
			if (this.dx < -this.maxSpeed) {
				this.dx = -this.maxSpeed;
			}
		} else if (this.dx < 0) {
			this.dx += this.deacc;
			if (this.dx > 0) {
				this.dx = 0;
			}
		}
		if (this.right) {
			this.dx += this.acc;
			if (this.dx > this.maxSpeed) {
				this.dx = this.maxSpeed;
			}
		} else if (this.dx > 0) {
			this.dx -= this.deacc;
			if (this.dx < 0) {
				this.dx = 0;
			}
		}
	}

	public void target(Entity e) {
		this.e = e;
		if(e != null) {
			this.acc = e.getAcc();
			this.deacc = e.getDeacc();
			this.maxSpeed = e.getMaxSpeed();
		} else {
			this.acc = 3;
			this.deacc = 0.3f;
			this.maxSpeed = 8;
		}
	}

	public void setMaxSpeed(float maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public void input(MouseHandler mouse, KeyHandler key) {

		if (this.e == null) {
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
		} else {
			if (!this.e.yCol) {
				if ((this.collisionCam.getPos().y + (this.collisionCam.getHeight() / 2) + this.dy) > (this.e.getPos().y
						+ (this.e.getSize() / 2) + this.e.getDy() + 2)) {
					this.up = true;
					this.down = false;
				} else if ((this.collisionCam.getPos().y + (this.collisionCam.getHeight() / 2)
						+ this.dy) < ((this.e.getPos().y + (this.e.getSize() / 2) + this.e.getDy()) - 2)) {
					this.down = true;
					this.up = false;
				} else {
					this.dy = 0;
					this.up = false;
					this.down = false;
				}
			}

			if (!this.e.xCol) {
				if ((this.collisionCam.getPos().x + (this.collisionCam.getWidth() / 2) + this.dx) > (this.e.getPos().x
						+ (this.e.getSize() / 2) + this.e.getDx() + 2)) {
					this.left = true;
					this.right = false;
				} else if ((this.collisionCam.getPos().x + (this.collisionCam.getWidth() / 2)
						+ this.dx) < ((this.e.getPos().x + (this.e.getSize() / 2) + this.e.getDx()) - 2)) {
					this.right = true;
					this.left = false;
				} else {
					this.dx = 0;
					this.right = false;
					this.left = false;
				}
			}
		}
	}

	public void render(Graphics g) {
		/* g.setColor(Color.blue);
        g.drawRect((int) collisionCam.getPos().getWorldVar().x, (int) collisionCam.getPos().getWorldVar().y, (int) collisionCam.getWidth(),
                (int) collisionCam.getHeight()); */


		/* g.setColor(Color.magenta);
         g.drawLine(GamePanel.width / 2, 0, GamePanel.width / 2, GamePanel.height);
         g.drawLine(0, GamePanel.height / 2, GamePanel.width,GamePanel.height / 2); */

	}
}